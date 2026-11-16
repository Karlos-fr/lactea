package fr.lactea.ui

import android.content.Context
import android.view.KeyEvent
import android.widget.TextView
import fr.lactea.presets.Playlist
import fr.lactea.presets.PresetStats
import fr.lactea.settings.Settings

/**
 * Preset browser: ◀▶ change category, ▲▼ browse, OK play, 🟡 favourite, 🔴 exclude.
 */
class BrowserPanel(
    context: Context,
    private val settings: Settings,
    private val stats: PresetStats,
    private val playlist: Playlist,
    private val onPlay: (Playlist.Entry) -> Unit,
    private val onClose: () -> Unit,
) : ListPanel(context, 16) {

    private var categories: List<String> = emptyList()
    private var categoryIndex = 0
    private var entries: List<Playlist.Entry> = emptyList()

    init {
        titleView.text = "Navigateur de presets"
        footerView.text = "◀▶ catégorie   OK lire   🟡 favori   🔴 exclure   Retour fermer"
        reload(focus = playlist.current())
    }

    fun reload(focus: Playlist.Entry? = null) {
        val all = playlist.all()
        categories = listOf(ALL) + all.map { it.category }.distinct()
        focus?.let { f -> categoryIndex = 0; filter(); selected = entries.indexOfFirst { it.key == f.key }.coerceAtLeast(0) }
            ?: filter()
        setCount(entries.size)
    }

    private fun filter() {
        val cat = categories.getOrElse(categoryIndex) { ALL }
        val all = playlist.all()
        entries = if (cat == ALL) all else all.filter { it.category == cat }
        subtitleView.text = "Catégorie : $cat  (${entries.size} presets)   ★ favori   ✕ exclu   🐢 lent"
    }

    override fun bindRow(index: Int, label: TextView, value: TextView, isSelected: Boolean) {
        val e = entries[index]
        val marks = buildString {
            if (e.key in settings.favorites) append("★ ")
            if (e.key in settings.blacklist) append("✕ ")
            if (stats.isSlow(e.key, settings.fpsCap)) append("🐢 ")
        }
        label.text = marks + e.key.removePrefix("${e.category}/").let { if (categoryIndex == 0) e.key else it }
        val m = stats.measured(e.key)
        value.text = m.entries.sortedBy { it.key }.joinToString("  ") { "${it.key}p ${it.value.toInt()}" }
    }

    override fun onKey(e: KeyEvent): Boolean {
        if (e.keyCode == KeyEvent.KEYCODE_BACK) {
            if (e.action == KeyEvent.ACTION_UP) onClose()
            return true
        }
        if (e.action != KeyEvent.ACTION_DOWN || e.repeatCount > 0 && e.keyCode !in ARROWS) return true
        val entry = entries.getOrNull(selected)
        when (e.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT -> {
                val n = categories.size
                categoryIndex = ((categoryIndex + if (e.keyCode == KeyEvent.KEYCODE_DPAD_RIGHT) 1 else -1) % n + n) % n
                filter()
                setCount(entries.size, keepSelection = false)
            }
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> entry?.let(onPlay)
            KeyEvent.KEYCODE_PROG_YELLOW -> entry?.let { settings.toggleFavorite(it.key); refresh() }
            KeyEvent.KEYCODE_PROG_RED -> entry?.let { settings.toggleBlacklist(it.key); refresh() }
        }
        return true
    }

    companion object {
        private const val ALL = "Toutes"
        private val ARROWS = setOf(KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT)
    }
}
