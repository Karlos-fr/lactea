package fr.lactea.ui

import android.content.Context
import android.graphics.Typeface
import android.view.KeyEvent
import android.widget.TextView

sealed class MenuItem(val label: String) {
    class Header(label: String) : MenuItem(label)
    class Choice(label: String, val options: List<String>, val get: () -> Int, val set: (Int) -> Unit) : MenuItem(label)
    class Toggle(label: String, val get: () -> Boolean, val set: (Boolean) -> Unit) : MenuItem(label)
    class Button(label: String, val value: () -> String = { "" }, val onClick: () -> Unit) : MenuItem(label)
    class Info(label: String, val value: () -> String) : MenuItem(label)
}

class MenuScreen(val title: String, val subtitle: String = "", val build: () -> List<MenuItem>)

/**
 * Hierarchical options menu. ▲▼ move, ◀▶ change a value, OK validate / enter,
 * Back goes up one level, then closes.
 */
class MenuPanel(context: Context, root: MenuScreen, private val onClose: () -> Unit) : ListPanel(context, 15) {

    private val stack = ArrayList<Pair<MenuScreen, Int>>()
    private var screen = root
    private var items: List<MenuItem> = emptyList()

    init {
        footerView.text = "▲▼ naviguer   ◀▶ modifier   OK valider   Retour : revenir"
        show(root, 0)
    }

    fun push(s: MenuScreen) {
        stack.add(screen to selected)
        show(s, 0)
    }

    /** Rebuilds the current screen (values may have changed). */
    fun rebuild() {
        items = screen.build()
        setCount(items.size)
    }

    private fun show(s: MenuScreen, sel: Int) {
        screen = s
        titleView.text = s.title
        subtitleView.text = s.subtitle
        subtitleView.visibility = if (s.subtitle.isEmpty()) GONE else VISIBLE
        items = s.build()
        selected = sel
        setCount(items.size)
    }

    override fun isSelectable(index: Int) = items.getOrNull(index) !is MenuItem.Header

    override fun bindRow(index: Int, label: TextView, value: TextView, isSelected: Boolean) {
        val item = items[index]
        label.text = item.label
        label.typeface = if (item is MenuItem.Header) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        if (item is MenuItem.Header) label.setTextColor(0xFFC9A8FF.toInt())
        value.text = when (item) {
            is MenuItem.Header -> ""
            is MenuItem.Choice -> {
                val v = item.options.getOrElse(item.get()) { "?" }
                if (isSelected) "◀  $v  ▶" else v
            }
            is MenuItem.Toggle -> if (item.get()) "Oui" else "Non"
            is MenuItem.Button -> item.value()
            is MenuItem.Info -> item.value()
        }
    }

    override fun onKey(e: KeyEvent): Boolean {
        val item = items.getOrNull(selected)
        if (e.keyCode == KeyEvent.KEYCODE_BACK) {
            if (e.action == KeyEvent.ACTION_UP) back()
            return true
        }
        if (e.action != KeyEvent.ACTION_DOWN) return true
        when (e.keyCode) {
            KeyEvent.KEYCODE_DPAD_LEFT -> change(item, -1)
            KeyEvent.KEYCODE_DPAD_RIGHT -> change(item, +1)
            KeyEvent.KEYCODE_DPAD_CENTER, KeyEvent.KEYCODE_ENTER -> if (e.repeatCount == 0) when (item) {
                is MenuItem.Button -> item.onClick()
                is MenuItem.Toggle, is MenuItem.Choice -> change(item, +1)
                else -> Unit
            }
        }
        // onClick may have pushed another screen; rebuild whatever is current.
        rebuild()
        return true
    }

    private fun change(item: MenuItem?, delta: Int) {
        when (item) {
            is MenuItem.Choice -> {
                val n = item.options.size
                item.set(((item.get() + delta) % n + n) % n)
            }
            is MenuItem.Toggle -> item.set(!item.get())
            else -> Unit
        }
    }

    fun back() {
        if (stack.isEmpty()) {
            onClose()
            return
        }
        val (s, sel) = stack.removeAt(stack.lastIndex)
        show(s, sel)
    }
}
