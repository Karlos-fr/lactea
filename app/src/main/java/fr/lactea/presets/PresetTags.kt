package fr.lactea.presets

import fr.lactea.settings.Settings
import org.json.JSONObject

/**
 * How easy on the eyes a preset is: measured on a PC for the published packs (tools/flashscan,
 * in each pack's index.json), and correctable by hand in the preset browser, the user's word
 * winning over the measure.
 */
class PresetTags(private val settings: Settings) {

    enum class Tag(val id: String) {
        /** Calm: no flash and a slow image. */
        CHILL("chill"),
        /** Lively but no flash. */
        OK("ok"),
        /** More than 3 flashes in a second (ITU-R BT.1702). */
        FLASH("flash");

        companion object {
            fun of(id: String?): Tag? = entries.firstOrNull { it.id == id }
        }
    }

    /** Tags measured for the presets of the installed packs; set when the catalogue loads. */
    @Volatile var measured: Map<String, Tag> = emptyMap()

    private var manual: MutableMap<String, Tag> = load()

    @Synchronized fun manual(key: String): Tag? = manual[key]

    /** The user's tag if any, else the measured one; null when neither is known. */
    @Synchronized fun effective(key: String): Tag? = manual[key] ?: measured[key]

    /** Browser key: measured → chill → OK → flash → measured again. Returns the new manual tag. */
    @Synchronized fun cycle(key: String): Tag? {
        val next = when (manual[key]) {
            null -> Tag.CHILL
            Tag.CHILL -> Tag.OK
            Tag.OK -> Tag.FLASH
            Tag.FLASH -> null
        }
        if (next == null) manual.remove(key) else manual[key] = next
        save()
        return next
    }

    /** Moves the manual tags of presets whose key changed. */
    @Synchronized fun renameKeys(renames: Map<String, String>) {
        renames.forEach { (old, new) -> manual.remove(old)?.let { manual.putIfAbsent(new, it) } }
        if (renames.isNotEmpty()) save()
    }

    private fun load(): MutableMap<String, Tag> = runCatching {
        val o = JSONObject(settings.prefs.getString(PREF, "{}")!!)
        o.keys().asSequence().mapNotNull { k -> Tag.of(o.getString(k))?.let { k to it } }.toMap(HashMap())
    }.getOrDefault(HashMap())

    private fun save() {
        val o = JSONObject()
        manual.forEach { (k, t) -> o.put(k, t.id) }
        settings.prefs.edit().putString(PREF, o.toString()).apply()
    }

    companion object {
        private const val PREF = "preset_tags"
    }
}
