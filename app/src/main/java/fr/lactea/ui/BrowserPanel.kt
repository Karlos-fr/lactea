package fr.lactea.ui

import android.content.Context
import fr.lactea.R
import fr.lactea.i18n.L
import android.graphics.BitmapFactory
import android.view.KeyEvent
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import java.io.File
import fr.lactea.presets.Ambiance
import fr.lactea.presets.Playlist
import fr.lactea.presets.PresetStats
import fr.lactea.presets.PresetTags
import fr.lactea.settings.Settings

/**
 * Preset browser: ◀▶ change list (all, the ambiance in use, each category), ▲▼ browse, OK play,
 * 🟡 favourite, 🔴 exclude, 🟢 tag (chill / OK / flash, over the measured one), 🔵 add to / remove
 * from the playlist in use. The selected
 * preset's preview (4 images) shows on the right, and the preset plays behind the browser once the
 * selection rests on it ([setLive]).
 */
class BrowserPanel(
    context: Context,
    private val settings: Settings,
    private val stats: PresetStats,
    private val tags: PresetTags,
    private val previews: Map<String, File>,
    private val playlist: Playlist,
    private val onPlay: (Playlist.Entry) -> Unit,
    /** The selection moved to this preset (the activity plays it behind once it rests there). */
    private val onSelect: (Playlist.Entry) -> Unit,
    private val onClose: () -> Unit,
    /** 🔵 on a preset: add it to / remove it from the playlist in use. */
    private val onPlaylistKey: (Playlist.Entry) -> Unit,
    /** A favourite, an exclusion or a tag changed (the web remote refreshes). */
    private val onChanged: () -> Unit,
    /** Name of the ambiance in use, shown as a list of its own; null when none. */
    private val ambianceName: String?,
) : ListPanel(context, 16) {

    /** A list the browser can show: its name and which presets it holds. */
    private class Source(
        val name: String,
        val category: String?,
        val subtitleRes: Int = R.string.browser_subtitle,
        val entries: () -> List<Playlist.Entry>,
    )

    private var sources: List<Source> = emptyList()
    private var categoryIndex = 0
    private var entries: List<Playlist.Entry> = emptyList()

    private val previewImage = ImageView(context).apply {
        scaleType = ImageView.ScaleType.FIT_CENTER
        adjustViewBounds = true
    }
    private val previewCaption = TextView(context).apply {
        setTextSize(android.util.TypedValue.COMPLEX_UNIT_SP, 14f)
        setTextColor(0xFFB8C0FF.toInt())
    }
    private var previewKey: String? = null

    init {
        setSideView(LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            addView(previewImage, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT))
            addView(previewCaption)
        }, PREVIEW_WIDTH_DP)
        titleView.text = context.getString(R.string.browser_title)
        footerView.text = context.getString(R.string.browser_footer)
        reload(focus = playlist.current())
    }

    fun reload(focus: Playlist.Entry? = null) {
        val all = playlist.all()
        val ambiance: Ambiance? = settings.activeAmbiance
        sources = listOf(Source(ALL, null) { playlist.all() }) +
            listOfNotNull(ambiance?.let { a ->
                // A playlist lists its presets in its order, as they are now (🔵 changes them).
                if (a.playlist) Source(ambianceName ?: "", null, R.string.browser_subtitle_playlist) {
                    playlist.playlistEntries(settings.activeAmbiance) ?: emptyList()
                }
                else Source(ambianceName ?: "", null, R.string.browser_subtitle_ambiance) { all.filter { a.accepts(it, stats, settings.fpsCap, tags) } }
            }) +
            all.map { it.category }.distinct().map { c -> Source(c, c) { all.filter { it.category == c } } }
        focus?.let { f -> categoryIndex = 0; filter(); selected = entries.indexOfFirst { it.key == f.key }.coerceAtLeast(0) }
            ?: filter()
        setCount(entries.size)
    }

    private fun filter() {
        val source = sources.getOrElse(categoryIndex) { sources.first() }
        entries = source.entries()
        subtitleView.text = context.getString(source.subtitleRes, source.name, entries.size)
    }

    /** The playlist in use, read once per refresh. */
    private var inPlaylist: Ambiance? = null

    override fun refresh() {
        inPlaylist = settings.activeAmbiance?.takeIf { it.playlist }
        super.refresh()
    }

    override fun bindRow(index: Int, label: TextView, value: TextView, isSelected: Boolean) {
        val e = entries[index]
        val marks = buildString {
            if (inPlaylist?.contains(e.key) == true) append("♪ ")
            if (e.key in settings.favorites) append("★ ")
            if (e.key in settings.blacklist) append("✕ ")
            if (stats.isSlow(e.key, settings.fpsCap)) append("🐢 ")
            when (tags.effective(e.key)) {
                PresetTags.Tag.CHILL -> append("🌙 ")
                PresetTags.Tag.FLASH -> append("⚡ ")
                else -> Unit
            }
        }
        if (isSelected) showPreview(e)
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
            KeyEvent.KEYCODE_PROG_YELLOW -> entry?.let { settings.toggleFavorite(it.key); refresh(); onChanged() }
            KeyEvent.KEYCODE_PROG_RED -> entry?.let { settings.toggleBlacklist(it.key); refresh(); onChanged() }
            KeyEvent.KEYCODE_PROG_GREEN -> entry?.let { tags.cycle(it.key); previewKey = null; refresh(); onChanged() }
            KeyEvent.KEYCODE_PROG_BLUE -> entry?.let {
                onPlaylistKey(it)
                // In the playlist's own list, a removed preset leaves it.
                if (sources.getOrNull(categoryIndex)?.subtitleRes == R.string.browser_subtitle_playlist) {
                    filter()
                    setCount(entries.size)
                }
                refresh()
            }
        }
        return true
    }

    /** The selected preset's 2 x 2 preview and its tag (measured or set by hand). */
    private fun showPreview(e: Playlist.Entry) {
        if (previewKey == e.key) return
        previewKey = e.key
        onSelect(e)
        val file = previews[e.key]
        val bitmap = file?.takeIf { it.exists() }?.let { BitmapFactory.decodeFile(it.path) }
        previewImage.setImageBitmap(bitmap)
        previewImage.visibility = if (bitmap != null) VISIBLE else GONE
        val tag = tags.effective(e.key)
        val tagText = when (tag) {
            PresetTags.Tag.CHILL -> context.getString(R.string.tag_chill)
            PresetTags.Tag.OK -> context.getString(R.string.tag_ok)
            PresetTags.Tag.FLASH -> context.getString(R.string.tag_flash)
            null -> context.getString(R.string.tag_unknown)
        }
        val origin = context.getString(if (tags.manual(e.key) != null) R.string.tag_by_you else R.string.tag_measured)
        captionBase = (if (bitmap == null) context.getString(R.string.preview_none) + "\n" else "") +
            if (tag == null) tagText else "$tagText · $origin"
        previewCaption.text = captionBase
    }

    private var captionBase = ""

    /** The selected preset [key] now plays behind the browser, which becomes see-through. */
    fun setLive(key: String) {
        setSeeThrough(true)
        if (key == previewKey) previewCaption.text = captionBase + "\n" + context.getString(R.string.preview_live)
    }

    companion object {
        private const val PREVIEW_WIDTH_DP = 320
        private val ALL get() = L.s(R.string.browser_all)
        private val ARROWS = setOf(KeyEvent.KEYCODE_DPAD_LEFT, KeyEvent.KEYCODE_DPAD_RIGHT)
    }
}
