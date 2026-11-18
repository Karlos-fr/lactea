package fr.lactea.presets

import fr.lactea.settings.Settings
import org.json.JSONObject

/**
 * Measured steady-state FPS per preset and render height, learned while presets play.
 * Used to pick a resolution (Auto), choose fade vs hard cut, and exclude slow presets.
 *
 * Presets this TV has not measured yet fall back to the reference frame rates that come with the
 * packs (one benchmark of the whole collection on a reference TV, see [PresetCatalog]).
 */
class PresetStats(private val settings: Settings) {
    // key -> (height -> fps)
    private val data = HashMap<String, HashMap<Int, Float>>()
    // key -> fps at [referenceHeight], from the reference TV
    private var reference: Map<String, Float> = emptyMap()
    private var referenceHeight = 720
    var referenceDevice = ""
        private set
    private var dirty = false

    init {
        runCatching {
            val root = JSONObject(settings.prefs.getString("preset_stats", "{}")!!)
            for (key in root.keys()) {
                val o = root.getJSONObject(key)
                val m = HashMap<Int, Float>()
                for (h in o.keys()) m[h.toInt()] = o.getDouble(h).toFloat()
                data[key] = m
            }
        }
    }

    @Synchronized fun record(key: String, height: Int, fps: Float) {
        val m = data.getOrPut(key) { HashMap() }
        m[height] = m[height]?.let { it * 0.5f + fps * 0.5f } ?: fps
        dirty = true
    }

    /** Reference frame rates of the installed packs, replaced whenever the packs change. */
    @Synchronized fun setReference(fps: Map<String, Float>, height: Int, device: String) {
        reference = fps
        referenceHeight = height
        referenceDevice = device
    }

    /** Moves the measures of presets whose key changed ([renames]: old key -> new key). */
    @Synchronized fun renameKeys(renames: Map<String, String>) {
        renames.forEach { (old, new) -> data.remove(old)?.let { if (new !in data) data[new] = it } }
        if (renames.isNotEmpty()) {
            dirty = true
            save()
        }
    }

    @Synchronized fun referenceCount(): Int = reference.size

    @Synchronized fun measured(key: String): Map<Int, Float> = data[key]?.toMap() ?: emptyMap()

    @Synchronized fun count(): Int = data.size

    @Synchronized fun measuredKeys(): Set<String> = data.keys.toSet()

    /**
     * Estimated FPS at [height], scaling by pixel count from the nearest measurement.
     * Measurements at the FPS cap are only lower bounds, so they are not scaled upwards.
     * Returns null when nothing usable is known.
     */
    @Synchronized fun predict(key: String, height: Int, cap: Int): Float? {
        val m = data[key] ?: reference[key]?.let { mapOf(referenceHeight to it) } ?: return null
        m[height]?.let { return it }
        val lower = m.filterKeys { it < height }.maxByOrNull { it.key }
        val higher = m.filterKeys { it > height }.minByOrNull { it.key }
        if (higher != null) {
            return (higher.value * pixels(higher.key) / pixels(height)).coerceAtMost(cap.toFloat())
        }
        if (lower != null && lower.value < cap - 3) {
            return lower.value * pixels(lower.key) / pixels(height)
        }
        return null // only capped measurements below: could go either way
    }

    /** Slow = even at the lowest resolution it would stay under the user threshold. */
    fun isSlow(key: String, cap: Int): Boolean {
        val p = predict(key, Settings.RESOLUTIONS.first(), cap) ?: return false
        return p < settings.slowThreshold
    }

    @Synchronized fun slowCount(cap: Int): Int = (data.keys + reference.keys).count { isSlow(it, cap) }

    @Synchronized fun reset() {
        data.clear()
        dirty = true
        save()
    }

    @Synchronized fun save() {
        if (!dirty) return
        val root = JSONObject()
        data.forEach { (k, m) ->
            val o = JSONObject()
            m.forEach { (h, f) -> o.put(h.toString(), f.toDouble()) }
            root.put(k, o)
        }
        settings.prefs.edit().putString("preset_stats", root.toString()).apply()
        dirty = false
    }

    companion object {
        fun pixels(height: Int): Float = height.toFloat() * height * 16f / 9f
    }
}
