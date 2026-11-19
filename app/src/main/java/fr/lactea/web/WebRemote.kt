package fr.lactea.web

import android.content.Context
import fr.lactea.R
import fr.lactea.input.Action
import fr.lactea.nowplaying.NowPlaying
import fr.lactea.presets.Ambiance
import fr.lactea.presets.Playlist
import fr.lactea.presets.PresetStats
import fr.lactea.presets.PresetTags
import fr.lactea.settings.Settings
import fr.lactea.ui.MenuItem
import fr.lactea.ui.MenuScreen
import fr.tvremote.core.AppInfo
import fr.tvremote.core.Image
import fr.tvremote.core.Item
import fr.tvremote.core.Result
import fr.tvremote.core.Screen
import fr.tvremote.core.TvRemote
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.security.MessageDigest
import java.util.concurrent.ConcurrentHashMap

/**
 * The web remote of Lactea (TV Remote core, see D:\VibeCoding\tvremote\FORMAT.md): the options
 * menus as they are on the TV, plus three screens of its own: what plays now, the presets (paged,
 * with search) and one preset (preview, play, favourite, tag, playlists).
 *
 * The menus are read from [MenuScreen]s: an item's id is its place in the screen plus a hash of its
 * label, so a phone acting on a screen that changed meanwhile gets an error instead of the wrong
 * setting. Everything here runs on the main thread, except [image].
 */
class WebRemote(private val host: Host) : TvRemote.App {

    interface Host {
        val context: Context
        val version: String
        val language: String
        val settings: Settings
        val playlist: Playlist
        val stats: PresetStats
        val tags: PresetTags
        val previews: Map<String, File>
        val locked: Boolean
        val track: NowPlaying.Track?

        /** The menus the page shows as tabs (Options, Ambiances): every other menu screen is reached from them. */
        fun rootScreens(): List<MenuScreen>

        /** Runs [block] for the web remote (confirmations already asked on the phone); returns the screen it opened. */
        fun webCall(block: () -> Unit): MenuScreen?

        fun ambianceName(a: Ambiance): String
        fun action(a: Action)
        fun play(e: Playlist.Entry)
        fun useAmbiance(id: String)
        fun togglePlaylistPreset(e: Playlist.Entry)
    }

    private var remote: TvRemote? = null
    val url: String? get() = remote?.url()
    /** http://lactea.local:8771/, or null. */
    val localName: String? get() = remote?.localName()
    /** Why the server could not start, or null. */
    val error: String? get() = TvRemote.lastError()

    /** Runs on the main thread when the address or the .local name changed. */
    var onAddressChanged: (() -> Unit)? = null
    val running: Boolean get() = remote?.running() == true

    fun start(): Boolean {
        if (running) return true
        // 8771: Proscenium, on the same TVs, takes 8770.
        val s = host.settings
        remote = TvRemote.start(host.context, this, TvRemote.Options()
            .port(PORT)
            .access(if (s.webFixedAddress) TvRemote.Access.OPEN else TvRemote.Access.TOKEN)
            .hostname(TvRemote.cleanHostname(s.webName, "lactea"))
            .password(s.webPassword)
            .realm("Lactea")
            .onAddressChanged { onAddressChanged?.invoke() })
        remote?.let { android.util.Log.i("Lactea", "Web remote: ${it.url()} ${it.localName() ?: ""}") }
        if (remote != null) pushState()
        return remote != null
    }

    fun stop() {
        remote?.stop()
        remote = null
    }

    /** New address settings (fixed or not, name): the server starts again. */
    fun restart() {
        stop()
        start()
    }

    fun setPassword(p: String) = remote?.setPassword(p)

    fun qr(px: Int) = remote?.qrBitmap(px)

    /** A setting changed on the TV: phones refresh the screen they show. */
    fun changed() = remote?.changed(null)

    private val main = android.os.Handler(android.os.Looper.getMainLooper())
    private val flush = Runnable { changed(); pushState() }

    /**
     * Something changed on the TV outside the menus (preset browser keys, remote actions):
     * phones refresh, once for the changes of the next 800 ms.
     */
    fun changedSoon() {
        main.removeCallbacks(flush)
        main.postDelayed(flush, 800)
    }

    /** The preset, the lock, the favourites or the song changed: the "now playing" bar and screen. */
    fun pushState() {
        val r = remote ?: return
        val e = host.playlist.current()
        val s = JSONObject()
        if (e != null) s.put("preset", JSONObject()
            .put("key", e.key).put("name", e.name).put("category", e.category).put("img", img(e.key) ?: JSONObject.NULL))
        s.put("locked", host.locked)
        s.put("favorite", e != null && e.key in host.settings.favorites)
        s.put("ambiance", host.settings.activeAmbiance?.let(host::ambianceName) ?: str(R.string.ambiance_all))
        host.track?.let { s.put("track", JSONObject().put("title", it.title).put("artist", it.artist)) }
        r.state(s)
        r.changed("live")
    }

    private fun str(id: Int, vararg args: Any): String = host.context.getString(id, *args)

    // ------------------------------------------------------------------ TvRemote.App

    override fun info(): AppInfo = AppInfo("Lactea", host.version, LIVE)
        .lang(host.language)
        .accent(0xFF7C5CFF.toInt())
        .theme("background", "#0B0F2E")
        .tab(LIVE, str(R.string.web_live), null)
        .tab(PRESETS, str(R.string.web_presets), null)
        .tab("ambiances", str(R.string.btn_ambiances), null)
        .tab("options", str(R.string.menu_options), null)

    override fun screen(id: String, q: TvRemote.Query): Screen? = when {
        id == LIVE -> liveScreen()
        id == PRESETS -> presetsScreen(q)
        id.startsWith(PRESET) -> presetScreen(id.removePrefix(PRESET))
        else -> menuScreen(id)?.let(::menu)
    }

    override fun set(screen: String, id: String, value: Any?): Result = when {
        screen == LIVE -> liveSet(id, value)
        screen.startsWith(PRESET) -> presetSet(screen.removePrefix(PRESET), id, value)
        else -> menuCall(screen, id) { item ->
            when (item) {
                is MenuItem.Toggle -> item.set(value as Boolean)
                is MenuItem.Choice -> item.set((value as Number).toInt().coerceIn(0, item.options.lastIndex))
                is MenuItem.Text -> item.set(value as String)
                is MenuItem.Web -> return@menuCall item.onSet?.invoke(id, value) ?: changedError()
                else -> return@menuCall changedError()
            }
            null
        }
    }

    override fun action(screen: String, id: String): Result = when {
        screen == LIVE -> liveAction(id)
        screen == PRESETS -> presetsAction(id)
        screen.startsWith(PRESET) -> presetAction(screen.removePrefix(PRESET), id)
        else -> menuCall(screen, id) { item ->
            when (item) {
                is MenuItem.Button -> {
                    val opens = item.opens
                    if (opens != null) return@menuCall Result.ok().open(opens().id)
                    item.onClick()
                }
                is MenuItem.Web -> return@menuCall item.onAction?.invoke(id) ?: changedError()
                else -> return@menuCall changedError()
            }
            null
        }
    }

    override fun opened(screen: String) {
        menuScreen(screen)?.onOpen?.invoke()
    }

    /** Previews, at img/pv-<hash of the preset key>. Server thread. */
    override fun image(key: String): Image? {
        val presetKey = imageKeys[key] ?: return null
        val file = host.previews[presetKey]?.takeIf { it.exists() } ?: return null
        return runCatching { Image(file.readBytes(), "image/webp", key + "-" + file.length()) }.getOrNull()
    }

    // ------------------------------------------------------------------ the options menus

    private fun itemId(index: Int, item: MenuItem): String =
        if (item is MenuItem.Web) item.id else "i$index.${(item.label.hashCode() and 0xffff).toString(36)}"

    private fun menu(ms: MenuScreen): Screen {
        val s = Screen.of(ms.id, ms.title).subtitle(ms.subtitle)
        ms.build().forEachIndexed { i, it ->
            if (it.tvOnly) return@forEachIndexed
            val id = itemId(i, it)
            s.add(when (it) {
                is MenuItem.Header -> Item.header(it.label)
                is MenuItem.Info -> Item.info(id, it.label, it.value())
                is MenuItem.Toggle -> Item.toggle(id, it.label, it.get())
                is MenuItem.Choice -> Item.choice(id, it.label, it.get()).also { c ->
                    it.options.forEachIndexed { k, o -> c.option(k, o) }
                }
                is MenuItem.Text -> Item.text(id, it.label, it.get())
                is MenuItem.Button -> Item.button(id, it.label).value(it.value()).also { b ->
                    it.opens?.let { o -> b.opens(o().also { t -> known[t.id] = o }.id) }
                    if (it.confirm) b.confirm(it.label + " ?")
                }
                is MenuItem.Web -> it.item()
            })
        }
        return s
    }

    /**
     * Finds item [id] of menu screen [screen] as it is now and runs [block] on it for the web
     * remote. [block] returns null when done, or its own result; when done, the screen it opened,
     * if any, opens on the phone.
     */
    private fun menuCall(screen: String, id: String, block: (MenuItem) -> Result?): Result {
        val ms = menuScreen(screen) ?: return changedError()
        var result: Result? = changedError()
        val opened = host.webCall {
            val items = ms.build()
            val web = items.filterIsInstance<MenuItem.Web>().firstOrNull { id == it.id || id.startsWith(it.id + "/") }
            val item = web ?: run {
                val i = id.removePrefix("i").substringBefore('.').toIntOrNull()
                items.getOrNull(i ?: -1)?.takeIf { !it.tvOnly && itemId(i!!, it) == id }
            }
            if (item != null) result = runCatching { block(item) }.getOrElse { changedError() }
        }
        return result ?: Result.ok().also { r -> opened?.let { r.open(it.id) } }
    }

    private fun changedError() = Result.error(str(R.string.web_changed))

    /** Menu screens already reached, by id: how to build them again. */
    private val known = ConcurrentHashMap<String, () -> MenuScreen>()

    /**
     * The menu screen [id]. A screen is never listed by hand: it is found by following the buttons
     * that open screens, from the tabs (a page reloaded after Lactea restarted asks for a screen
     * not reached yet). Screens shown on the TV only are not followed.
     */
    private fun menuScreen(id: String): MenuScreen? {
        known[id]?.let { return it() }
        val seen = HashSet<String>()
        var level = host.rootScreens()
        repeat(MAX_MENU_DEPTH) {
            val next = ArrayList<MenuScreen>()
            for (s in level) {
                if (!seen.add(s.id)) continue
                if (s.id == id) return s
                for (item in s.build()) {
                    if (item !is MenuItem.Button || item.tvOnly) continue
                    val o = item.opens ?: continue
                    next += o().also { known[it.id] = o }
                }
            }
            level = next
        }
        return null
    }

    // ------------------------------------------------------------------ now playing

    private fun liveScreen(): Screen {
        val s = Screen.of(LIVE, str(R.string.web_live))
        val e = host.playlist.current()
        val settings = host.settings
        if (e != null) {
            s.add(Item.grid("now", "", Item.Check.NONE).tile(Item.tile("0", e.name).img(img(e.key))))
            s.add(Item.info("cat", str(R.string.web_category), e.category))
        }
        s.add(Item.info("track", str(R.string.info_now_playing), host.track?.let { "${it.title} — ${it.artist}" } ?: "—"))
        s.add(Item.button("prev", str(R.string.action_prev_preset)))
        s.add(Item.button("next", str(R.string.action_next_preset)))
        s.add(Item.toggle("lock", str(R.string.action_lock), host.locked))
        val ambiances = settings.ambiances
        s.add(Item.choice("amb", str(R.string.ambiance_active), settings.activeAmbianceId).also { c ->
            c.option("", str(R.string.ambiance_all))
            ambiances.forEach { a -> c.option(a.id, (if (a.playlist) "♪ " else "") + host.ambianceName(a)) }
        })
        if (e != null) {
            s.add(Item.toggle("fav", str(R.string.web_favorite), e.key in settings.favorites))
            settings.activeAmbiance?.takeIf { it.playlist }?.let { a ->
                s.add(Item.toggle("inpl", "♪ " + host.ambianceName(a), a.contains(e.key)))
            }
            s.add(Item.button("excl", str(R.string.action_blacklist)).confirm(str(R.string.action_blacklist) + " ?"))
            s.add(Item.button("details", str(R.string.web_details)).opens(PRESET + e.key))
        }
        return s
    }

    private fun liveSet(id: String, value: Any?): Result {
        val e = host.playlist.current()
        host.webCall {
            when (id) {
                "lock" -> if (value != host.locked) host.action(Action.TOGGLE_LOCK)
                "amb" -> host.useAmbiance(value as String)
                "fav" -> if (e != null && (value == true) != (e.key in host.settings.favorites)) host.action(Action.TOGGLE_FAVORITE)
                "inpl" -> if (e != null) host.togglePlaylistPreset(e)
            }
        }
        pushState()
        return Result.ok()
    }

    private fun liveAction(id: String): Result {
        host.webCall {
            when (id) {
                "prev" -> host.action(Action.PREVIOUS_PRESET)
                "next" -> host.action(Action.NEXT_PRESET)
                "excl" -> host.action(Action.TOGGLE_BLACKLIST)
            }
        }
        return Result.ok()
    }

    // ------------------------------------------------------------------ presets

    private fun presetsScreen(q: TvRemote.Query): Screen {
        val all = host.playlist.all()
        val words = q.q.lowercase().split(' ').filter { it.isNotBlank() }
        val found = all.withIndex().filter { (_, e) -> words.all { e.key.lowercase().contains(it) } }
        val page = found.drop(q.offset).take(q.limit)
        val list = Item.list("p", "", Item.Check.NONE).opens(PRESET)
        page.forEach { (i, e) -> list.row(Item.row("$i", e.name).sub(e.category + marks(e.key)).img(img(e.key))) }
        return Screen.of(PRESETS, str(R.string.web_presets))
            .subtitle(str(R.string.n_presets, found.size))
            .paged(found.size, q.offset, q.limit, true)
            .add(list)
    }

    private fun presetsAction(id: String): Result {
        val i = id.substringAfter('/').toIntOrNull() ?: return changedError()
        val e = host.playlist.all().getOrNull(i) ?: return changedError()
        return Result.ok().open(PRESET + e.key)
    }

    private fun marks(key: String): String = buildString {
        val s = host.settings
        if (key in s.favorites) append("  ★")
        if (key in s.blacklist) append("  ✕")
        when (host.tags.effective(key)) {
            PresetTags.Tag.CHILL -> append("  🌙")
            PresetTags.Tag.FLASH -> append("  ⚡")
            else -> Unit
        }
        if (s.ambiances.any { it.playlist && it.contains(key) }) append("  ♪")
    }

    private fun entry(key: String): Playlist.Entry? = host.playlist.all().firstOrNull { it.key == key }

    private fun presetScreen(key: String): Screen {
        val e = entry(key) ?: return Screen.of(PRESET + key, key.substringAfterLast('/'))
            .add(Item.info("none", str(R.string.web_not_found), ""))
        val settings = host.settings
        val s = Screen.of(PRESET + key, e.name).subtitle(e.category)
        s.add(Item.grid("pv", "", Item.Check.NONE).tile(Item.tile("0", e.name).img(img(key))))
        s.add(Item.button("play", str(R.string.btn_play_now)))
        s.add(Item.toggle("fav", str(R.string.web_favorite), key in settings.favorites))
        s.add(Item.toggle("excl", str(R.string.web_excluded), key in settings.blacklist))
        val measured = host.tags.measured[key]
        s.add(Item.choice("tag", str(R.string.web_tag), host.tags.manual(key)?.id ?: "").also { c ->
            c.option("", str(R.string.web_tag_measured, measured?.let(::tagName) ?: str(R.string.tag_unknown)))
            PresetTags.Tag.entries.forEach { t -> c.option(t.id, tagName(t)) }
        })
        val fps = host.stats.measured(key)
        if (fps.isNotEmpty()) {
            s.add(Item.info("fps", str(R.string.web_frame_rates),
                fps.entries.sortedBy { it.key }.joinToString("   ") { "${it.key}p ${it.value.toInt()}" }))
        }
        val playlists = settings.ambiances.filter { it.playlist }
        if (playlists.isNotEmpty()) {
            s.add(Item.header(str(R.string.web_playlists)))
            playlists.forEach { a -> s.add(Item.toggle("pl.${a.id}", "♪ " + host.ambianceName(a), a.contains(key))) }
        }
        return s
    }

    private fun tagName(t: PresetTags.Tag) = str(when (t) {
        PresetTags.Tag.CHILL -> R.string.tag_chill
        PresetTags.Tag.OK -> R.string.tag_ok
        PresetTags.Tag.FLASH -> R.string.tag_flash
    })

    private fun presetSet(key: String, id: String, value: Any?): Result {
        entry(key) ?: return Result.error(str(R.string.web_not_found))
        val settings = host.settings
        host.webCall {
            when {
                id == "fav" -> if ((value == true) != (key in settings.favorites)) settings.toggleFavorite(key)
                id == "excl" -> if ((value == true) != (key in settings.blacklist)) settings.toggleBlacklist(key)
                id == "tag" -> host.tags.set(key, PresetTags.Tag.of(value as? String))
                id.startsWith("pl.") -> settings.ambiances.firstOrNull { it.id == id.removePrefix("pl.") }?.let { a ->
                    if ((value == true) != a.contains(key)) settings.saveAmbiance(a.toggle(key))
                }
            }
        }
        pushState()
        return Result.ok()
    }

    private fun presetAction(key: String, id: String): Result {
        val e = entry(key) ?: return Result.error(str(R.string.web_not_found))
        if (id == "play") host.webCall { host.play(e) }
        return Result.ok()
    }

    // ------------------------------------------------------------------ playlist screen, as a list

    /** The presets of playlist [a] as a list the phone can reorder; a touch opens the preset. */
    fun playlistList(a: Ambiance, entries: List<Playlist.Entry>): MenuItem.Web = MenuItem.Web(
        "pl",
        item = {
            Item.list("pl", "", Item.Check.NONE).order(true).opens(PRESET).also { l ->
                entries.forEachIndexed { i, e -> l.row(Item.row("$i", e.name).sub(e.category).img(img(e.key))) }
            }
        },
        onSet = { _, value ->
            val ids = value as? JSONArray
            if (ids == null || ids.length() != entries.size) changedError() else {
                val order = (0 until ids.length()).mapNotNull { entries.getOrNull(ids.optString(it).toIntOrNull() ?: -1)?.key }
                val missing = a.presets.filter { k -> entries.none { it.key == k } }   // not installed now: kept at the end
                host.settings.ambiances.firstOrNull { it.id == a.id }?.let { cur ->
                    host.settings.saveAmbiance(cur.copy(presets = order + missing))
                }
                Result.ok()
            }
        },
        onAction = { id ->
            entries.getOrNull(id.substringAfter('/').toIntOrNull() ?: -1)?.let { Result.ok().open(PRESET + it.key) } ?: changedError()
        },
    )

    // ------------------------------------------------------------------ previews

    /** img/… URL of the preview of [key], or null when the packs have none. */
    private fun img(key: String): String? {
        if (host.previews[key] == null) return null
        val h = hash(key)
        imageKeys[h] = key
        return "img/$h"
    }

    private fun hash(key: String): String {
        val d = MessageDigest.getInstance("SHA-1").digest(key.toByteArray())
        return "pv-" + d.take(8).joinToString("") { "%02x".format(it) }
    }

    private val imageKeys = ConcurrentHashMap<String, String>()

    companion object {
        const val PORT = 8771
        const val LIVE = "live"
        const val PRESETS = "presets"
        const val PRESET = "preset:"
        private const val MAX_MENU_DEPTH = 5
    }
}
