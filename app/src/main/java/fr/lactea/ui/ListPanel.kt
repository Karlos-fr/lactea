package fr.lactea.ui

import android.content.Context
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.text.TextUtils
import android.util.TypedValue
import android.view.Gravity
import android.view.KeyEvent
import android.widget.LinearLayout
import android.widget.TextView

/**
 * D-pad driven list overlay drawn with a fixed number of row views (windowed), so it
 * stays fast with thousands of entries. Subclasses fill rows and handle extra keys.
 */
abstract class ListPanel(context: Context, maxRows: Int) : LinearLayout(context) {

    /** As many rows as fit on screen (title, subtitle, footer and margins take ~230 dp). */
    private val visibleRows: Int = run {
        val m = context.resources.displayMetrics
        val heightDp = m.heightPixels / m.density
        minOf(maxRows, ((heightDp - 230f) / 34f).toInt().coerceAtLeast(5))
    }

    protected val titleView = text(22f, Color.WHITE, bold = true)
    protected val subtitleView = text(14f, 0xFFB8C0FF.toInt())
    private val rowViews = List(visibleRows) { Row(context) }
    protected val footerView = text(13f, 0xFF9AA0C8.toInt())

    private val body = LinearLayout(context)
    private val rows = LinearLayout(context)

    /** A view shown on the right of the rows (e.g. a preview), [widthDp] wide. */
    protected fun setSideView(v: android.view.View, widthDp: Int) {
        body.addView(v, LayoutParams(dp(widthDp), LayoutParams.WRAP_CONTENT).apply { leftMargin = dp(20); topMargin = dp(4) })
    }

    private val backgroundDrawable = GradientDrawable().apply {
        setColor(OPAQUE)
        cornerRadius = dp(18).toFloat()
        setStroke(dp(1), 0x667C5CFF)
    }

    /** See-through background, to watch what plays behind (live preview). */
    protected fun setSeeThrough(on: Boolean) {
        backgroundDrawable.setColor(if (on) SEE_THROUGH else OPAQUE)
    }

    var count = 0
        private set
    var selected = 0
        protected set
    private var top = 0

    init {
        orientation = VERTICAL
        setPadding(dp(28), dp(22), dp(28), dp(18))
        background = backgroundDrawable
        addView(titleView)
        addView(subtitleView)
        body.orientation = HORIZONTAL
        rows.orientation = VERTICAL
        rowViews.forEach { rows.addView(it, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT)) }
        body.addView(rows, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
        addView(body, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT))
        addView(footerView)
        (footerView.layoutParams as LayoutParams).topMargin = dp(10)
    }

    /** Fill [label] / [value] for row [index]. */
    protected abstract fun bindRow(index: Int, label: TextView, value: TextView, isSelected: Boolean)

    /** Return false for rows that cannot be selected (headers). */
    protected open fun isSelectable(index: Int) = true

    /** Extra keys (OK, left/right, colours…). Return true if consumed. */
    protected abstract fun onKey(e: KeyEvent): Boolean

    protected fun setCount(n: Int, keepSelection: Boolean = true) {
        count = n
        if (!keepSelection) selected = 0
        selected = selected.coerceIn(0, (n - 1).coerceAtLeast(0))
        if (n > 0 && !isSelectable(selected)) move(+1)
        refresh()
    }

    open fun refresh() {
        if (selected < top) top = selected
        if (selected >= top + visibleRows) top = selected - visibleRows + 1
        top = top.coerceIn(0, (count - visibleRows).coerceAtLeast(0))
        rowViews.forEachIndexed { r, row ->
            val i = top + r
            if (i < count) {
                row.visibility = VISIBLE
                val sel = i == selected
                row.setBackgroundColor(if (sel) 0x557C5CFF else Color.TRANSPARENT)
                row.label.setTextColor(if (sel) Color.WHITE else 0xFFD8DCF5.toInt())
                bindRow(i, row.label, row.value, sel)
            } else {
                row.visibility = INVISIBLE
            }
        }
    }

    /** Called by the activity for every key event while the panel is shown. */
    fun handleKey(e: KeyEvent): Boolean {
        if (e.action == KeyEvent.ACTION_DOWN) {
            when (e.keyCode) {
                KeyEvent.KEYCODE_DPAD_UP -> { move(-1); return true }
                KeyEvent.KEYCODE_DPAD_DOWN -> { move(+1); return true }
                KeyEvent.KEYCODE_PAGE_UP, KeyEvent.KEYCODE_CHANNEL_UP -> { move(-visibleRows); return true }
                KeyEvent.KEYCODE_PAGE_DOWN, KeyEvent.KEYCODE_CHANNEL_DOWN -> { move(+visibleRows); return true }
            }
        }
        return onKey(e)
    }

    protected fun move(delta: Int) {
        if (count == 0) return
        var i = selected
        val step = if (delta > 0) 1 else -1
        var remaining = kotlin.math.abs(delta)
        while (remaining > 0) {
            var j = i + step
            while (j in 0 until count && !isSelectable(j)) j += step
            if (j !in 0 until count) break
            i = j
            remaining--
        }
        selected = i
        refresh()
    }

    protected fun dp(v: Int) = TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics).toInt()

    private fun text(sp: Float, color: Int, bold: Boolean = false) = TextView(context).apply {
        setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
        setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private inner class Row(context: Context) : LinearLayout(context) {
        val label = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setShadowLayer(4f, 0f, 1f, 0xE6000000.toInt()) // readable over a see-through background
            isSingleLine = true
            ellipsize = TextUtils.TruncateAt.END
        }
        val value = TextView(context).apply {
            setTextSize(TypedValue.COMPLEX_UNIT_SP, 16f)
            setTextColor(0xFF8FE3FF.toInt())
            setShadowLayer(4f, 0f, 1f, 0xE6000000.toInt())
            isSingleLine = true
            gravity = Gravity.END
        }

        init {
            orientation = HORIZONTAL
            setPadding(dp(12), dp(5), dp(12), dp(5))
            addView(label, LayoutParams(0, LayoutParams.WRAP_CONTENT, 1f))
            addView(value, LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT).apply { leftMargin = dp(16) })
        }
    }

    private companion object {
        val OPAQUE = 0xE60B0F2E.toInt()
        val SEE_THROUGH = 0x800B0F2E.toInt()
    }
}
