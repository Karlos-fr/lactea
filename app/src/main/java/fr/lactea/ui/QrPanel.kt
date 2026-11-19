package fr.lactea.ui

import android.content.Context
import android.graphics.Bitmap
import android.view.KeyEvent
import android.widget.ImageView
import android.widget.TextView
import fr.lactea.R

/** The QR code of the web remote and its address; Back or OK closes it. */
class QrPanel(context: Context, qr: Bitmap, url: String, private val onClose: () -> Unit) : ListPanel(context, 1) {

    init {
        titleView.text = context.getString(R.string.h_web_remote)
        subtitleView.text = url
        footerView.text = context.getString(R.string.web_qr_footer)
        setSideView(ImageView(context).apply {
            setImageBitmap(qr)
            adjustViewBounds = true
        }, QR_DP)
    }

    override fun bindRow(index: Int, label: TextView, value: TextView, isSelected: Boolean) = Unit

    override fun onKey(e: KeyEvent): Boolean {
        val close = e.keyCode == KeyEvent.KEYCODE_BACK || e.keyCode == KeyEvent.KEYCODE_DPAD_CENTER || e.keyCode == KeyEvent.KEYCODE_ENTER
        if (close && e.action == KeyEvent.ACTION_UP) onClose()
        return true
    }

    companion object {
        private const val QR_DP = 260
    }
}
