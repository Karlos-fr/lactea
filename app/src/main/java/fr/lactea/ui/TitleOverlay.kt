package fr.lactea.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import fr.lactea.nowplaying.NowPlaying

/**
 * Song title announcement in the spirit of MilkDrop: big glowing title that zooms in,
 * holds, then drifts up and fades away.
 */
class TitleOverlay(context: Context) : LinearLayout(context) {

    private val title = TextView(context).apply {
        setTextColor(Color.WHITE)
        typeface = Typeface.create("sans-serif-light", Typeface.BOLD)
        gravity = Gravity.CENTER
        maxLines = 2
        letterSpacing = 0.04f
    }
    private val artist = TextView(context).apply {
        setTextColor(0xFFE6DCFF.toInt())
        gravity = Gravity.CENTER
        maxLines = 1
        letterSpacing = 0.12f
    }

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        addView(title)
        addView(artist)
        visibility = GONE
    }

    /**
     * @param position 0 = centre, 1 = bottom
     * @param size 0 = small, 1 = medium, 2 = large
     */
    fun show(track: NowPlaying.Track, durationMs: Long, position: Int, size: Int) {
        val titleSp = floatArrayOf(34f, 48f, 64f)[size.coerceIn(0, 2)]
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, titleSp)
        artist.setTextSize(TypedValue.COMPLEX_UNIT_SP, titleSp * 0.45f)
        val glow = GLOWS.random()
        title.setShadowLayer(titleSp * 0.6f, 0f, 0f, glow)
        artist.setShadowLayer(titleSp * 0.4f, 0f, 0f, glow)
        title.text = track.title
        val album = track.album.takeUnless { it.equals(track.title, ignoreCase = true) } ?: ""
        artist.text = listOf(track.artist, album).filter { it.isNotBlank() }.joinToString("  ·  ").uppercase()
        artist.visibility = if (artist.text.isNullOrEmpty()) GONE else VISIBLE

        (layoutParams as? FrameLayout.LayoutParams)?.let {
            it.gravity = if (position == 1) Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL else Gravity.CENTER
            it.bottomMargin = if (position == 1) dp(70) else 0
            layoutParams = it
        }

        animate().cancel()
        visibility = VISIBLE
        alpha = 0f
        scaleX = 0.6f
        scaleY = 0.6f
        translationY = 0f
        animate().alpha(1f).scaleX(1f).scaleY(1f)
            .setStartDelay(0).setDuration(800).setInterpolator(OvershootInterpolator(1.2f))
            .withEndAction {
                animate().alpha(0f).scaleX(1.08f).scaleY(1.08f).translationY(-dp(40).toFloat())
                    .setStartDelay((durationMs - 2000).coerceAtLeast(500))
                    .setDuration(1200).setInterpolator(AccelerateInterpolator())
                    .withEndAction { visibility = GONE; animate().setStartDelay(0) }
            }
    }

    fun hide() {
        animate().cancel()
        visibility = View.GONE
    }

    private fun dp(v: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

    companion object {
        private val GLOWS = intArrayOf(0xFFB04CFF.toInt(), 0xFF3FD0FF.toInt(), 0xFFFF4FB8.toInt(), 0xFF7C5CFF.toInt())
    }
}
