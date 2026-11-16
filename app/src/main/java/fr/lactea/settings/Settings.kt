package fr.lactea.settings

import android.content.Context
import android.content.SharedPreferences
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

/** Persistent user settings (SharedPreferences). Safe to read from any thread. */
class Settings(context: Context) {
    val prefs: SharedPreferences = context.getSharedPreferences("lactea", Context.MODE_PRIVATE)

    // --- Visuel ---
    /** Render height in pixels (16:9), or 0 for Auto. */
    var resolution by int("resolution", 0)
    /** Highest resolution the Auto mode may pick. */
    var autoMaxResolution by int("auto_max_resolution", 1080)
    var fpsCap by int("fps_cap", 60)
    var meshIndex by int("mesh_index", 1)
    var showHud by bool("show_hud", false)

    // --- Presets & transitions ---
    var presetDuration by int("preset_duration", 25)
    var transitionMode by string("transition_mode", TRANSITION_AUTO)
    var fadeSeconds by float("fade_seconds", 2f)
    var hardCutsOnBeat by bool("hard_cuts", true)
    var hardCutSensitivity by float("hard_cut_sensitivity", 2f)
    var shuffle by bool("shuffle", true)
    var favoritesOnly by bool("favorites_only", false)
    var excludeSlow by bool("exclude_slow", true)
    var slowThreshold by int("slow_threshold", 25)
    var showPresetNameOnChange by bool("show_preset_name", true)

    // --- Titre du morceau ---
    var showTitleOnTrackChange by bool("title_on_change", true)
    var titleSeconds by int("title_seconds", 6)
    /** 0 = centre, 1 = bottom */
    var titlePosition by int("title_position", 0)
    /** 0 = small, 1 = medium, 2 = large */
    var titleSize by int("title_size", 1)
    var newPresetOnTrack by bool("new_preset_on_track", false)

    // --- Pochette ---
    var coverWithTitle by bool("cover_with_title", true)
    var coverPulse by bool("cover_pulse", true)
    var coverInPresets by bool("cover_in_presets", true)
    var coverPresetOnTrack by bool("cover_preset_on_track", false)
    var coverOnline by bool("cover_online", true)

    // --- Lancement automatique ---
    /** Seconds of continuous playback before Lactea opens by itself; 0 = never. */
    var autoLaunchDelay by int("auto_launch_delay", 30)
    var autoLaunchMusicAppsOnly by bool("auto_launch_music_only", true)
    var autoCloseWhenMusicStops by bool("auto_close", true)

    // --- Audio ---
    var beatSensitivity by float("beat_sensitivity", 1f)
    /** 0 = normalised (constant level), 1 = as played (follows the volume). */
    var captureMode by int("capture_mode", 0)

    // --- Packs ---
    var manifestUrl by string("manifest_url", DEFAULT_MANIFEST_URL)

    // --- Preset lists ---
    var favorites: Set<String>
        get() = prefs.getStringSet("favorites", emptySet())!!.toSet()
        set(v) = prefs.edit().putStringSet("favorites", HashSet(v)).apply()
    var blacklist: Set<String>
        get() = prefs.getStringSet("blacklist", emptySet())!!.toSet()
        set(v) = prefs.edit().putStringSet("blacklist", HashSet(v)).apply()

    fun toggleFavorite(key: String): Boolean {
        val fav = favorites.toMutableSet()
        val added = fav.add(key) || run { fav.remove(key); false }
        favorites = fav
        return added
    }

    fun toggleBlacklist(key: String): Boolean {
        val bl = blacklist.toMutableSet()
        val added = bl.add(key) || run { bl.remove(key); false }
        blacklist = bl
        return added
    }

    val meshSize: Pair<Int, Int> get() = MESH_SIZES[meshIndex.coerceIn(0, MESH_SIZES.lastIndex)]

    private fun int(key: String, def: Int) = object : ReadWriteProperty<Any?, Int> {
        override fun getValue(thisRef: Any?, property: KProperty<*>) = prefs.getInt(key, def)
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Int) = prefs.edit().putInt(key, value).apply()
    }

    private fun float(key: String, def: Float) = object : ReadWriteProperty<Any?, Float> {
        override fun getValue(thisRef: Any?, property: KProperty<*>) = prefs.getFloat(key, def)
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Float) = prefs.edit().putFloat(key, value).apply()
    }

    private fun bool(key: String, def: Boolean) = object : ReadWriteProperty<Any?, Boolean> {
        override fun getValue(thisRef: Any?, property: KProperty<*>) = prefs.getBoolean(key, def)
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: Boolean) = prefs.edit().putBoolean(key, value).apply()
    }

    private fun string(key: String, def: String) = object : ReadWriteProperty<Any?, String> {
        override fun getValue(thisRef: Any?, property: KProperty<*>) = prefs.getString(key, def) ?: def
        override fun setValue(thisRef: Any?, property: KProperty<*>, value: String) = prefs.edit().putString(key, value).apply()
    }

    companion object {
        const val TRANSITION_AUTO = "auto"
        const val TRANSITION_FADE = "fade"
        const val TRANSITION_CUT = "cut"
        const val DEFAULT_MANIFEST_URL = "https://github.com/Karlos-fr/lactea/releases/latest/download/manifest.json"
        val RESOLUTIONS = listOf(540, 720, 1080, 1440, 2160)
        val MESH_SIZES = listOf(32 to 24, 48 to 32, 64 to 48, 96 to 72, 128 to 96)
    }
}
