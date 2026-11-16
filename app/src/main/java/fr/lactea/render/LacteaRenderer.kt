package fr.lactea.render

import android.opengl.GLSurfaceView
import android.os.SystemClock
import android.util.Log
import fr.lactea.audio.AudioCapture
import fr.lactea.engine.NativeEngine
import java.io.File
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Drives projectM on the GLSurfaceView thread: feeds audio, renders, handles
 * preset switching (automatic via projectM's timer/hard cuts, or manual), measures FPS.
 */
class LacteaRenderer(
    private val presets: List<File>,
    private val presetRoot: File,
    private val textureDir: File,
    private val audio: AudioCapture,
) : GLSurfaceView.Renderer {

    private var handle = 0L

    // Shuffled play order with history, so "previous" goes back to what was shown.
    private val order = presets.indices.shuffled().toMutableList()
    private var position = 0

    @Volatile var stats = Stats()
        private set

    data class Stats(
        val fps: Int = 0,
        val frameMsAvg: Float = 0f,
        val frameMsMax: Float = 0f,
        val width: Int = 0,
        val height: Int = 0,
        val preset: String = "",
        val presetIndex: Int = 0,
        val presetCount: Int = 0,
        val lastError: String? = null,
    )

    private var width = 0
    private var height = 0
    private var frames = 0
    private var windowStart = 0L
    private var lastFrame = 0L
    private var frameMsSum = 0f
    private var frameMsMax = 0f
    private var lastError: String? = null

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        if (handle != 0L) NativeEngine.destroy(handle) // context was lost: everything is gone
        handle = NativeEngine.create(textureDir.absolutePath)
        if (handle == 0L) {
            Log.e(TAG, "projectM could not be created")
            return
        }
        NativeEngine.setFps(handle, 60)
        NativeEngine.setMeshSize(handle, 48, 32)
        NativeEngine.setPresetDuration(handle, 25.0)
        NativeEngine.setSoftCutDuration(handle, 3.0)
        NativeEngine.setHardCut(handle, true, 2.0f)
        NativeEngine.setBeatSensitivity(handle, 1.0f)
        loadCurrent(smooth = false)
    }

    override fun onSurfaceChanged(gl: GL10?, w: Int, h: Int) {
        width = w
        height = h
        if (handle != 0L) NativeEngine.resize(handle, w, h)
        Log.i(TAG, "Surface ${w}x$h")
        resetWindow()
    }

    override fun onDrawFrame(gl: GL10?) {
        if (handle == 0L) return
        audio.drain { NativeEngine.addPcmU8(handle, it) }

        when (NativeEngine.consumeSwitchRequest(handle)) {
            1 -> step(+1, smooth = true)
            2 -> step(+1, smooth = false)
        }
        NativeEngine.consumeError(handle)?.let {
            lastError = it
            step(+1, smooth = false) // skip presets that fail to compile
        }

        NativeEngine.render(handle)
        measure()
    }

    /** Called through GLSurfaceView.queueEvent. */
    fun next() = step(+1, smooth = true)
    fun previous() = step(-1, smooth = true)

    private fun step(delta: Int, smooth: Boolean) {
        if (presets.isEmpty()) return
        position = ((position + delta) % order.size + order.size) % order.size
        loadCurrent(smooth)
    }

    private fun loadCurrent(smooth: Boolean) {
        if (presets.isEmpty() || handle == 0L) return
        val file = presets[order[position]]
        NativeEngine.loadPreset(handle, file.absolutePath, smooth)
        Log.i(TAG, "PRESET ${position + 1}/${order.size} ${name(file)}")
        resetWindow()
    }

    private fun name(f: File) = f.relativeTo(presetRoot).path.removeSuffix(".milk")

    private fun resetWindow() {
        frames = 0
        frameMsSum = 0f
        frameMsMax = 0f
        windowStart = SystemClock.elapsedRealtime()
        lastFrame = 0L
    }

    private fun measure() {
        val now = SystemClock.elapsedRealtime()
        if (lastFrame != 0L) {
            val dt = (now - lastFrame).toFloat()
            frameMsSum += dt
            if (dt > frameMsMax) frameMsMax = dt
        }
        lastFrame = now
        frames++
        val elapsed = now - windowStart
        if (elapsed >= 1000) {
            val fps = Math.round(frames * 1000f / elapsed)
            val avg = if (frames > 1) frameMsSum / (frames - 1) else 0f
            val preset = presets.getOrNull(order[position])?.let(::name) ?: ""
            stats = Stats(fps, avg, frameMsMax, width, height, preset, position + 1, order.size, lastError)
            Log.i(TAG, "FPS fps=$fps avgMs=${"%.1f".format(avg)} maxMs=${"%.0f".format(frameMsMax)} " +
                "res=${width}x$height audio=${"%.2f".format(audio.level)} preset=\"$preset\"")
            frames = 0
            frameMsSum = 0f
            frameMsMax = 0f
            windowStart = now
        }
    }

    fun release() {
        if (handle != 0L) NativeEngine.destroy(handle)
        handle = 0L
    }

    companion object {
        private const val TAG = "LACTEA"
    }
}
