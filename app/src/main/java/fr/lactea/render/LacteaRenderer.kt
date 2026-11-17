package fr.lactea.render

import android.opengl.GLSurfaceView
import android.os.SystemClock
import android.util.Log
import fr.lactea.audio.AudioCapture
import fr.lactea.cover.CoverPresets
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

    /** Texture name of the current album cover, or null when covers inside presets are off. */
    @Volatile var coverTexture: String? = null

    /** Keys of the presets that can show the cover (they sample a random texture). */
    @Volatile var coverCompatible: Set<String> = emptySet()

    /** Texture names Lactea uses for random textures (see CoverPresets). */
    @Volatile var randomTextures: List<String> = emptyList()

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
        bench = null // tied to the old projectM instance; the activity restarts it
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

    /**
     * Runs [action] on the GL thread at the next frame. Unlike GLSurfaceView.queueEvent, the GL
     * context is then guaranteed to be current: queued events can run while the view is paused,
     * without a context, which made presets fail to compile ("Error compiling shader", id 0).
     */
    fun post(action: () -> Unit) {
        pending.add(action)
    }

    private val pending = java.util.concurrent.ConcurrentLinkedQueue<() -> Unit>()

    override fun onDrawFrame(gl: GL10?) {
        if (handle == 0L) return
        while (true) (pending.poll() ?: break).invoke()
        if (bench == null) limitFps()
        audio.drain { NativeEngine.addPcmU8(handle, it) }

        val switch = NativeEngine.consumeSwitchRequest(handle)
        if (bench == null) when (switch) {
            1 -> if (!locked) advance(smoothRequested = true)
            2 -> if (!locked) advance(smoothRequested = false)
        }
        NativeEngine.consumeError(handle)?.let { msg ->
            current?.let { playlist.markFailed(it.key) }
            if (bench != null) {
                benchRecord("FAILED", 0f, msg)
            } else {
                listener.onPresetFailed(current, msg)
                advance(smoothRequested = false)
            }
        }

        NativeEngine.render(handle)
        measure()
        bench?.let { benchTick() }
    }

    // ---- Benchmark: every preset in turn, hard cut, settle, then measure ----

    class Bench(
        val entries: List<Playlist.Entry>,
        val out: java.io.File,
        val settleMs: Long,
        val measureMs: Long,
        val onDone: (Int) -> Unit,
    )

    @Volatile var bench: Bench? = null
        private set
    @Volatile var benchProgress = ""
        private set
    private var benchIndex = 0
    private var benchStart = 0L
    private var benchFrames = 0
    private var benchLast = 0L
    private var benchMax = 0f

    fun startBench(b: Bench) {
        if (handle == 0L || b.entries.isEmpty()) {
            b.onDone(0)
            return
        }
        bench = b
        benchIndex = 0
        NativeEngine.setPresetLocked(handle, true)
        NativeEngine.setPresetDuration(handle, 100000.0)
        NativeEngine.setHardCut(handle, false, 0f)
        NativeEngine.setFps(handle, 60)
        benchLoad()
    }

    /** Drops the benchmark without reporting it done (the app was left; it restarts it on return). */
    fun abortBench() {
        if (bench == null) return
        bench = null
        stats.save()
        if (handle != 0L) {
            NativeEngine.setPresetLocked(handle, locked)
            applySettings()
        }
    }

    fun stopBench() {
        val b = bench ?: return
        bench = null
        stats.save()
        NativeEngine.setPresetLocked(handle, locked)
        applySettings()
        b.onDone(benchIndex)
    }

    private fun benchLoad() {
        val b = bench ?: return
        load(b.entries[benchIndex], smooth = false)
        benchStart = SystemClock.elapsedRealtime()
        benchFrames = 0
        benchLast = 0L
        benchMax = 0f
    }

    private fun benchTick() {
        val b = bench ?: return
        val now = SystemClock.elapsedRealtime()
        val elapsed = now - benchStart
        if (elapsed < b.settleMs) return
        if (benchLast != 0L) benchMax = maxOf(benchMax, (now - benchLast).toFloat())
        benchLast = now
        benchFrames++
        if (elapsed >= b.settleMs + b.measureMs) {
            val fps = benchFrames * 1000f / (elapsed - b.settleMs)
            benchRecord("OK", fps, "")
        }
    }

    private fun benchRecord(status: String, fps: Float, message: String) {
        val b = bench ?: return
        val e = b.entries[benchIndex]
        if (status == "OK" && height > 0) stats.record(e.key, height, fps)
        val line = listOf(e.key, status, "%.1f".format(java.util.Locale.ROOT, fps), "%.0f".format(java.util.Locale.ROOT, benchMax),
            "$height", message.replace('\t', ' ').replace('\n', ' ')).joinToString("\t")
        runCatching { b.out.appendText(line + "\n") }
        benchProgress = "${benchIndex + 1}/${b.entries.size}"
        Log.i(TAG, "BENCH $benchProgress $status ${"%.0f".format(fps)} ${e.key}")
        if ((benchIndex + 1) % 50 == 0) stats.save()
        benchIndex++
        if (benchIndex >= b.entries.size) stopBench() else benchLoad()
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

    /** Switches to a random eligible preset able to show the cover, if any. */
    fun jumpToCoverPreset(): Boolean {
        val pool = playlist.eligible().filter { it.key in coverCompatible && it.key != current?.key }
        if (pool.isEmpty()) return false
        jumpTo(pool.random())
        return true
    }

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
        // Presets sampling random textures are rewritten on the fly: album cover + textures picked
        // by Lactea (projectM's own random pick fails on Android).
        val cover = coverTexture
        val rewritten = if (entry.key in coverCompatible || CoverPresets.isCompatible(entry.file)) {
            CoverPresets.read(entry.file)?.let { CoverPresets.rewrite(it, cover, randomTextures) }
        } else null
        val withCover = if (rewritten != null && cover != null) rewritten else null
        if (rewritten != null) NativeEngine.loadPresetData(handle, rewritten, smooth)
        else NativeEngine.loadPreset(handle, entry.file.absolutePath, smooth)
        val now = SystemClock.elapsedRealtime()
        blendUntil = now + (if (smooth) (settings.fadeSeconds * 1000).toLong() else 0L) + SETTLE_MS
        resetMeasure(now)
        Log.i(TAG, "PRESET ${if (smooth) "fade" else "cut"} res=${width}x$height" +
            (if (withCover != null) " cover=$cover" else "") + " ${entry.key}")
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
