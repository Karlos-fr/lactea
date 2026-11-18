package fr.lactea.ui

import android.content.Context
import fr.lactea.R
import fr.lactea.i18n.L
import android.view.KeyEvent
import android.widget.TextView
import fr.lactea.presets.Ambiance
import fr.lactea.presets.Playlist
import fr.lactea.presets.PresetStats
import fr.lactea.settings.Settings

/**
 * Preset browser: ◀▶ change list (all, the ambiance in use, each category), ▲▼ browse, OK play,
 * 🟡 favourite, 🔴 exclude.
 */
class BrowserPanel(
    context: Context,
    private val settings: Settings,
    private val stats: PresetStats,
    private val playlist: Playlist,
    private val onPlay: (Playlist.Entry) -> Unit,
    private val onClose: () -> Unit,
    /** Name of the ambiance in use, shown as a list of its own; null when none. */
    private val ambianceName: String?,
) : ListPanel(context, 16) {

    /** A list the browser can show: its name and which presets it holds. */
    private class Source(
        val name: String,
        val category: String?,
        val subtitleRes: Int = R.string.browser_subtitle,
        val accepts: (Playlist.Entry) -> Boolean,
    )

    private var sources: List<Source> = emptyList()
    private var categoryIndex = 0
    private var entries: List<Playlist.Entry> = emptyList()

    init {
        titleView.text = context.getString(R.string.browser_title)
        footerView.text = context.getString(R.string.browser_footer)
        reload(focus = playlist.current())
    }

    fun reload(focus: Playlist.Entry? = null) {
        val all = playlist.all()
        val ambiance: Ambiance? = settings.activeAmbiance
        sources = listOf(Source(ALL, null) { true }) +
            listOfNotNull(ambiance?.let { a -> Source(ambianceName ?: "", null, R.string.browser_subtitle_ambiance) { a.accepts(it, stats, settings.fpsCap) } }) +
            all.map { it.category }.distinct().map { c -> Source(c, c) { it.category == c } }
        focus?.let { f -> categoryIndex = 0; filter(); selected = entries.indexOfFirst { it.key == f.key }.coerceAtLeast(0) }
            ?: filter()
        setCount(entries.size)
    }

    private fun filter() {
        val source = sources.getOrElse(categoryIndex) { sources.first() }
        val all = playlist.all()
        entries = if (categoryIndex == 0) all else all.filter(source.accepts)
        subtitleView.text = context.getString(source.subtitleRes, source.name, entries.size)
    }

    override fun bindRow(index: Int, label: TextView, value: TextView, isSelected: Boolean) {
        val e = entries[index]
        val marks = buildString {
            if (e.key in settings.favorites) append("★ ")
            if (e.key in settings.blacklist) append("✕ ")
            if (stats.isSlow(e.key, settings.fpsCap)) append("🐢 ")
        }
        val inCategory = sources.getOrNull(categoryIndex)?.category != null
        label.text = marks + if (inCategory) e.key.removePrefix("${e.category}/") else e.key
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
                val n = sources.size
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
        private val ALL get() = L.s(R.string.browser_all)
        private val ARROWS = setOf(KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT)
    }
}
