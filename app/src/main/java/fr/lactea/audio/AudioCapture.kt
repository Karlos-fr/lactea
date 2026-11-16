package fr.lactea.audio

import android.media.audiofx.Visualizer
import android.util.Log
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.math.sqrt

/**
 * Captures the global output mix (audio session 0) with [Visualizer].
 * Works with any app playing through Android (Spotify, Deezer, YouTube Music...).
 * Buffers are 8-bit unsigned mono PCM, consumed on the GL thread via [drain].
 */
class AudioCapture {
    private var visualizer: Visualizer? = null
    private val queue = ConcurrentLinkedQueue<ByteArray>()

    @Volatile var level: Float = 0f   // RMS 0..1 of the latest buffer, for the overlay
        private set
    @Volatile var status: String = "stopped"
        private set

    fun start() {
        if (visualizer != null) return
        try {
            val v = Visualizer(0)
            v.enabled = false
            v.captureSize = Visualizer.getCaptureSizeRange()[1]
            v.scalingMode = Visualizer.SCALING_MODE_NORMALIZED
            v.setDataCaptureListener(object : Visualizer.OnDataCaptureListener {
                override fun onWaveFormDataCapture(vis: Visualizer, waveform: ByteArray, samplingRate: Int) {
                    val copy = waveform.copyOf()
                    queue.add(copy)
                    while (queue.size > MAX_QUEUED) queue.poll()
                    level = rms(copy)
                }

                override fun onFftDataCapture(vis: Visualizer, fft: ByteArray, samplingRate: Int) = Unit
            }, Visualizer.getMaxCaptureRate(), true, false)
            v.enabled = true
            visualizer = v
            status = "capturing ${v.captureSize} @ ${Visualizer.getMaxCaptureRate() / 1000} Hz"
            Log.i(TAG, "Audio capture started: $status")
        } catch (t: Throwable) {
            status = "capture failed: ${t.message}"
            Log.e(TAG, status, t)
        }
    }

    fun stop() {
        visualizer?.run {
            enabled = false
            release()
        }
        visualizer = null
        queue.clear()
        status = "stopped"
    }

    /** Hands every pending buffer to [consumer]; call from the GL thread. */
    inline fun drain(consumer: (ByteArray) -> Unit) {
        while (true) consumer(poll() ?: return)
    }

    fun poll(): ByteArray? = queue.poll()

    private fun rms(buf: ByteArray): Float {
        var sum = 0.0
        for (b in buf) {
            val s = ((b.toInt() and 0xFF) - 128) / 128.0
            sum += s * s
        }
        return sqrt(sum / buf.size).toFloat()
    }

    companion object {
        private const val TAG = "LACTEA"
        private const val MAX_QUEUED = 8
    }
}
