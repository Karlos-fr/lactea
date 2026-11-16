package fr.lactea.cover

import java.io.File

/**
 * Presets that sample random textures (`sampler_rand00`, `sampler_fw_rand01`, `texsize_rand02`,
 * `sampler_rand00_smalltiled`…) are rewritten on the fly before loading:
 * - the first random texture becomes the album cover (when there is one);
 * - every other random texture becomes a texture picked by Lactea from the texture folders.
 *
 * Lactea picks them itself because projectM 4.1.8 fails to load a preset whenever it has to
 * pick a random texture on Android (std::exception). projectM itself is not modified.
 */
object CoverPresets {
    private val RANDOM_TEXTURE =
        Regex("""\b(sampler|texsize)_((?:[fp][wc]_)?)(rand\d\d(?:_[A-Za-z0-9]+)?)\b""", RegexOption.IGNORE_CASE)
    private val VALID_NAME = Regex("[A-Za-z0-9_]+")

    fun read(file: File): String? = runCatching { file.readText(Charsets.ISO_8859_1) }.getOrNull()

    fun isCompatible(text: String): Boolean = RANDOM_TEXTURE.containsMatchIn(text)

    fun isCompatible(file: File): Boolean = read(file)?.let(::isCompatible) ?: false

    /** Texture names (file names without extension) usable in a shader, from [dirs]. */
    fun textureNames(dirs: List<File>): List<String> = dirs.flatMap { d ->
        d.listFiles()?.filter { it.isFile && it.extension.lowercase() in EXTENSIONS }?.map { it.nameWithoutExtension }
            ?: emptyList()
    }.filter { VALID_NAME.matches(it) && !it.startsWith("cover_") }.distinct()

    /**
     * @param cover texture name of the album cover, or null to use only regular textures
     * @param textures candidate texture names for the other random textures
     */
    fun rewrite(text: String, cover: String?, textures: List<String>): String {
        val ids = RANDOM_TEXTURE.findAll(text).map { it.groupValues[3].lowercase() }.distinct().sorted().toList()
        if (ids.isEmpty()) return text
        val pool = textures.shuffled().toMutableList()
        val mapping = HashMap<String, String>()
        ids.forEachIndexed { i, id ->
            if (i == 0 && cover != null) {
                mapping[id] = cover
            } else {
                // "rand00_smalltiled" means: a random texture whose name starts with "smalltiled".
                val prefix = id.substringAfter('_', "")
                val pick = pool.firstOrNull { prefix.isNotEmpty() && it.lowercase().startsWith(prefix) } ?: pool.firstOrNull()
                if (pick != null) {
                    pool.remove(pick) // distinct textures: the same sampler must not be declared twice
                    mapping[id] = pick
                }
            }
        }
        return RANDOM_TEXTURE.replace(text) { m ->
            mapping[m.groupValues[3].lowercase()]?.let { "${m.groupValues[1]}_${m.groupValues[2]}$it" } ?: m.value
        }
    }

    private val EXTENSIONS = setOf("jpg", "jpeg", "png", "tga", "bmp", "dds", "dib")
}
