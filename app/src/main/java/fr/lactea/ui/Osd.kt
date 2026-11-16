package fr.lactea.ui

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Handler
import android.os.Looper
import android.util.TypedValue
import android.view.View
import android.widget.TextView

/** Short on-screen messages that fade out (MilkDrop-style notifications). */
class Osd(private val view: TextView) {
    private val handler = Handler(Looper.getMainLooper())
    private val hide = Runnable { view.animate().alpha(0f).setDuration(400).withEndAction { view.visibility = View.GONE } }

    init {
        view.setTextColor(Color.WHITE)
        view.setShadowLayer(6f, 0f, 0f, Color.BLACK)
        val pad = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 14f, view.resources.displayMetrics).toInt()
        view.setPadding(pad * 2, pad, pad * 2, pad)
        view.background = GradientDrawable().apply {
            setColor(0xB30B0F2E.toInt())
            cornerRadius = pad.toFloat()
        }
        view.visibility = View.GONE
    }

    fun show(text: CharSequence, durationMs: Long = 2200) {
        handler.removeCallbacks(hide)
        view.animate().cancel()
        view.text = text
        view.alpha = 1f
        view.visibility = View.VISIBLE
        handler.postDelayed(hide, durationMs)
    }

    fun hideNow() {
        handler.removeCallbacks(hide)
        view.visibility = View.GONE
    }
}
