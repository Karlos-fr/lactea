package fr.lactea.render

import android.opengl.GLSurfaceView
import android.os.SystemClock
import android.util.Log
import fr.lactea.audio.AudioCapture
import fr.lactea.engine.NativeEngine
import fr.lactea.presets.Playlist
import fr.lactea.presets.PresetStats
import fr.lactea.settings.Settings
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Drives projectM on the GL thread: audio feed, rendering, preset switching with
 * smart transitions (fade between light presets, hard cut around heavy ones), per-preset
 * Auto resolution, FPS cap and per-preset FPS learning.
 *
 * Public methods other than the GLSurfaceView callbacks must be called via queueEvent.
 */
class LacteaRenderer(
    private val settings: Settings,
    private val playlist: Playlist,
    private val stats: PresetStats,
    private val audio: AudioCapture,
    private val listener: Listener,
) : GLSurfaceView.Renderer {

    interface Listener {
        fun onPresetChanged(entry: Playlist.Entry, hardCut: Boolean)
        fun onResolutionWanted(height: Int)
        fun onPresetFailed(entry: Playlist.Entry?, message: String)
    }

    data class Hud(
        val fps: Int = 0,
        val avgMs: Float = 0f,
        val maxMs: Float = 0f,
        val width: Int = 0,
        val height: Int = 0,
        val blending: Boolean = false,
    )

    @Volatile var hud = Hud()
        private set
    @Volatile var texturePaths: List<String> = emptyList()

    private var handle = 0L
    private var width = 0
    private var height = 0
    private var current: Playlist.Entry? = null
    private var locked = false

    // FPS cap
    private var lastFrameStart = 0L

    // HUD window (1 s)
    private var hudFrames = 0
    private var hudStart = 0L
    private var lastFrame = 0L
    private var hudMsSum = 0f
    private var hudMsMax = 0f

    // Per-preset steady-state measurement (after the blend)
    private var blendUntil = 0L
    private var measureStart = 0L
    private var measureFrames = 0

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        if (handle != 0L) NativeEngine.destroy(handle)
        handle = NativeEngine.create(texturePaths.firstOrNull() ?: "")
        if (handle == 0L) {
            Log.e(TAG, "projectM could not be created")
            return
        }
        NativeEngine.setTexturePaths(handle, texturePaths.toTypedArray())
        applySettings()
        (current ?: playlist.next())?.let { load(it, smooth = false) }
    }

    override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {
        width = w
        height = h
        if (handle != 0L) NativeEngine.resize(handle, w, h)
        Log.i(TAG, "Surface ${w}x$h")
        resetMeasure(SystemClock.elapsedRealtime())
    }

    override fun onDrawFrame(gl: GL10?) {
        if (handle == 0L) return
        limitFps()
        audio.drain { NativeEngine.addPcmU8(handle, it) }

        when (NativeEngine.consumeSwitchRequest(handle)) {
            1 -> if (!locked) advance(smoothRequested = true)
            2 -> if (!locked) advance(smoothRequested = false)
        }
        NativeEngine.consumeError(handle)?.let { msg ->
            listener.onPresetFailed(current, msg)
            advance(smoothRequested = false)
        }

        NativeEngine.render(handle)
        measure()
    }

    // ---- Commands (GL thread) ----

    fun applySettings() {
        if (handle == 0L) return
        val (mw, mh) = settings.meshSize
        NativeEngine.setFps(handle, settings.fpsCap)
        NativeEngine.setMeshSize(handle, mw, mh)
        NativeEngine.setPresetDuration(handle, settings.presetDuration.toDouble())
        NativeEngine.setSoftCutDuration(handle, settings.fadeSeconds.toDouble())
        NativeEngine.setHardCut(handle, settings.hardCutsOnBeat, settings.hardCutSensitivity)
        NativeEngine.setBeatSensitivity(handle, settings.beatSensitivity)
        NativeEngine.setPresetLocked(handle, locked)
    }

    fun updateTexturePaths(paths: List<String>) {
        texturePaths = paths
        if (handle != 0L) NativeEngine.setTexturePaths(handle, paths.toTypedArray())
    }

    fun next() = advance(smoothRequested = true)

    fun previous() {
        playlist.previous()?.let { switchTo(it, smoothRequested = true) }
    }

    fun jumpTo(entry: Playlist.Entry) = switchTo(playlist.jumpTo(entry), smoothRequested = true)

    fun setLocked(value: Boolean) {
        locked = value
        if (handle != 0L) NativeEngine.setPresetLocked(handle, value)
    }

    fun flushStats() = flushMeasure(SystemClock.elapsedRealtime())

    fun release() {
        flushStats()
        if (handle != 0L) NativeEngine.destroy(handle)
        handle = 0L
    }

    // ---- Switching ----

    private fun advance(smoothRequested: Boolean) {
        playlist.next()?.let { switchTo(it, smoothRequested) }
    }

    private fun switchTo(entry: Playlist.Entry, smoothRequested: Boolean) {
        val now = SystemClock.elapsedRealtime()
        flushMeasure(now)
        var smooth = when (settings.transitionMode) {
            Settings.TRANSITION_CUT -> false
            Settings.TRANSITION_FADE -> smoothRequested
            else -> smoothRequested && !isHeavy(current) && !isHeavy(entry)
        }
        if (settings.resolution == 0) {
            val target = chooseAutoHeight(entry)
            if (target != height) {
                smooth = false // the resize is hidden by the cut
                listener.onResolutionWanted(target)
            }
        }
        load(entry, smooth)
    }

    private fun load(entry: Playlist.Entry, smooth: Boolean) {
        if (handle == 0L) return
        current = entry
        NativeEngine.loadPreset(handle, entry.file.absolutePath, smooth)
        val now = SystemClock.elapsedRealtime()
        blendUntil = now + (if (smooth) (settings.fadeSeconds * 1000).toLong() else 0L) + SETTLE_MS
        resetMeasure(now)
        Log.i(TAG, "PRESET ${if (smooth) "fade" else "cut"} res=${width}x$height ${entry.key}")
        listener.onPresetChanged(entry, !smooth)
    }

    /** A preset whose expected FPS at the current size would make a fade stutter. */
    private fun isHeavy(entry: Playlist.Entry?): Boolean {
        entry ?: return false
        val fps = stats.predict(entry.key, if (height > 0) height else 720, settings.fpsCap) ?: return false
        return fps < HEAVY_FPS
    }

    /** Highest resolution (≤ Auto max) expected to hold the FPS cap; unknown presets start at 720p. */
    private fun chooseAutoHeight(entry: Playlist.Entry): Int {
        val target = settings.fpsCap - 5
        val candidates = Settings.RESOLUTIONS.filter { it <= settings.autoMaxResolution }.sortedDescending()
        val measured = stats.measured(entry.key)
        if (measured.isEmpty()) return minOf(DEFAULT_AUTO_HEIGHT, candidates.first())
        for (h in candidates) {
            val p = stats.predict(entry.key, h, settings.fpsCap)
            if (p != null && p >= target) return h
            // Explore one step above a resolution where the preset already ran at the cap.
            if (p == null) {
                val below = Settings.RESOLUTIONS.getOrNull(Settings.RESOLUTIONS.indexOf(h) - 1)
                if (below != null && (measured[below] ?: 0f) >= target) return h
            }
        }
        return candidates.last()
    }

    // ---- Timing ----

    private fun limitFps() {
        val frameMs = 1000L / settings.fpsCap.coerceAtLeast(1)
        if (settings.fpsCap < 60 && lastFrameStart != 0L) {
            val wait = frameMs - (SystemClock.elapsedRealtime() - lastFrameStart)
            if (wait > 1) SystemClock.sleep(wait)
        }
        lastFrameStart = SystemClock.elapsedRealtime()
    }

    private fun resetMeasure(now: Long) {
        measureStart = 0L
        measureFrames = 0
        hudFrames = 0
        hudMsSum = 0f
        hudMsMax = 0f
        hudStart = now
        lastFrame = 0L
    }

    private fun flushMeasure(now: Long) {
        val entry = current ?: return
        if (measureStart == 0L || height == 0) return
        val elapsed = now - measureStart
        if (elapsed >= MIN_MEASURE_MS && measureFrames > 10) {
            val fps = measureFrames * 1000f / elapsed
            stats.record(entry.key, height, fps)
        }
        measureStart = 0L
        measureFrames = 0
    }

    private fun measure() {
        val now = SystemClock.elapsedRealtime()
        if (now >= blendUntil) {
            if (measureStart == 0L) {
                measureStart = now
                measureFrames = 0
            } else {
                measureFrames++
                if (now - measureStart >= FLUSH_EVERY_MS) flushMeasure(now)
            }
        }
        if (lastFrame != 0L) {
            val dt = (now - lastFrame).toFloat()
            hudMsSum += dt
            if (dt > hudMsMax) hudMsMax = dt
        }
        lastFrame = now
        hudFrames++
        val elapsed = now - hudStart
        if (elapsed >= 1000) {
            val fps = Math.round(hudFrames * 1000f / elapsed)
            val avg = if (hudFrames > 1) hudMsSum / (hudFrames - 1) else 0f
            hud = Hud(fps, avg, hudMsMax, width, height, now < blendUntil)
            Log.i(TAG, "FPS fps=$fps avgMs=${"%.1f".format(avg)} maxMs=${"%.0f".format(hudMsMax)} " +
                "res=${width}x$height blend=${now < blendUntil} audio=${"%.2f".format(audio.level)} " +
                "preset=\"${current?.key}\"")
            hudFrames = 0
            hudMsSum = 0f
            hudMsMax = 0f
            hudStart = now
        }
    }

    companion object {
        private const val TAG = "LACTEA"
        private const val SETTLE_MS = 700L
        private const val MIN_MEASURE_MS = 2000L
        private const val FLUSH_EVERY_MS = 10_000L
        private const val HEAVY_FPS = 45f
        private const val DEFAULT_AUTO_HEIGHT = 720
    }
}
