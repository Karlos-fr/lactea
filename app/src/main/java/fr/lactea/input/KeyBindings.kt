package fr.lactea.input

import android.view.KeyEvent
import fr.lactea.R
import fr.lactea.i18n.L
import fr.lactea.settings.Settings
import org.json.JSONArray
import org.json.JSONObject

/** Everything the remote can trigger. */
enum class Action(private val labelRes: Int) {
    NEXT_PRESET(R.string.action_next_preset),
    PREVIOUS_PRESET(R.string.action_prev_preset),
    TOGGLE_LOCK(R.string.action_lock),
    TOGGLE_SHUFFLE(R.string.action_shuffle),
    TOGGLE_FAVORITE(R.string.action_favorite),
    TOGGLE_BLACKLIST(R.string.action_blacklist),
    SHOW_TITLE(R.string.action_show_title),
    SHOW_PRESET_NAME(R.string.action_show_preset_name),
    SENSITIVITY_UP(R.string.action_sens_up),
    SENSITIVITY_DOWN(R.string.action_sens_down),
    OPEN_OPTIONS(R.string.action_options),
    OPEN_BROWSER(R.string.action_browser),
    SHOW_HELP(R.string.action_help),
    TOGGLE_HUD(R.string.action_hud),
    PLAY_PAUSE(R.string.action_play_pause),
    NEXT_TRACK(R.string.action_next_track),
    PREVIOUS_TRACK(R.string.action_prev_track),
    NEXT_AMBIANCE(R.string.action_next_ambiance);

    val label: String get() = L.s(labelRes)
}

/** A physical key, pressed briefly or held. */
data class Trigger(val keyCode: Int, val long: Boolean) {
    val label: String get() = KeyBindings.keyName(keyCode) + if (long) L.s(R.string.long_press_suffix) else ""
}

/**
 * Remote mapping, fully reconfigurable and persisted. Long-press OK always opens the
 * options panel and Back always keeps its role, so the user can never lock themselves out.
 */
class KeyBindings(private val settings: Settings) {
    private val map = LinkedHashMap<Trigger, Action>()

    init {
        load()
    }

    @Synchronized fun actionFor(t: Trigger): Action? =
        if (t in RESERVED) Action.OPEN_OPTIONS else map[t]

    @Synchronized fun triggersFor(a: Action): List<Trigger> =
        map.filterValues { it == a }.keys.toList() + RESERVED.filter { a == Action.OPEN_OPTIONS }

    @Synchronized fun hasLongBinding(keyCode: Int): Boolean =
        map.keys.any { it.keyCode == keyCode && it.long } || RESERVED.any { it.keyCode == keyCode }

    @Synchronized fun isBound(keyCode: Int): Boolean =
        map.keys.any { it.keyCode == keyCode } || RESERVED.any { it.keyCode == keyCode }

    @Synchronized fun bind(t: Trigger, a: Action) {
        map[t] = a
        save()
    }

    @Synchronized fun unbind(t: Trigger) {
        map.remove(t)
        save()
    }

    @Synchronized fun resetToDefaults() {
        map.clear()
        map.putAll(DEFAULTS)
        save()
    }

    private fun load() {
        val json = settings.prefs.getString("key_bindings", null)
        if (json == null) {
            map.putAll(DEFAULTS)
            return
        }
        runCatching {
            val arr = JSONArray(json)
            for (i in 0 until arr.length()) {
                val o = arr.getJSONObject(i)
                val action = runCatching { Action.valueOf(o.getString("action")) }.getOrNull() ?: continue
                map[Trigger(o.getInt("key"), o.getBoolean("long"))] = action
            }
        }.onFailure { map.clear(); map.putAll(DEFAULTS) }
        migrate()
    }

    /** Gives actions added in later versions their default key, if that key is still free. */
    private fun migrate() {
        val version = settings.prefs.getInt("key_bindings_version", 1)
        if (version >= VERSION) return
        ADDED_IN.filterKeys { it > version }.values.flatten().forEach { a ->
            DEFAULTS.filterValues { it == a }.keys.forEach { t -> if (t !in map) map[t] = a }
        }
        save()
    }

    private fun save() {
        val arr = JSONArray()
        map.forEach { (t, a) ->
            arr.put(JSONObject().put("key", t.keyCode).put("long", t.long).put("action", a.name))
        }
        settings.prefs.edit().putString("key_bindings", arr.toString())
            .putInt("key_bindings_version", VERSION).apply()
    }

    companion object {
        private const val VERSION = 2
        private val ADDED_IN = mapOf(2 to listOf(Action.NEXT_TRACK, Action.PREVIOUS_TRACK, Action.PLAY_PAUSE))

        /** Long-press OK → options panel, always. */
        val RESERVED = setOf(
            Trigger(KeyEvent.KEYCODE_DPAD_CENTER, true),
            Trigger(KeyEvent.KEYCODE_ENTER, true),
        )

        /** Keys an app never receives or must not take over. */
        val FORBIDDEN = setOf(
            KeyEvent.KEYCODE_BACK, KeyEvent.KEYCODE_HOME, KeyEvent.KEYCODE_APP_SWITCH,
            KeyEvent.KEYCODE_POWER, KeyEvent.KEYCODE_SETTINGS, KeyEvent.KEYCODE_ASSIST,
            KeyEvent.KEYCODE_VOICE_ASSIST, KeyEvent.KEYCODE_SEARCH, KeyEvent.KEYCODE_TV_INPUT,
            KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_MUTE,
        )

        val DEFAULTS: Map<Trigger, Action> = linkedMapOf(
            Trigger(KeyEvent.KEYCODE_DPAD_RIGHT, false) to Action.NEXT_PRESET,
            Trigger(KeyEvent.KEYCODE_DPAD_LEFT, false) to Action.PREVIOUS_PRESET,
            Trigger(KeyEvent.KEYCODE_DPAD_UP, false) to Action.SENSITIVITY_UP,
            Trigger(KeyEvent.KEYCODE_DPAD_DOWN, false) to Action.SENSITIVITY_DOWN,
            Trigger(KeyEvent.KEYCODE_DPAD_CENTER, false) to Action.SHOW_TITLE,
            Trigger(KeyEvent.KEYCODE_ENTER, false) to Action.SHOW_TITLE,
            Trigger(KeyEvent.KEYCODE_PROG_RED, false) to Action.TOGGLE_LOCK,
            Trigger(KeyEvent.KEYCODE_PROG_GREEN, false) to Action.TOGGLE_SHUFFLE,
            Trigger(KeyEvent.KEYCODE_PROG_YELLOW, false) to Action.TOGGLE_FAVORITE,
            Trigger(KeyEvent.KEYCODE_PROG_YELLOW, true) to Action.TOGGLE_BLACKLIST,
            Trigger(KeyEvent.KEYCODE_PROG_BLUE, false) to Action.OPEN_OPTIONS,
            Trigger(KeyEvent.KEYCODE_PROG_BLUE, true) to Action.OPEN_BROWSER,
            Trigger(KeyEvent.KEYCODE_INFO, false) to Action.SHOW_PRESET_NAME,
            Trigger(KeyEvent.KEYCODE_INFO, true) to Action.SHOW_HELP,
            Trigger(KeyEvent.KEYCODE_MENU, false) to Action.OPEN_OPTIONS,
            Trigger(KeyEvent.KEYCODE_0, false) to Action.TOGGLE_HUD,
            // Media keys (Play/Pause, ⏭, ⏮) are left to Android, which sends them to the playing app.
            Trigger(KeyEvent.KEYCODE_CHANNEL_UP, false) to Action.NEXT_TRACK,
            Trigger(KeyEvent.KEYCODE_CHANNEL_DOWN, false) to Action.PREVIOUS_TRACK,
        )

        /** Key names: a text resource, or a name that is the same in every language. */
        private val NAMES: Map<Int, Any> = mapOf(
            KeyEvent.KEYCODE_DPAD_RIGHT to R.string.key_right,
            KeyEvent.KEYCODE_DPAD_LEFT to R.string.key_left,
            KeyEvent.KEYCODE_DPAD_UP to R.string.key_up,
            KeyEvent.KEYCODE_DPAD_DOWN to R.string.key_down,
            KeyEvent.KEYCODE_DPAD_CENTER to "OK",
            KeyEvent.KEYCODE_ENTER to R.string.key_enter,
            KeyEvent.KEYCODE_PROG_RED to R.string.key_red,
            KeyEvent.KEYCODE_PROG_GREEN to R.string.key_green,
            KeyEvent.KEYCODE_PROG_YELLOW to R.string.key_yellow,
            KeyEvent.KEYCODE_PROG_BLUE to R.string.key_blue,
            KeyEvent.KEYCODE_INFO to "Info",
            KeyEvent.KEYCODE_MENU to "Menu",
            KeyEvent.KEYCODE_GUIDE to "Guide",
            KeyEvent.KEYCODE_CHANNEL_UP to R.string.key_channel_up,
            KeyEvent.KEYCODE_CHANNEL_DOWN to R.string.key_channel_down,
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE to R.string.key_play_pause,
            KeyEvent.KEYCODE_MEDIA_NEXT to R.string.key_next,
            KeyEvent.KEYCODE_MEDIA_PREVIOUS to R.string.key_previous,
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD to R.string.key_ff,
            KeyEvent.KEYCODE_MEDIA_REWIND to R.string.key_rewind,
            KeyEvent.KEYCODE_CAPTIONS to R.string.key_captions,
        )

        fun keyName(code: Int): String = NAMES[code]?.let { if (it is Int) L.s(it) else it.toString() }
            ?: if (code in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9) L.s(R.string.key_digit, code - KeyEvent.KEYCODE_0)
            else KeyEvent.keyCodeToString(code).removePrefix("KEYCODE_").lowercase().replace('_', ' ')
    }
}
