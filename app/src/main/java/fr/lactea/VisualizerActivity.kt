package fr.lactea

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.opengl.GLSurfaceView
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.view.View
import android.view.WindowManager
import android.widget.FrameLayout
import android.widget.TextView
import fr.lactea.audio.AudioCapture
import fr.lactea.presets.PresetLibrary
import fr.lactea.render.LacteaRenderer

/**
 * Step 1 visualizer: projectM full screen, audio from the global output mix,
 * adjustable render resolution, FPS hud. Temporary test keys:
 *   ◀ ▶  previous / next preset      ▲ ▼  render resolution up / down
 *   OK   show / hide the hud
 * Benchmark from adb:  am start -n fr.lactea/.VisualizerActivity --ei res 2160
 */
class VisualizerActivity : Activity() {

    private lateinit var glView: GLSurfaceView
    private lateinit var hud: TextView
    private lateinit var renderer: LacteaRenderer
    private val audio = AudioCapture()
    private val ui = Handler(Looper.getMainLooper())

    private var resIndex = RESOLUTIONS.indexOf(1080)
    private var overlayVisible = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        logDisplayModes()

        val library = PresetLibrary(this)
        val presets = library.prepare()
        Log.i(TAG, "${presets.size} presets available")
        renderer = LacteaRenderer(presets, library.presetDir, library.textureDir, audio)

        glView = GLSurfaceView(this).apply {
            setEGLContextClientVersion(3)
            setEGLConfigChooser(8, 8, 8, 8, 0, 0)
            preserveEGLContextOnPause = true
            setRenderer(renderer)
            renderMode = GLSurfaceView.RENDERMODE_CONTINUOUSLY
        }
        hud = TextView(this).apply {
            setTextColor(Color.WHITE)
            setShadowLayer(4f, 0f, 0f, Color.BLACK)
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setPadding(32, 24, 32, 24)
            setBackgroundColor(0x66000000)
        }
        setContentView(FrameLayout(this).apply {
            addView(glView, FrameLayout.LayoutParams(-1, -1))
            addView(hud, FrameLayout.LayoutParams(-2, -2, Gravity.TOP or Gravity.START))
        })

        applyIntent(intent)
        applyResolution()
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        applyIntent(intent)
        applyResolution()
    }

    private fun applyIntent(intent: Intent?) {
        val res = intent?.getIntExtra("res", -1) ?: -1
        if (res > 0) {
            val i = RESOLUTIONS.indexOf(res)
            if (i >= 0) resIndex = i else Log.w(TAG, "Unsupported res $res, use one of $RESOLUTIONS")
        }
    }

    override fun onResume() {
        super.onResume()
        hideSystemUi()
        glView.onResume()
        if (checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            audio.start()
        } else {
            requestPermissions(arrayOf(Manifest.permission.RECORD_AUDIO), REQ_AUDIO)
        }
        ui.post(overlayTick)
    }

    override fun onPause() {
        super.onPause()
        ui.removeCallbacks(overlayTick)
        audio.stop()
        glView.onPause()
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, results: IntArray) {
        if (requestCode == REQ_AUDIO && results.firstOrNull() == PackageManager.PERMISSION_GRANTED) audio.start()
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_DPAD_RIGHT -> glView.queueEvent { renderer.next() }
            KeyEvent.KEYCODE_DPAD_LEFT -> glView.queueEvent { renderer.previous() }
            KeyEvent.KEYCODE_DPAD_UP -> changeResolution(+1)
            KeyEvent.KEYCODE_DPAD_DOWN -> changeResolution(-1)
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> {
                overlayVisible = !overlayVisible
                hud.visibility = if (overlayVisible) View.VISIBLE else View.GONE
            }
            else -> return super.onKeyDown(keyCode, event)
        }
        return true
    }

    private fun changeResolution(delta: Int) {
        resIndex = (resIndex + delta).coerceIn(0, RESOLUTIONS.lastIndex)
        applyResolution()
    }

    /** Render surface at a fixed size; the display hardware scales it to the screen. */
    private fun applyResolution() {
        val h = RESOLUTIONS[resIndex]
        val w = h * 16 / 9
        glView.holder.setFixedSize(w, h)
        Log.i(TAG, "Render resolution requested: ${w}x$h")
    }

    private val overlayTick = object : Runnable {
        override fun run() {
            val s = renderer.stats
            hud.text = buildString {
                append("Lactea — étape 1\n")
                append("${s.fps} FPS  (moy ${"%.1f".format(s.frameMsAvg)} ms, max ${"%.0f".format(s.frameMsMax)} ms)\n")
                append("Rendu ${s.width}×${s.height}\n")
                append("Preset ${s.presetIndex}/${s.presetCount} : ${s.preset}\n")
                append("Audio ${"%.2f".format(audio.level)} — ${audio.status}\n")
                s.lastError?.let { append("Dernière erreur : ${it.take(120)}\n") }
                append("◀ ▶ preset   ▲ ▼ résolution   OK masquer")
            }
            ui.postDelayed(this, 500)
        }
    }

    private fun logDisplayModes() {
        val display = display ?: return
        Log.i(TAG, "Display current mode: ${display.mode}")
        display.supportedModes.forEach { Log.i(TAG, "Display supported mode: $it") }
    }

    @Suppress("DEPRECATION")
    private fun hideSystemUi() {
        window.decorView.systemUiVisibility = (View.SYSTEM_UI_FLAG_FULLSCREEN
            or View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            or View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY)
    }

    companion object {
        private const val TAG = "LACTEA"
        private const val REQ_AUDIO = 1
        val RESOLUTIONS = listOf(540, 720, 1080, 1440, 2160)
    }
}
