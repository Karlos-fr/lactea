package fr.lactea.dream

import android.Manifest
import android.content.pm.PackageManager
import android.opengl.GLSurfaceView
import android.os.Handler
import android.os.Looper
import android.service.dreams.DreamService
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.widget.FrameLayout
import android.graphics.Bitmap
import fr.lactea.audio.AudioCapture
import fr.lactea.cover.CoverManager
import fr.lactea.cover.CoverPresets
import fr.lactea.nowplaying.AutoLaunch
import fr.lactea.nowplaying.NowPlaying
import fr.lactea.packs.PackManager
import fr.lactea.presets.Playlist
import fr.lactea.presets.PresetLibrary
import fr.lactea.presets.PresetStats
import fr.lactea.render.LacteaRenderer
import fr.lactea.settings.Settings
import fr.lactea.ui.TitleOverlay

/**
 * Lactea as the TV screensaver: the same visualizer, settings and presets as the app,
 * non-interactive (any key wakes the TV up). Activation on Google TV, from a PC:
 *   adb shell settings put secure screensaver_components fr.lactea/fr.lactea.dream.LacteaDreamService
 */
class LacteaDreamService : DreamService(), LacteaRenderer.Listener, NowPlaying.Listener {

    private lateinit var settings: Settings
    private lateinit var stats: PresetStats
    private lateinit var renderer: LacteaRenderer
    private lateinit var glView: GLSurfaceView
    private lateinit var titleOverlay: TitleOverlay
    private lateinit var nowPlaying: NowPlaying
    private lateinit var covers: CoverManager
    private val audio = AudioCapture()
    private val ui = Handler(Looper.getMainLooper())
    private var autoHeight = 720

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        isInteractive = false
        isFullscreen = true
        isScreenBright = true

        settings = Settings(this)
        stats = PresetStats(settings)
        val playlist = Playlist(settings, stats)
        val library = PresetLibrary(this)
        val packs = PackManager(this, settings)
        library.prepareBundled()
        playlist.setLibrary(library.scan(packs.presetRoots()))

        covers = CoverManager(this, settings)
        renderer = LacteaRenderer(settings, playlist, stats, audio, this)
        renderer.texturePaths = (listOf(library.bundledTextureDir) + packs.textureDirs() +
            (if (settings.coverInPresets) listOf(covers.dir) else emptyList())).map { it.absolutePath }
        renderer.randomTextures = CoverPresets.textureNames(listOf(library.bundledTextureDir) + packs.textureDirs())
        Thread {
            renderer.coverCompatible = playlist.all().filter { CoverPresets.isCompatible(it.file) }.mapTo(HashSet()) { it.key }
        }.start()
        nowPlaying = NowPlaying(this, this)

        glView = GLSurfaceView(this).apply {
            setEGLContextClientVersion(3)
            setEGLConfigChooser(8, 8, 8, 8, 0, 0)
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
        titleOverlay = TitleOverlay(this).apply { levelProvider = { audio.level } }
        val side = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 60f, resources.displayMetrics).toInt()
        setContentView(FrameLayout(this).apply {
            addView(glView, FrameLayout.LayoutParams(-1, -1))
            addView(titleOverlay, FrameLayout.LayoutParams(-1, -2, Gravity.CENTER).apply { setMargins(side, 0, side, 0) })
        })
        applyResolution()
        Log.i(TAG, "Dream attached: ${playlist.all().size} presets")
    }

    override fun onDreamingStarted() {
        super.onDreamingStarted()
        AutoLaunch.visualizerVisible = true
        glView.onResume()
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            audio.start(settings.captureMode == 1)
        }
        nowPlaying.start()
        Log.i(TAG, "Dream started")
    }

    override fun onDreamingStopped() {
        AutoLaunch.visualizerVisible = false
        nowPlaying.stop()
        audio.stop()
        glView.onPause()
        stats.save()
        Log.i(TAG, "Dream stopped")
        super.onDreamingStopped()
    }

    private fun applyResolution() {
        val h = if (settings.resolution == 0) autoHeight else settings.resolution
        glView.holder.setFixedSize(h * 16 / 9, h)
    }

    // Renderer callbacks (GL thread)
    override fun onPresetChanged(entry: Playlist.Entry, hardCut: Boolean) = Unit
    override fun onResolutionWanted(height: Int) {
        ui.post { autoHeight = height; if (settings.resolution == 0) applyResolution() }
    }
    override fun onPresetFailed(entry: Playlist.Entry?, message: String) {
        Log.w(TAG, "Dream preset failed: ${entry?.key}: $message")
    }

    // Now playing (main thread)
    override fun onTrackChanged(track: NowPlaying.Track) {
        if (settings.newPresetOnTrack) renderer.post { renderer.next() }
        if (settings.showTitleOnTrackChange) {
            val cover = covers.current?.takeIf { settings.coverWithTitle && it.trackKey == track.key }
            titleOverlay.pulse = settings.coverPulse
            titleOverlay.show(track, cover, settings.titleSeconds * 1000L, settings.titlePosition, settings.titleSize)
        }
    }

    override fun onArtwork(track: NowPlaying.Track, bitmap: Bitmap?, uri: String?) {
        covers.request(track, bitmap, uri) { cover ->
            renderer.coverTexture = if (settings.coverInPresets) cover?.textureName else null
            if (settings.coverWithTitle) titleOverlay.setCover(track.key, cover)
        }
    }

    override fun onPlaybackChanged(playing: Boolean) = Unit

    companion object {
        private const val TAG = "LACTEA"
        const val ADB_ENABLE =
            "adb shell settings put secure screensaver_components fr.lactea/fr.lactea.dream.LacteaDreamService"
    }
}
