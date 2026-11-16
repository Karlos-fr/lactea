package fr.lactea.input

import android.view.KeyEvent
import fr.lactea.settings.Settings
import org.json.JSONArray
import org.json.JSONObject

/** Everything the remote can trigger. */
enum class Action(val label: String) {
    NEXT_PRESET("Preset suivant"),
    PREVIOUS_PRESET("Preset précédent"),
    TOGGLE_LOCK("Verrouiller le preset"),
    TOGGLE_SHUFFLE("Mode aléatoire"),
    TOGGLE_FAVORITE("Ajouter / retirer des favoris"),
    TOGGLE_BLACKLIST("Exclure / réintégrer le preset"),
    SHOW_TITLE("Afficher le titre"),
    SHOW_PRESET_NAME("Afficher le nom du preset"),
    SENSITIVITY_UP("Sensibilité audio +"),
    SENSITIVITY_DOWN("Sensibilité audio −"),
    OPEN_OPTIONS("Panneau d'options"),
    OPEN_BROWSER("Navigateur de presets"),
    SHOW_HELP("Aide des touches"),
    TOGGLE_HUD("Compteur FPS"),
    PLAY_PAUSE("Musique : lecture / pause"),
    NEXT_TRACK("Musique : morceau suivant"),
    PREVIOUS_TRACK("Musique : morceau précédent"),
}

/** A physical key, pressed briefly or held. */
data class Trigger(val keyCode: Int, val long: Boolean) {
    val label: String get() = KeyBindings.keyName(keyCode) + if (long) " (appui long)" else ""
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

        private val NAMES = mapOf(
            KeyEvent.KEYCODE_DPAD_RIGHT to "▶ Droite",
            KeyEvent.KEYCODE_DPAD_LEFT to "◀ Gauche",
            KeyEvent.KEYCODE_DPAD_UP to "▲ Haut",
            KeyEvent.KEYCODE_DPAD_DOWN to "▼ Bas",
            KeyEvent.KEYCODE_DPAD_CENTER to "OK",
            KeyEvent.KEYCODE_ENTER to "Entrée",
            KeyEvent.KEYCODE_PROG_RED to "🔴 Rouge",
            KeyEvent.KEYCODE_PROG_GREEN to "🟢 Vert",
            KeyEvent.KEYCODE_PROG_YELLOW to "🟡 Jaune",
            KeyEvent.KEYCODE_PROG_BLUE to "🔵 Bleu",
            KeyEvent.KEYCODE_INFO to "Info",
            KeyEvent.KEYCODE_MENU to "Menu",
            KeyEvent.KEYCODE_GUIDE to "Guide",
            KeyEvent.KEYCODE_CHANNEL_UP to "Chaîne +",
            KeyEvent.KEYCODE_CHANNEL_DOWN to "Chaîne −",
            KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE to "Lecture/Pause",
            KeyEvent.KEYCODE_MEDIA_NEXT to "Suivant ⏭",
            KeyEvent.KEYCODE_MEDIA_PREVIOUS to "Précédent ⏮",
            KeyEvent.KEYCODE_MEDIA_FAST_FORWARD to "Avance rapide",
            KeyEvent.KEYCODE_MEDIA_REWIND to "Retour rapide",
            KeyEvent.KEYCODE_CAPTIONS to "Sous-titres",
        )

        fun keyName(code: Int): String = NAMES[code]
            ?: if (code in KeyEvent.KEYCODE_0..KeyEvent.KEYCODE_9) "Touche ${code - KeyEvent.KEYCODE_0}"
            else KeyEvent.keyCodeToString(code).removePrefix("KEYCODE_").lowercase().replace('_', ' ')
    }
}
