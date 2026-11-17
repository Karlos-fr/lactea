package fr.lactea.presets

import java.util.TreeMap

/**
 * Reads a preset's equations the way the original MilkDrop did, for presets projectM refuses
 * ("Could not compile per-frame code"…). Only used as a retry after a failed load, so presets
 * that already work are never affected.
 *
 * What MilkDrop tolerated and projectM does not:
 *  - code lines are glued together without a separator, so a name or number may be split
 *    across two lines ("k1 = is_" + "beat…", "/11" + "99;") and a missing ";" can be absorbed;
 *  - "." alone is 0 (".-.4"), trailing letters after a number are ignored ("0.99l");
 *  - an assignment with nothing after "=" ("shpt =") is 0;
 *  - "\\" starts a comment, like "//".
 */
object MilkdropCompat {

    private val CODE_LINE = Regex(
        """^((?:per_frame_init_|per_frame_|per_pixel_)|(?:shape|wave)_\d+_(?:init|per_frame|per_point))(\d+)=(.*)$""",
        RegexOption.IGNORE_CASE,
    )
    private val COMMENT = Regex("""(//|\\\\).*$""")
    private val LONE_DOT = Regex("""(?<![\w.])\.(?![\w.])""")
    private val NUMBER_SUFFIX = Regex("""(?<![\w.])(\d+\.?\d*|\.\d+)([a-df-zA-DF-Z_]\w*)""")
    private val EMPTY_ASSIGNMENT = Regex("""(?<![=<>!])=\s*(?=;|$)""")

    /** The preset with its equation blocks rewritten MilkDrop-style, or null if nothing changes. */
    fun fix(text: String): String? {
        val blocks = LinkedHashMap<String, TreeMap<Int, String>>()
        val out = ArrayList<String>()
        for (raw in text.lines()) {
            val line = raw.trimEnd('\r')
            val m = CODE_LINE.find(line)
            if (m == null) {
                out += line
                continue
            }
            val prefix = m.groupValues[1]
            val block = blocks.getOrPut(prefix) {
                out += PLACEHOLDER + prefix
                TreeMap()
            }
            block[m.groupValues[2].toIntOrNull() ?: continue] = m.groupValues[3]
        }
        var changed = false
        val result = out.map { line ->
            if (!line.startsWith(PLACEHOLDER)) return@map line
            val prefix = line.removePrefix(PLACEHOLDER)
            val lines = blocks.getValue(prefix).values
            val code = join(lines)
            if (code != lines.joinToString("\n")) changed = true
            "${prefix}1=$code"
        }
        return if (changed) result.joinToString("\n") else null
    }

    private fun join(lines: Collection<String>): String {
        var code = lines.joinToString("") { it.replace(COMMENT, "").trim() }
        code = LONE_DOT.replace(code, "0")
        code = NUMBER_SUFFIX.replace(code) { it.groupValues[1] }
        code = EMPTY_ASSIGNMENT.replace(code, "=0")
        return code
    }

    private const val PLACEHOLDER = "\u0000"
}
