package fr.lactea.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Outline
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.util.TypedValue
import android.view.Gravity
import android.view.View
import android.view.ViewOutlineProvider
import android.view.animation.AccelerateInterpolator
import android.view.animation.OvershootInterpolator
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import fr.lactea.cover.CoverManager
import fr.lactea.nowplaying.NowPlaying

/**
 * Song announcement in the spirit of MilkDrop: album cover with a coloured glow (pulsing on
 * the beat), big glowing title and artist; zooms in, holds, then drifts up and fades away.
 */
class TitleOverlay(context: Context) : LinearLayout(context) {

    private val glow = View(context)
    private val coverView = ImageView(context).apply {
        scaleType = ImageView.ScaleType.CENTER_CROP
        clipToOutline = true
        outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(v: View, o: Outline) = o.setRoundRect(0, 0, v.width, v.height, v.width * 0.05f)
        }
        elevation = 12f
    }
    private val coverFrame = FrameLayout(context).apply {
        clipChildren = false
        addView(glow, FrameLayout.LayoutParams(-1, -1))
        addView(coverView, FrameLayout.LayoutParams(-1, -1, Gravity.CENTER))
        visibility = GONE
    }
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

    /** Audio level 0..1, polled every frame for the pulse. */
    var levelProvider: (() -> Float)? = null
    var pulse = true

    private var trackKey: String? = null
    private var coverSize = 0
    private var smoothed = 0f

    init {
        orientation = VERTICAL
        gravity = Gravity.CENTER
        clipChildren = false
        addView(coverFrame)
        addView(title)
        addView(artist)
        visibility = GONE
    }

    /**
     * @param cover null = title only
     * @param position 0 = centre, 1 = bottom
     * @param size 0 = small, 1 = medium, 2 = large
     */
    fun show(track: NowPlaying.Track, cover: CoverManager.Cover?, durationMs: Long, position: Int, size: Int) {
        trackKey = track.key
        val titleSp = floatArrayOf(34f, 48f, 64f)[size.coerceIn(0, 2)]
        title.setTextSize(TypedValue.COMPLEX_UNIT_SP, titleSp)
        artist.setTextSize(TypedValue.COMPLEX_UNIT_SP, titleSp * 0.45f)
        val glowColor = cover?.color ?: GLOWS.random()
        setTextGlow(glowColor, titleSp)
        title.text = track.title
        val album = track.album.takeUnless {
            it.equals(track.title, ignoreCase = true) || it.equals(track.artist, ignoreCase = true)
        } ?: ""
        artist.text = listOf(track.artist, album).filter { it.isNotBlank() }.joinToString("  ·  ").uppercase()
        artist.visibility = if (artist.text.isNullOrEmpty()) GONE else VISIBLE

        // Smaller cover when shown at the bottom, so the visual stays visible.
        coverSize = dp((titleSp * if (position == 1) 2.6f else 3.8f).toInt())
        val pad = (coverSize * 0.22f).toInt()
        coverFrame.layoutParams = LayoutParams(coverSize + 2 * pad, coverSize + 2 * pad).apply { bottomMargin = -pad / 2 }
        (coverView.layoutParams as FrameLayout.LayoutParams).apply { width = coverSize; height = coverSize }
        applyCover(cover, animate = false)

        (layoutParams as? FrameLayout.LayoutParams)?.let {
            it.gravity = if (position == 1) Gravity.BOTTOM or Gravity.CENTER_HORIZONTAL else Gravity.CENTER
            it.bottomMargin = if (position == 1) dp(50) else 0
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
        removeCallbacks(pulseTick)
        postOnAnimation(pulseTick)
    }

    /** The cover often arrives a moment after the title: add it if this track is still shown. */
    fun setCover(key: String, cover: CoverManager.Cover?) {
        if (visibility == VISIBLE && key == trackKey && cover != null) {
            setTextGlow(cover.color, title.textSize / resources.displayMetrics.scaledDensity)
            applyCover(cover, animate = true)
        }
    }

    fun hide() {
        animate().cancel()
        visibility = GONE
    }

    private fun applyCover(cover: CoverManager.Cover?, animate: Boolean) {
        if (cover == null) {
            coverFrame.visibility = GONE
            return
        }
        coverView.setImageBitmap(cover.bitmap)
        glow.background = GradientDrawable().apply {
            gradientType = GradientDrawable.RADIAL_GRADIENT
            gradientRadius = coverSize * 0.75f
            colors = intArrayOf((cover.color and 0x00FFFFFF) or 0xB0000000.toInt(), Color.TRANSPARENT)
        }
        coverFrame.visibility = VISIBLE
        if (animate) {
            coverFrame.alpha = 0f
            coverFrame.animate().alpha(1f).setDuration(500).start()
        } else {
            coverFrame.alpha = 1f
        }
    }

    private fun setTextGlow(color: Int, titleSp: Float) {
        title.setShadowLayer(titleSp * 0.6f, 0f, 0f, color)
        artist.setShadowLayer(titleSp * 0.4f, 0f, 0f, color)
    }

    private val pulseTick = object : Runnable {
        override fun run() {
            if (visibility != VISIBLE) {
                coverFrame.scaleX = 1f
                coverFrame.scaleY = 1f
                return
            }
            val level = levelProvider?.invoke() ?: 0f
            smoothed = smoothed * 0.75f + level * 0.25f
            val s = if (pulse) 1f + smoothed * 0.14f else 1f
            coverFrame.scaleX = s
            coverFrame.scaleY = s
            postOnAnimation(this)
        }
    }

    private fun dp(v: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

    companion object {
        private val GLOWS = intArrayOf(0xFFB04CFF.toInt(), 0xFF3FD0FF.toInt(), 0xFFFF4FB8.toInt(), 0xFF7C5CFF.toInt())
    }
}
