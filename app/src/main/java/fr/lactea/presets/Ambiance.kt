package fr.lactea.presets

import org.json.JSONArray
import org.json.JSONObject

/**
 * A saved selection of the presets to play ("chill evening", "party"…), combining criteria.
 * An empty criterion lets every preset through.
 *
 * @param categories categories to play (first folder of the preset key); empty = all
 * @param minFps minimum frame rate expected at [FPS_HEIGHT] on this TV; 0 = no minimum.
 *               Presets never measured are kept.
 */
data class Ambiance(
    val id: String,
    val categories: Set<String> = emptySet(),
    val minFps: Int = 0,
) {
    fun accepts(entry: Playlist.Entry, stats: PresetStats, fpsCap: Int): Boolean {
        if (categories.isNotEmpty() && entry.category !in categories) return false
        if (minFps > 0) {
            val fps = stats.predict(entry.key, FPS_HEIGHT, fpsCap)
            if (fps != null && fps < minFps) return false
        }
        return true
    }

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("categories", JSONArray(categories.sorted()))
        .put("minFps", minFps)

    companion object {
        /** Render height of the "minimum FPS" criterion (the reference measures are at 720p). */
        const val FPS_HEIGHT = 720

        fun fromJson(o: JSONObject) = Ambiance(
            id = o.getString("id"),
            categories = o.optJSONArray("categories")?.let { a -> (0 until a.length()).mapTo(HashSet()) { a.getString(it) } }
                ?: emptySet(),
            minFps = o.optInt("minFps"),
        )

        fun listToJson(list: List<Ambiance>): String = JSONArray(list.map { it.toJson() }).toString()

        fun listFromJson(json: String): List<Ambiance> = runCatching {
            val a = JSONArray(json)
            (0 until a.length()).map { fromJson(a.getJSONObject(it)) }
        }.getOrDefault(emptyList())
    }
}
