package fr.lactea.presets

import java.io.File

/**
 * Everything Lactea knows about the installed packs, built from their index.json (see [PackIndex]):
 * the presets, those able to show the album cover, the texture folders and the reference frame
 * rates. Loading it reads one small file per pack; no preset is scanned nor opened.
 */
class PresetCatalog private constructor(
    val entries: List<Playlist.Entry>,
    /** Keys of the presets that can show the album cover. */
    val imageKeys: Set<String>,
    val textureDirs: List<File>,
    /** Texture names Lactea can use for random textures. */
    val textureNames: List<String>,
    /** key -> FPS measured on the reference TV, at [referenceHeight]. */
    val referenceFps: Map<String, Float>,
    val referenceHeight: Int,
    val referenceDevice: String,
    /** key -> tag measured on a PC. */
    val tags: Map<String, PresetTags.Tag>,
    /** key -> preview image (2 x 2 mosaic). */
    val previews: Map<String, File>,
) {
    companion object {
        val EMPTY = PresetCatalog(emptyList(), emptySet(), emptyList(), emptyList(), emptyMap(), 720, "", emptyMap(), emptyMap())

        /**
         * @param packDirs installed packs; a preset found in several packs keeps the first one.
         * Presets keep the order of the indexes (sorted by tools/make_packs.py), pack by pack.
         */
        fun load(packDirs: List<File>): PresetCatalog {
            val seen = HashSet<String>()
            val entries = ArrayList<Playlist.Entry>()
            val images = HashSet<String>()
            val textureDirs = ArrayList<File>()
            val textureNames = LinkedHashSet<String>()
            val fps = HashMap<String, Float>()
            val tags = HashMap<String, PresetTags.Tag>()
            val previews = HashMap<String, File>()
            var refHeight = 720
            var refDevice = ""
            for (dir in packDirs) {
                val index = PackIndex.read(dir)
                index.textureDir?.let { textureDirs += File(dir, it) }
                textureNames += index.textureNames
                index.reference?.let { refHeight = it.height; refDevice = it.device }
                for (p in index.presets) {
                    val key = p.file.removeSuffix(".milk")
                    if (!seen.add(key)) continue
                    entries += Playlist.Entry(key, File(dir, p.file))
                    if (p.image) images += key
                    p.fps?.let { fps[key] = it }
                    PresetTags.Tag.of(p.tag)?.let { tags[key] = it }
                    if (p.preview) previews[key] = File(dir, "$key.webp")
                }
            }
            return PresetCatalog(entries, images, textureDirs, textureNames.toList(), fps, refHeight, refDevice, tags, previews)
        }
    }
}
