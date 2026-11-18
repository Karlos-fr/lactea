package fr.lactea.presets

import org.json.JSONArray
import org.json.JSONObject

/**
 * A saved selection of the presets to play ("chill evening", "party"…), combining criteria.
 * An empty criterion lets every preset through.
 *
 * @param name name given by the user; empty = described by its criteria
 * @param categories categories to play (first folder of the preset key); empty = all
 * @param minFps minimum frame rate expected at [FPS_HEIGHT] on this TV; 0 = no minimum.
 *               Presets never measured are kept.
 * @param noFlash leave out the presets tagged "flash" (presets without a tag are kept)
 * @param chillOnly only the presets tagged "chill"
 */
data class Ambiance(
    val id: String,
    val name: String = "",
    val categories: Set<String> = emptySet(),
    val minFps: Int = 0,
    val noFlash: Boolean = false,
    val chillOnly: Boolean = false,
) {
    fun accepts(entry: Playlist.Entry, stats: PresetStats, fpsCap: Int, tags: PresetTags): Boolean {
        if (categories.isNotEmpty() && entry.category !in categories) return false
        if (chillOnly || noFlash) {
            val tag = tags.effective(entry.key)
            if (chillOnly && tag != PresetTags.Tag.CHILL) return false
            if (noFlash && tag == PresetTags.Tag.FLASH) return false
        }
        if (minFps > 0) {
            val fps = stats.predict(entry.key, FPS_HEIGHT, fpsCap)
            if (fps != null && fps < minFps) return false
        }
        return true
    }

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("name", name)
        .put("categories", JSONArray(categories.sorted()))
        .put("minFps", minFps)
        .put("noFlash", noFlash)
        .put("chillOnly", chillOnly)

    companion object {
        /** Render height of the "minimum FPS" criterion (the reference measures are at 720p). */
        const val FPS_HEIGHT = 720

        fun fromJson(o: JSONObject) = Ambiance(
            id = o.getString("id"),
            name = o.optString("name"),
            categories = o.optJSONArray("categories")?.let { a -> (0 until a.length()).mapTo(HashSet()) { a.getString(it) } }
                ?: emptySet(),
            minFps = o.optInt("minFps"),
            noFlash = o.optBoolean("noFlash"),
            chillOnly = o.optBoolean("chillOnly"),
        )

        fun listToJson(list: List<Ambiance>): String = JSONArray(list.map { it.toJson() }).toString()

        fun listFromJson(json: String): List<Ambiance> = runCatching {
            val a = JSONArray(json)
            (0 until a.length()).map { fromJson(a.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }
}
