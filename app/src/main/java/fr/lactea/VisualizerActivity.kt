package fr.lactea

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import fr.lactea.audio.AudioCapture
import fr.lactea.input.Action
import fr.lactea.input.KeyBindings
import fr.lactea.input.RemoteInput
import fr.lactea.input.Trigger
import android.graphics.Bitmap
import fr.lactea.cover.CoverManager
import fr.lactea.cover.CoverPresets
import fr.lactea.dream.LacteaDreamService
import fr.lactea.nowplaying.AutoLaunch
import fr.lactea.nowplaying.NowPlaying
import fr.lactea.packs.PackManager
import fr.lactea.presets.Playlist
import fr.lactea.presets.PresetLibrary
import fr.lactea.presets.PresetStats
import fr.lactea.render.LacteaRenderer
import fr.lactea.settings.Settings
import fr.lactea.ui.BrowserPanel
import fr.lactea.ui.HelpPanel
import fr.lactea.ui.ListPanel
import fr.lactea.ui.MenuItem
import fr.lactea.ui.MenuPanel
import fr.lactea.ui.MenuScreen
import fr.lactea.ui.Osd
import fr.lactea.ui.TitleOverlay
import java.util.concurrent.Executors

/**
 * Lactea main screen: projectM full screen, remote control actions, options panel,
 * preset browser, help, key remapping and preset pack downloads.
 *
 * Dev/benchmark extras:  am start -n fr.lactea/.VisualizerActivity --ei res 1080   (0 = Auto)
 *                                                                   --es manifest http://host/manifest.json
 */
class VisualizerActivity : Activity(), LacteaRenderer.Listener, NowPlaying.Listener {

    private lateinit var settings: Settings
    private lateinit var bindings: KeyBindings
    private lateinit var stats: PresetStats
    private lateinit var playlist: Playlist
    private lateinit var library: PresetLibrary
    private lateinit var packs: PackManager
    private lateinit var renderer: LacteaRenderer
    private lateinit var input: RemoteInput
    private lateinit var nowPlaying: NowPlaying
    private lateinit var titleOverlay: TitleOverlay
    private lateinit var covers: CoverManager
    private var coverJumpTrack: String? = null

    private lateinit var glView: GLSurfaceView
    private lateinit var hud: TextView
    private lateinit var presetLabel: TextView
    private lateinit var osd: Osd
    private lateinit var captureBox: TextView
    private lateinit var panelLayer: FrameLayout

    private val audio = AudioCapture()
    private val ui = Handler(Looper.getMainLooper())
    private val io = Executors.newSingleThreadExecutor()
    private val version by lazy { packageManager.getPackageInfo(packageName, 0).versionName ?: "?" }

    private var panel: ListPanel? = null
    private var swallowUpKey = -1
    private var currentEvent: KeyEvent? = null
    private var locked = false
    private var autoHeight = 720
    private var autoLaunched = false
    private var musicPlayingAtPause = false

    // Key capture (remapping)
    private var captureAction: Action? = null
    private var captureKey = -1
    private var captureDown = 0L
    private var captureLong = false
    private var captureConflict: Trigger? = null

    // Double-press confirmations in menus
    private var confirmKey: String? = null
    private var confirmUntil = 0L

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        settings = Settings(this)
        bindings = KeyBindings(settings)
        stats = PresetStats(settings)
        PresetStats.loadReferenceAsync(this, stats)
        playlist = Playlist(settings, stats)
        library = PresetLibrary(this)
        packs = PackManager(this, settings)
        input = RemoteInput(bindings, ::onAction)
        nowPlaying = NowPlaying(this, this)
        covers = CoverManager(this, settings)
        applyIntent(intent)

        library.prepareBundled()
        playlist.setLibrary(library.scan(packs.presetRoots()))
        Log.i(TAG, "${playlist.all().size} presets, ${packs.installedIds().size} packs")

        renderer = LacteaRenderer(settings, playlist, stats, audio, this)
        renderer.texturePaths = texturePaths()
        indexCoverPresets()

        glView = GLSurfaceView(this).apply {
            setEGLContextClientVersion(3)
            setEGLConfigChooser(8, 8, 8, 8, 0, 0)
            preserveEGLContextOnPause = true
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
        buildViews()
        applyResolution()
        if (playlist.all().isEmpty()) osd.show(getString(R.string.osd_no_presets), 5000)
        // Extras of the launching intent (bench, preset…): onNewIntent only sees later ones.
        applyDevExtras(intent)
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        applyIntent(intent)
        applyResolution()
        applyDevExtras(intent)
    }

    private fun applyDevExtras(intent: Intent) {
        // Dev/test: am start -n fr.lactea/.VisualizerActivity --es preset "Category/name"
        //       or --es preset_b64 <base64 of the key> (names with quotes, parentheses…)
        val presetKey = intent.getStringExtra("preset")
            ?: intent.getStringExtra("preset_b64")?.let {
                runCatching { String(android.util.Base64.decode(it, android.util.Base64.DEFAULT), Charsets.UTF_8) }.getOrNull()
            }
        // Benchmark: --es bench all | <category prefix>   (results: files/bench/bench.tsv, resumable)
        intent.getStringExtra("bench")?.let { startBench(it) }
        if (intent.getBooleanExtra("bench_stop", false)) interruptBench()
        presetKey?.let { key ->
            playlist.all().firstOrNull { it.key == key }?.let { e -> renderer.post { renderer.jumpTo(e) } }
                ?: Log.w(TAG, "Unknown preset $key")
        }
    }

    private fun applyIntent(intent: Intent?) {
        intent ?: return
        autoLaunched = intent.getBooleanExtra(EXTRA_AUTO_LAUNCHED, false)
        if (autoLaunched) Log.i(TAG, "Opened automatically (music playing)")
        if (intent.hasExtra("res")) {
            val res = intent.getIntExtra("res", 0)
            if (res == 0 || res in Settings.RESOLUTIONS) settings.resolution = res
        }
        intent.getStringExtra("manifest")?.let { settings.manifestUrl = it }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUi()
        glView.onResume()
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            audio.start(settings.captureMode == 1)
        } else {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQ_AUDIO)
        }
        nowPlaying.start()
        AutoLaunch.visualizerVisible = true
        ui.post(tick)
        // A benchmark interrupted by leaving the app (settings, Home…) resumes where it stopped.
        if (benchPaused) {
            benchPaused = false
            benchFilter?.let { startBench(it) }
        }
    }

    override fun onPause() {
        super.onPause()
        AutoLaunch.visualizerVisible = false
        musicPlayingAtPause = nowPlaying.playing
        ui.removeCallbacks(tick)
        ui.removeCallbacks(autoClose)
        nowPlaying.stop()
        audio.stop()
        glView.onPause()
        stats.save()
        if (benchFilter != null) {
            benchPaused = true
            benchRunning = false
            renderer.post { renderer.abortBench() }
        }
    }

    override fun onStop() {
        super.onStop()
        // Left while music plays (Home, another app…): don't reopen automatically for this session.
        if (musicPlayingAtPause && !isChangingConfigurations) AutoLaunch.dismissedWhilePlaying = true
    }

    /** Closes an automatically opened Lactea once the music has stayed stopped for a while. */
    private val autoClose = Runnable {
        if (autoLaunched && !nowPlaying.playing) {
            Log.i(TAG, "Music stopped: closing automatically opened Lactea")
            finish()
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, results: IntArray) {
        if (requestCode == REQ_AUDIO && results.firstOrNull() == PackageManager.PERMISSION_GRANTED) {
            audio.start(settings.captureMode == 1)
        }
    }

    // ------------------------------------------------------------------ Keys

    override fun dispatchKeyEvent(e: KeyEvent): Boolean {
        if (autoLaunched && e.action == KeyEvent.ACTION_DOWN && e.keyCode !in MEDIA_KEYS &&
            e.keyCode !in VOLUME_KEYS
        ) {
            autoLaunched = false // the user is now in charge: no automatic close
            ui.removeCallbacks(autoClose)
        }
        if (e.keyCode == swallowUpKey) {
            if (e.action == KeyEvent.ACTION_UP) swallowUpKey = -1
            return true
        }
        if (captureAction != null) return handleCapture(e)
        if (e.keyCode in VOLUME_KEYS) return super.dispatchKeyEvent(e)
        if (benchRunning && e.action == KeyEvent.ACTION_DOWN && e.keyCode !in MEDIA_KEYS) {
            swallowUpKey = e.keyCode
            interruptBench()
            return true
        }
        // Remember the event so a panel opened by it ignores the release of the same key.
        currentEvent = e
        try {
            panel?.let { return it.handleKey(e) }
            if (input.handle(e)) return true
        } finally {
            currentEvent = null
        }
        return super.dispatchKeyEvent(e)
    }

    private fun onAction(a: Action) {
        Log.i(TAG, "ACTION $a")
        when (a) {
            Action.NEXT_PRESET -> renderer.post { renderer.next() }
            Action.PREVIOUS_PRESET -> renderer.post { renderer.previous() }
            Action.TOGGLE_LOCK -> {
                locked = !locked
                val l = locked
                renderer.post { renderer.setLocked(l) }
                osd.show(getString(if (l) R.string.osd_locked else R.string.osd_unlocked))
            }
            Action.TOGGLE_SHUFFLE -> {
                settings.shuffle = !settings.shuffle
                osd.show(getString(if (settings.shuffle) R.string.osd_shuffle_on else R.string.osd_shuffle_off))
            }
            Action.TOGGLE_FAVORITE -> playlist.current()?.let {
                val added = settings.toggleFavorite(it.key)
                osd.show(getString(if (added) R.string.osd_fav_added else R.string.osd_fav_removed, it.name))
            }
            Action.TOGGLE_BLACKLIST -> playlist.current()?.let {
                val added = settings.toggleBlacklist(it.key)
                osd.show(getString(if (added) R.string.osd_excluded else R.string.osd_restored, it.name))
                if (added) renderer.post { renderer.next() }
            }
            Action.SHOW_TITLE -> {
                val t = nowPlaying.track
                when {
                    t != null -> showTitle(t)
                    !nowPlaying.permissionGranted -> osd.show(getString(R.string.osd_title_permission), 4000)
                    else -> osd.show(getString(R.string.osd_no_music))
                }
            }
            Action.SHOW_PRESET_NAME -> playlist.current()?.let { showPresetLabel(it) }
            Action.PLAY_PAUSE -> { nowPlaying.playPause(); osd.show(getString(if (nowPlaying.playing) R.string.osd_pause else R.string.osd_play), 1200) }
            Action.NEXT_TRACK -> { nowPlaying.nextTrack(); osd.show(getString(R.string.osd_next_track), 1200) }
            Action.PREVIOUS_TRACK -> { nowPlaying.previousTrack(); osd.show(getString(R.string.osd_prev_track), 1200) }
            Action.SENSITIVITY_UP, Action.SENSITIVITY_DOWN -> {
                val delta = if (a == Action.SENSITIVITY_UP) 0.1f else -0.1f
                settings.beatSensitivity = (Math.round((settings.beatSensitivity + delta) * 10) / 10f).coerceIn(0.1f, 5f)
                applyRender()
                osd.show(getString(R.string.osd_sensitivity, settings.beatSensitivity), 1200)
            }
            Action.OPEN_OPTIONS -> openOptions()
            Action.OPEN_BROWSER -> openBrowser()
            Action.SHOW_HELP -> openPanel(HelpPanel(this, bindings, ::closePanel), Gravity.CENTER, 0.62f)
            Action.TOGGLE_HUD -> {
                settings.showHud = !settings.showHud
                hud.visibility = if (settings.showHud) View.VISIBLE else View.GONE
            }
        }
    }

    // ------------------------------------------------------------------ Panels

    private fun openPanel(p: ListPanel, gravity: Int, widthFraction: Float) {
        closePanel()
        currentEvent?.let { if (it.action == KeyEvent.ACTION_DOWN) swallowUpKey = it.keyCode }
        val w = (resources.displayMetrics.widthPixels * widthFraction).toInt()
        val margin = dp(36)
        panelLayer.addView(p, FrameLayout.LayoutParams(w, FrameLayout.LayoutParams.WRAP_CONTENT, gravity).apply {
            setMargins(margin, margin, margin, margin)
        })
        panel = p
        presetLabel.visibility = View.GONE
    }

    private fun closePanel() {
        panel?.let { panelLayer.removeView(it) }
        panel = null
    }

    private fun openBrowser() {
        openPanel(BrowserPanel(this, settings, stats, playlist,
            onPlay = { e -> renderer.post { renderer.jumpTo(e) }; closePanel() },
            onClose = ::closePanel), Gravity.START or Gravity.CENTER_VERTICAL, 0.62f)
    }

    private fun openOptions() {
        openPanel(MenuPanel(this, rootScreen(), ::closePanel), Gravity.END or Gravity.CENTER_VERTICAL, 0.5f)
    }

    private val menu: MenuPanel? get() = panel as? MenuPanel

    private fun confirm(key: String, action: () -> Unit) {
        val now = SystemClock.uptimeMillis()
        if (confirmKey == key && now < confirmUntil) {
            confirmKey = null
            action()
        } else {
            confirmKey = key
            confirmUntil = now + 4000
        }
    }

    private fun confirmLabel(key: String, normal: String) =
        if (confirmKey == key && SystemClock.uptimeMillis() < confirmUntil) getString(R.string.confirm_again) else normal

    private fun rootScreen(): MenuScreen = MenuScreen(getString(R.string.menu_options), getString(R.string.menu_version, version)) {
        val resLabels = listOf(getString(R.string.res_auto)) + Settings.RESOLUTIONS.map { if (it == 2160) getString(R.string.res_4k) else "${it}p" }
        val autoMaxChoices = listOf(720, 1080, 1440, 2160)
        val durations = listOf(10, 15, 20, 25, 30, 45, 60, 90, 120)
        val fades = listOf(0.5f, 1f, 1.5f, 2f, 3f, 5f)
        val cutSens = listOf(0.5f, 1f, 1.5f, 2f, 3f, 4f, 5f)
        val thresholds = listOf(15, 20, 25, 30, 40)
        val sens = (1..30).map { it / 10f }
        val modes = listOf(Settings.TRANSITION_AUTO, Settings.TRANSITION_FADE, Settings.TRANSITION_CUT)
        val titleDurations = listOf(3, 4, 6, 8, 12)
        val autoDelays = listOf(0, 10, 30, 60, 120, 300)
        listOf(
            MenuItem.Header(getString(R.string.h_visual)),
            MenuItem.Choice(getString(R.string.opt_resolution), resLabels,
                { if (settings.resolution == 0) 0 else Settings.RESOLUTIONS.indexOf(settings.resolution) + 1 },
                { settings.resolution = if (it == 0) 0 else Settings.RESOLUTIONS[it - 1]; applyResolution() }),
            MenuItem.Choice(getString(R.string.opt_auto_max), autoMaxChoices.map { "${it}p" },
                { autoMaxChoices.indexOf(settings.autoMaxResolution).coerceAtLeast(0) },
                { settings.autoMaxResolution = autoMaxChoices[it] }),
            MenuItem.Choice(getString(R.string.opt_fps), listOf("30", "60"),
                { if (settings.fpsCap == 30) 0 else 1 },
                { settings.fpsCap = if (it == 0) 30 else 60; applyRender() }),
            MenuItem.Choice(getString(R.string.opt_mesh), Settings.MESH_SIZES.map { "${it.first}×${it.second}" },
                { settings.meshIndex }, { settings.meshIndex = it; applyRender() }),
            MenuItem.Toggle(getString(R.string.opt_hud), { settings.showHud }, {
                settings.showHud = it; hud.visibility = if (it) View.VISIBLE else View.GONE
            }),

            MenuItem.Header(getString(R.string.h_presets)),
            MenuItem.Choice(getString(R.string.opt_duration), durations.map { "$it s" },
                { durations.indexOf(settings.presetDuration).coerceAtLeast(0) },
                { settings.presetDuration = durations[it]; applyRender() }),
            MenuItem.Choice(getString(R.string.opt_transition), listOf(getString(R.string.tr_auto), getString(R.string.tr_fade), getString(R.string.tr_cut)),
                { modes.indexOf(settings.transitionMode).coerceAtLeast(0) },
                { settings.transitionMode = modes[it] }),
            MenuItem.Choice(getString(R.string.opt_fade), fades.map { "${"%.1f".format(it)} s" },
                { fades.indexOf(settings.fadeSeconds).coerceAtLeast(0) },
                { settings.fadeSeconds = fades[it]; applyRender() }),
            MenuItem.Toggle(getString(R.string.opt_beat_cuts), { settings.hardCutsOnBeat },
                { settings.hardCutsOnBeat = it; applyRender() }),
            MenuItem.Choice(getString(R.string.opt_cut_sens), cutSens.map { "%.1f".format(it) },
                { cutSens.indexOf(settings.hardCutSensitivity).coerceAtLeast(0) },
                { settings.hardCutSensitivity = cutSens[it]; applyRender() }),
            MenuItem.Toggle(getString(R.string.opt_shuffle), { settings.shuffle }, { settings.shuffle = it }),
            MenuItem.Toggle(getString(R.string.opt_fav_only), { settings.favoritesOnly }, { settings.favoritesOnly = it }),
            MenuItem.Toggle(getString(R.string.opt_name_on_change), { settings.showPresetNameOnChange },
                { settings.showPresetNameOnChange = it }),

            MenuItem.Header(getString(R.string.h_title)),
            MenuItem.Toggle(getString(R.string.opt_title_each), { settings.showTitleOnTrackChange },
                { settings.showTitleOnTrackChange = it }),
            MenuItem.Choice(getString(R.string.opt_title_duration), titleDurations.map { "$it s" },
                { titleDurations.indexOf(settings.titleSeconds).coerceAtLeast(0) },
                { settings.titleSeconds = titleDurations[it]; previewTitle() }),
            MenuItem.Choice(getString(R.string.opt_position), listOf(getString(R.string.pos_center), getString(R.string.pos_bottom)),
                { settings.titlePosition }, { settings.titlePosition = it; previewTitle() }),
            MenuItem.Choice(getString(R.string.opt_size), listOf(getString(R.string.size_small), getString(R.string.size_medium), getString(R.string.size_large)),
                { settings.titleSize }, { settings.titleSize = it; previewTitle() }),
            MenuItem.Toggle(getString(R.string.opt_new_preset_track), { settings.newPresetOnTrack },
                { settings.newPresetOnTrack = it }),
            MenuItem.Info(getString(R.string.info_now_playing)) {
                nowPlaying.track?.let { "${it.title} — ${it.source}" }
                    ?: getString(if (nowPlaying.permissionGranted) R.string.np_none else R.string.np_no_permission)
            },
            MenuItem.Button(getString(R.string.btn_title_howto),
                { if (nowPlaying.permissionGranted) getString(R.string.allowed) else "" }) {
                osd.show(getString(R.string.osd_title_howto, NowPlaying.ADB_GRANT), 12000)
            },

            MenuItem.Header(getString(R.string.h_cover)),
            MenuItem.Toggle(getString(R.string.opt_cover_title), { settings.coverWithTitle },
                { settings.coverWithTitle = it; previewTitle() }),
            MenuItem.Toggle(getString(R.string.opt_cover_pulse), { settings.coverPulse },
                { settings.coverPulse = it; previewTitle() }),
            MenuItem.Toggle(getString(R.string.opt_cover_presets), { settings.coverInPresets },
                { settings.coverInPresets = it; applyCoverSettings() }),
            MenuItem.Toggle(getString(R.string.opt_cover_preset_track), { settings.coverPresetOnTrack },
                { settings.coverPresetOnTrack = it }),
            MenuItem.Toggle(getString(R.string.opt_cover_online), { settings.coverOnline },
                { settings.coverOnline = it }),
            MenuItem.Info(getString(R.string.info_cover)) { covers.current?.source ?: getString(R.string.none) },
            MenuItem.Info(getString(R.string.info_cover_presets)) { "${renderer.coverCompatible.size}" },

            MenuItem.Header(getString(R.string.h_auto)),
            MenuItem.Choice(getString(R.string.opt_autolaunch), autoDelays.map {
                if (it == 0) getString(R.string.never) else if (it < 60) getString(R.string.after_s, it) else getString(R.string.after_min, it / 60)
            }, { autoDelays.indexOf(settings.autoLaunchDelay).coerceAtLeast(0) },
                { settings.autoLaunchDelay = autoDelays[it] }),
            MenuItem.Toggle(getString(R.string.opt_music_only), { settings.autoLaunchMusicAppsOnly },
                { settings.autoLaunchMusicAppsOnly = it }),
            MenuItem.Toggle(getString(R.string.opt_autoclose), { settings.autoCloseWhenMusicStops },
                { settings.autoCloseWhenMusicStops = it }),
            MenuItem.Info(getString(R.string.info_overlay)) {
                getString(if (android.provider.Settings.canDrawOverlays(this)) R.string.granted else R.string.missing)
            },
            MenuItem.Info(getString(R.string.info_screensaver)) { screensaverLabel() },
            MenuItem.Button(getString(R.string.btn_setup_howto)) {
                osd.show(getString(R.string.osd_setup_howto,
                    "adb shell appops set $packageName SYSTEM_ALERT_WINDOW allow", LacteaDreamService.ADB_ENABLE), 15000)
            },

            MenuItem.Header(getString(R.string.h_manage)),
            MenuItem.Button(getString(R.string.btn_browser), { getString(R.string.n_presets, playlist.all().size) }) { openBrowser() },
            MenuItem.Toggle(getString(R.string.opt_exclude_slow), { settings.excludeSlow }, { settings.excludeSlow = it }),
            MenuItem.Choice(getString(R.string.opt_slow_threshold), thresholds.map { "< $it" },
                { thresholds.indexOf(settings.slowThreshold).coerceAtLeast(0) },
                { settings.slowThreshold = thresholds[it] }),
            MenuItem.Info(getString(R.string.info_summary)) {
                getString(R.string.summary_value, settings.favorites.size, settings.blacklist.size, stats.slowCount(settings.fpsCap), stats.count())
            },
            MenuItem.Button(getString(R.string.btn_test)) {
                menu?.push(benchScreen())
            },
            MenuItem.Info(getString(R.string.info_reference)) {
                if (stats.referenceCount() == 0) "—" else getString(R.string.reference_value, stats.referenceCount(), stats.referenceDevice)
            },
            MenuItem.Button(getString(R.string.btn_reset_stats), { confirmLabel("stats", "") }) {
                confirm("stats") { stats.reset(); osd.show(getString(R.string.osd_stats_reset)) }
            },
            MenuItem.Button(getString(R.string.btn_clear_blacklist), { confirmLabel("blacklist", "${settings.blacklist.size}") }) {
                confirm("blacklist") { settings.blacklist = emptySet(); osd.show(getString(R.string.osd_blacklist_cleared)) }
            },
            MenuItem.Button(getString(R.string.btn_packs), { getString(R.string.n_installed, packs.installedIds().size) }) {
                menu?.push(packsScreen())
                if (packs.catalog.isEmpty()) refreshCatalog()
            },

            MenuItem.Header(getString(R.string.h_audio)),
            MenuItem.Choice(getString(R.string.opt_audio_sens), sens.map { "%.1f".format(it) },
                { sens.indexOfFirst { kotlin.math.abs(it - settings.beatSensitivity) < 0.05f }.coerceAtLeast(0) },
                { settings.beatSensitivity = sens[it]; applyRender() }),
            MenuItem.Choice(getString(R.string.opt_capture_mode), listOf(getString(R.string.mode_constant), getString(R.string.mode_volume)),
                { settings.captureMode }, { settings.captureMode = it; restartAudio() }),
            MenuItem.Info(getString(R.string.info_capture)) { audio.status },

            MenuItem.Header(getString(R.string.h_remote)),
            MenuItem.Button(getString(R.string.btn_keys)) { menu?.push(remoteScreen()) },
            MenuItem.Button(getString(R.string.btn_key_help)) { onAction(Action.SHOW_HELP) },

            MenuItem.Header(getString(R.string.h_about)),
            MenuItem.Info("Lactea") { version },
            MenuItem.Info(getString(R.string.info_engine)) { getString(R.string.engine_value) },
            MenuItem.Info(getString(R.string.info_presets)) { getString(R.string.presets_value) },
        )
    }

    private val benchFile get() = java.io.File(filesDir, "bench/bench.tsv")

    /** Test of the presets on this TV: everything, or one category at a time. */
    private fun benchScreen(): MenuScreen = MenuScreen(getString(R.string.bench_title), getString(R.string.bench_subtitle)) {
        val done = runCatching { benchFile.readLines().mapTo(HashSet()) { it.substringBefore('\t') } }.getOrDefault(HashSet())
        val all = playlist.all()
        val left = all.count { it.key !in done }
        val items = mutableListOf<MenuItem>(
            MenuItem.Info(getString(R.string.bench_done_count)) { "${all.size - left} / ${all.size}" },
            MenuItem.Button(getString(if (left == all.size) R.string.bench_all else R.string.bench_continue),
                { if (left == 0) getString(R.string.bench_nothing) else getString(R.string.bench_left, left, durationText(left)) }) {
                if (left > 0) { closePanel(); startBench("all") }
            },
            MenuItem.Button(getString(R.string.bench_restart), { confirmLabel("bench-reset", getString(R.string.bench_restart_hint)) }) {
                confirm("bench-reset") { benchFile.delete(); osd.show(getString(R.string.osd_bench_cleared)); menu?.rebuild() }
            },
            MenuItem.Header(getString(R.string.h_by_category)),
        )
        all.groupBy { it.key.substringBefore('/') }.toSortedMap().forEach { (cat, entries) ->
            val n = entries.count { it.key !in done }
            items += MenuItem.Button(cat, { if (n == 0) getString(R.string.bench_cat_done) else getString(R.string.bench_left, n, durationText(n)) }) {
                if (n > 0) { closePanel(); startBench("$cat/") }
            }
        }
        items
    }

    private fun remoteScreen(): MenuScreen = MenuScreen(getString(R.string.h_remote), getString(R.string.remote_subtitle)) {
        listOf<MenuItem>(
            MenuItem.Button(getString(R.string.btn_reset_keys), { confirmLabel("keys", "") }) {
                confirm("keys") { bindings.resetToDefaults(); osd.show(getString(R.string.osd_keys_reset)) }
            },
            MenuItem.Header(getString(R.string.h_actions)),
        ) + Action.entries.map { a ->
            MenuItem.Button(a.label, { bindings.triggersFor(a).joinToString(", ") { it.label }.ifEmpty { "—" } }) {
                menu?.push(actionScreen(a))
            }
        }
    }

    private fun actionScreen(a: Action): MenuScreen = MenuScreen(a.label, getString(R.string.action_subtitle)) {
        listOf<MenuItem>(
            MenuItem.Button(getString(R.string.btn_add_key)) { startCapture(a) },
            MenuItem.Header(getString(R.string.h_current_keys)),
        ) + bindings.triggersFor(a).map { t ->
            if (t in KeyBindings.RESERVED) MenuItem.Info(t.label) { getString(R.string.key_fixed) }
            else MenuItem.Button(getString(R.string.btn_remove_key, t.label)) { bindings.unbind(t) }
        }
    }

    private fun packsScreen(): MenuScreen = MenuScreen(getString(R.string.packs_title), settings.manifestUrl) {
        val items = mutableListOf<MenuItem>(
            MenuItem.Info(getString(R.string.info_catalog)) { packs.catalogStatus },
            MenuItem.Info(getString(R.string.info_state)) { packStateText() },
            MenuItem.Button(getString(R.string.btn_refresh)) { refreshCatalog() },
            MenuItem.Info(getString(R.string.info_disk)) { getString(R.string.size_mb, packs.diskUsageBytes() / 1e6) },
            MenuItem.Header(getString(R.string.h_packs)),
        )
        val catalogIds = packs.catalog.map { it.id }.toSet()
        packs.catalog.forEach { p ->
            items += MenuItem.Button(getString(R.string.pack_label, p.name, p.presets), {
                val installed = packs.installedVersion(p.id)
                when {
                    installed == null -> getString(R.string.pack_install, p.size / 1e6)
                    installed != p.version -> getString(R.string.pack_update, installed, p.version)
                    else -> confirmLabel("pack-${p.id}", getString(R.string.pack_installed))
                }
            }) {
                val installed = packs.installedVersion(p.id)
                if (installed == null || installed != p.version) installPack(p)
                else confirm("pack-${p.id}") { removePack(p.id) }
            }
        }
        packs.installedIds().filter { it !in catalogIds }.forEach { id ->
            items += MenuItem.Button(id, { confirmLabel("pack-$id", getString(R.string.pack_installed)) }) {
                confirm("pack-$id") { removePack(id) }
            }
        }
        items
    }

    private fun packStateText(): String = when (val s = packs.state) {
        is PackManager.State.Idle -> getString(R.string.state_ready)
        is PackManager.State.Downloading ->
            if (s.total > 0) getString(R.string.state_downloading_pct, s.packId, (s.done * 100 / s.total).toInt())
            else getString(R.string.state_downloading, s.packId)
        is PackManager.State.Extracting -> getString(R.string.state_installing, s.packId)
        is PackManager.State.Error -> getString(R.string.state_error, s.message)
    }

    private fun refreshCatalog() = packs.refreshCatalog { ui.post { menu?.rebuild() } }

    private fun installPack(p: PackManager.Pack) {
        osd.show(getString(R.string.osd_downloading, p.name))
        packs.install(p) { ok ->
            if (ok) reloadLibrary(getString(R.string.osd_installed, p.name))
            else ui.post { osd.show(getString(R.string.osd_failed, packStateText()), 4000) }
        }
    }

    private fun removePack(id: String) = packs.remove(id) { reloadLibrary(getString(R.string.osd_pack_removed, id)) }

    /** Rescans presets after a pack change (background), then updates playlist and textures. */
    private fun reloadLibrary(message: String) {
        io.execute {
            val entries = library.scan(packs.presetRoots())
            playlist.setLibrary(entries)
            val paths = texturePaths()
            renderer.post { renderer.updateTexturePaths(paths) }
            Log.i(TAG, "Library reloaded: ${entries.size} presets")
            indexCoverPresets()
            ui.post {
                osd.show(getString(R.string.osd_library, message, entries.size))
                menu?.rebuild()
            }
        }
    }

    private fun texturePaths(): List<String> =
        (listOf(library.bundledTextureDir) + packs.textureDirs() +
            (if (settings.coverInPresets) listOf(covers.dir) else emptyList())).map { it.absolutePath }

    /** Finds, in the background, the presets able to show the cover. */
    private fun indexCoverPresets() {
        io.execute {
            val start = SystemClock.elapsedRealtime()
            renderer.randomTextures = CoverPresets.textureNames(listOf(library.bundledTextureDir) + packs.textureDirs())
            val keys = playlist.all().filter { CoverPresets.isCompatible(it.file) }.mapTo(HashSet()) { it.key }
            renderer.coverCompatible = keys
            Log.i(TAG, "Cover-compatible presets: ${keys.size} (${SystemClock.elapsedRealtime() - start} ms)")
        }
    }

    private var benchRunning = false
    /** Filter of the benchmark in progress, kept until it completes or is stopped. */
    private var benchFilter: String? = null
    private var benchPaused = false
    private var benchTotal = 0

    /** About 3.2 s per preset (1 s to settle, 2 s of measure, loading). */
    private fun durationText(presets: Int): String {
        val min = (presets * 3.2 / 60).toInt().coerceAtLeast(1)
        return if (min < 60) getString(R.string.duration_min, min) else getString(R.string.duration_h, min / 60, min % 60)
    }

    /** Stops the benchmark at the user's request; what was measured is kept. */
    private fun interruptBench() {
        benchFilter = null
        benchPaused = false
        renderer.post { renderer.stopBench() }
        osd.show(getString(R.string.osd_bench_interrupted), 8000)
    }

    /**
     * Loads every preset (matching [filter]) in turn at 720p / 60 FPS max, without touching the
     * user's settings, and appends one line per preset to files/bench/bench.tsv. Presets already
     * in the file are skipped, so an interrupted run resumes where it stopped.
     */
    private fun startBench(filter: String) {
        benchFilter = filter
        io.execute {
            val out = java.io.File(filesDir, "bench/bench.tsv").apply { parentFile?.mkdirs() }
            val done = if (out.exists()) out.readLines().mapTo(HashSet()) { it.substringBefore('\t') } else emptySet()
            val entries = playlist.all().filter { (filter == "all" || it.key.startsWith(filter)) && it.key !in done }
            Log.i(TAG, "BENCH start: ${entries.size} presets to measure (${done.size} already done)")
            ui.post {
                benchRunning = true
                titleOverlay.hide()
                glView.holder.setFixedSize(1280, 720)
                benchTotal = entries.size
                if (entries.isNotEmpty()) {
                    osd.show(getString(R.string.osd_bench_start, entries.size, durationText(entries.size)), 8000)
                }
                renderer.post {
                    renderer.startBench(LacteaRenderer.Bench(entries, out, settleMs = 1000, measureMs = 2000) { n ->
                        Log.i(TAG, "BENCH done: $n presets")
                        ui.post {
                            benchRunning = false
                            hud.visibility = if (settings.showHud) View.VISIBLE else View.GONE
                            applyResolution()
                            if (benchFilter != null) {
                                benchFilter = null
                                osd.show(getString(R.string.osd_bench_done, n), 8000)
                            }
                            menu?.rebuild()
                        }
                    })
                }
            }
        }
    }

    /** Cover settings changed: texture paths and the cover used inside presets. */
    private fun applyCoverSettings() {
        val paths = texturePaths()
        val texture = if (settings.coverInPresets) covers.current?.textureName else null
        renderer.post { renderer.updateTexturePaths(paths) }
        renderer.coverTexture = texture
    }

    // ------------------------------------------------------------------ Key capture

    private fun startCapture(a: Action) {
        captureAction = a
        captureKey = -1
        captureConflict = null
        showCaptureBox(getString(R.string.capture_press, a.label))
    }

    private fun handleCapture(e: KeyEvent): Boolean {
        val a = captureAction ?: return true
        captureConflict?.let { t ->
            if (e.action == KeyEvent.ACTION_UP) when (e.keyCode) {
                KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                    bindings.bind(t, a)
                    endCapture("${t.label} → ${a.label}")
                }
                KeyEvent.KEYCODE_BACK -> endCapture(getString(R.string.osd_cancelled))
            }
            return true
        }
        if (e.keyCode == KeyEvent.KEYCODE_BACK) {
            if (e.action == KeyEvent.ACTION_UP) endCapture(getString(R.string.osd_cancelled))
            return true
        }
        when (e.action) {
            KeyEvent.ACTION_DOWN -> if (e.repeatCount == 0) {
                captureKey = e.keyCode
                captureDown = SystemClock.uptimeMillis()
                captureLong = false
            } else if (e.keyCode == captureKey && !captureLong &&
                (e.isLongPress || SystemClock.uptimeMillis() - captureDown >= RemoteInput.LONG_PRESS_MS)
            ) {
                captureLong = true
                showCaptureBox(getString(R.string.capture_long, KeyBindings.keyName(e.keyCode)))
            }
            KeyEvent.ACTION_UP -> if (e.keyCode == captureKey) finishCapture(a, Trigger(e.keyCode, captureLong))
        }
        return true
    }

    private fun finishCapture(a: Action, t: Trigger) {
        captureKey = -1
        when {
            t.keyCode in KeyBindings.FORBIDDEN ->
                showCaptureBox(getString(R.string.capture_forbidden, t.label))
            t in KeyBindings.RESERVED ->
                showCaptureBox(getString(R.string.capture_reserved, t.label))
            else -> when (val existing = bindings.actionFor(t)) {
                null -> { bindings.bind(t, a); endCapture("${t.label} → ${a.label}") }
                a -> endCapture(getString(R.string.capture_same, t.label))
                else -> {
                    captureConflict = t
                    showCaptureBox(getString(R.string.capture_conflict, t.label, existing.label))
                }
            }
        }
    }

    private fun endCapture(message: String) {
        captureAction = null
        captureConflict = null
        captureBox.visibility = View.GONE
        osd.show(message)
        menu?.rebuild()
    }

    private fun showCaptureBox(text: String) {
        captureBox.text = text
        captureBox.visibility = View.VISIBLE
    }

    // ------------------------------------------------------------------ Renderer callbacks (GL thread)

    override fun onPresetChanged(entry: Playlist.Entry, hardCut: Boolean) {
        if (settings.showPresetNameOnChange) ui.post {
            if (panel == null && !benchRunning && titleOverlay.visibility != View.VISIBLE) showPresetLabel(entry)
        }
    }

    override fun onResolutionWanted(height: Int) {
        ui.post {
            autoHeight = height
            if (settings.resolution == 0) applyResolution()
        }
    }

    override fun onPresetFailed(entry: Playlist.Entry?, message: String) {
        Log.w(TAG, "Preset failed: ${entry?.key}: $message")
        ui.post { osd.show(getString(R.string.osd_preset_skipped, entry?.name ?: ""), 2500) }
    }

    // ------------------------------------------------------------------ Now playing (main thread)

    override fun onTrackChanged(track: NowPlaying.Track) {
        // With "cover preset on each track", the switch happens once the cover is ready.
        if (settings.newPresetOnTrack && !settings.coverPresetOnTrack) renderer.post { renderer.next() }
        if (settings.showTitleOnTrackChange && panel == null && !benchRunning) showTitle(track)
    }

    override fun onArtwork(track: NowPlaying.Track, bitmap: Bitmap?, uri: String?) {
        covers.request(track, bitmap, uri) { cover -> onCoverReady(track, cover) }
    }

    private fun onCoverReady(track: NowPlaying.Track, cover: CoverManager.Cover?) {
        renderer.coverTexture = if (settings.coverInPresets) cover?.textureName else null
        if (settings.coverWithTitle) titleOverlay.setCover(track.key, cover)
        if (cover != null && settings.coverInPresets && settings.coverPresetOnTrack && coverJumpTrack != track.key) {
            coverJumpTrack = track.key
            renderer.post { if (!renderer.jumpToCoverPreset()) renderer.next() }
        }
    }

    override fun onPlaybackChanged(playing: Boolean) {
        Log.i(TAG, "Playback ${if (playing) "playing" else "paused"}")
        ui.removeCallbacks(autoClose)
        if (!playing && autoLaunched && settings.autoCloseWhenMusicStops) ui.postDelayed(autoClose, AUTO_CLOSE_MS)
    }

    private fun screensaverLabel(): String {
        val current = runCatching {
            android.provider.Settings.Secure.getString(contentResolver, "screensaver_components")
        }.getOrNull()?.substringBefore(',') ?: return getString(R.string.unknown)
        return if (current.startsWith("$packageName/")) "Lactea ✓" else current.substringBefore('/')
    }

    private fun showTitle(t: NowPlaying.Track) {
        presetLabel.visibility = View.GONE
        val cover = covers.current?.takeIf { settings.coverWithTitle && it.trackKey == t.key }
        titleOverlay.pulse = settings.coverPulse
        titleOverlay.show(t, cover, settings.titleSeconds * 1000L, settings.titlePosition, settings.titleSize)
    }

    /** Shows the title (or a sample) behind the options panel while its settings change. */
    private fun previewTitle() {
        showTitle(nowPlaying.track ?: NowPlaying.Track(getString(R.string.sample_title), getString(R.string.sample_artist), getString(R.string.sample_album), "Lactea"))
    }

    // ------------------------------------------------------------------ Helpers

    private fun applyRender() = renderer.post { renderer.applySettings() }

    private fun restartAudio() {
        audio.stop()
        audio.start(settings.captureMode == 1)
    }

    private fun applyResolution() {
        val h = if (settings.resolution == 0) autoHeight else settings.resolution
        glView.holder.setFixedSize(h * 16 / 9, h)
        Log.i(TAG, "Render resolution: ${h * 16 / 9}x$h (${if (settings.resolution == 0) "auto" else "manual"})")
    }

    private fun showPresetLabel(e: Playlist.Entry) {
        presetLabel.text = "${e.name}\n${e.category}" + if (e.key in settings.favorites) "  ★" else ""
        presetLabel.alpha = 1f
        presetLabel.visibility = View.VISIBLE
        presetLabel.animate().cancel()
        presetLabel.animate().alpha(0f).setStartDelay(3500).setDuration(800)
            .withEndAction { presetLabel.visibility = View.GONE }
    }

    private val tick = object : Runnable {
        override fun run() {
            if (benchRunning) {
                val done = renderer.benchProgress.substringBefore('/').toIntOrNull() ?: 0
                hud.visibility = View.VISIBLE
                hud.text = getString(R.string.hud_bench, done, benchTotal, durationText(benchTotal - done))
            } else if (settings.showHud) {
                val s = renderer.hud
                hud.text = "${s.fps} FPS  ${s.width}×${s.height}" +
                    (if (settings.resolution == 0) getString(R.string.hud_auto) else "") +
                    (if (s.blending) getString(R.string.hud_fade) else "") +
                    (if (locked) "  🔒" else "") +
                    "\n" + getString(R.string.hud_line2, s.avgMs, s.maxMs, audio.level)
            }
            if (packs.state !is PackManager.State.Idle) menu?.rebuild()
            ui.postDelayed(this, 500)
        }
    }

    private fun buildViews() {
        hud = TextView(this).apply {
            setTextColor(Color.WHITE)
            setShadowLayer(4f, 0f, 0f, Color.BLACK)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 13f)
            setPadding(dp(16), dp(10), dp(16), dp(10))
            setBackgroundColor(0x66000000)
            visibility = if (settings.showHud) View.VISIBLE else View.GONE
        }
        presetLabel = TextView(this).apply {
            setTextColor(Color.WHITE)
            setShadowLayer(8f, 0f, 0f, Color.BLACK)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            visibility = View.GONE
        }
        val osdView = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 20f)
            gravity = Gravity.CENTER
        }
        osd = Osd(osdView)
        captureBox = TextView(this).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 22f)
            setTextColor(Color.WHITE)
            gravity = Gravity.CENTER
            setPadding(dp(40), dp(30), dp(40), dp(30))
            background = GradientDrawable().apply {
                setColor(0xF00B0F2E.toInt()); cornerRadius = dp(18).toFloat(); setStroke(dp(2), 0xFF7C5CFF.toInt())
            }
            visibility = View.GONE
        }
        panelLayer = FrameLayout(this)
        titleOverlay = TitleOverlay(this).apply { levelProvider = { audio.level } }

        setContentView(FrameLayout(this).apply {
            addView(glView, FrameLayout.LayoutParams(-1, -1))
            addView(hud, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.START))
            addView(presetLabel, FrameLayout.LayoutParams(-2, -2, Gravity.BOTTOM or Gravity.START).apply {
                setMargins(dp(48), 0, 0, dp(40))
            })
            addView(titleOverlay, FrameLayout.LayoutParams(-1, -2, Gravity.CENTER).apply {
                setMargins(dp(60), 0, dp(60), 0)
            })
            addView(panelLayer, FrameLayout.LayoutParams(-1, -1))
            addView(osdView, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.CENTER_HORIZONTAL).apply {
                topMargin = dp(28)
            })
            addView(captureBox, FrameLayout.LayoutParams(-2, -2, Gravity.CENTER))
        })
    }

    private fun dp(v: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

    @Suppress("DEPRECATION")
    private fun hideSystemUi() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    companion object {
        private const val TAG = "LACTEA"
        private const val REQ_AUDIO = 1
        private const val AUTO_CLOSE_MS = 30_000L
        const val EXTRA_AUTO_LAUNCHED = "auto_launched"
        private val VOLUME_KEYS = setOf(KeyEvent.KEYCODE_VOLUME_UP, KeyEvent.KEYCODE_VOLUME_DOWN, KeyEvent.KEYCODE_VOLUME_MUTE)
        /** Same keys as KeyEvent.isMediaSessionKey(), which only exists from Android 12 (API 31). */
        private val MEDIA_KEYS = setOf(
            KeyEvent.KEYCODE_MEDIA_PLAY, KeyEvent.KEYCODE_MEDIA_PAUSE, KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE,
            KeyEvent.KEYCODE_MUTE, KeyEvent.KEYCODE_HEADSETHOOK, KeyEvent.KEYCODE_MEDIA_STOP,
            KeyEvent.KEYCODE_MEDIA_NEXT, KeyEvent.KEYCODE_MEDIA_PREVIOUS, KeyEvent.KEYCODE_MEDIA_REWIND,
            KeyEvent.KEYCODE_MEDIA_RECORD, KeyEvent.KEYCODE_MEDIA_FAST_FORWARD,
        )
    }
}
