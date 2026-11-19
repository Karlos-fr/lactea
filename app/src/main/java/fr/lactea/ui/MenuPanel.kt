package fr.lactea.ui

import android.content.Context
import fr.lactea.R
import android.graphics.Typeface
import android.view.KeyEvent
import android.widget.TextView

sealed class MenuItem(val label: String) {
    /** Left out of the web remote: it acts on the TV screen itself (key bindings, help…). */
    var tvOnly = false

    class Header(label: String) : MenuItem(label)
    class Choice(label: String, val options: List<String>, val get: () -> Int, val set: (Int) -> Unit) : MenuItem(label)
    class Toggle(label: String, val get: () -> Boolean, val set: (Boolean) -> Unit) : MenuItem(label)
    class Button(
        label: String,
        val value: () -> String = { "" },
        /** Opens this screen, with no other effect. */
        val opens: (() -> MenuScreen)? = null,
        /** [onClick] asks to be confirmed (OK twice on the TV, a question on the phone). */
        val confirm: Boolean = false,
        val onClick: () -> Unit = {},
    ) : MenuItem(label)
    class Info(label: String, val value: () -> String) : MenuItem(label)
    /** A text: typed with the remote's keyboard (or voice) on the TV, in a field on the phone. */
    class Text(label: String, val dialogTitle: String, val get: () -> String, val set: (String) -> Unit) : MenuItem(label)
    /**
     * Shown on the web remote only, as [item] (a control the TV list cannot show, such as a list of
     * presets to reorder). [onSet] / [onAction] get the ids of its sub-entries ("<item id>/<entry id>").
     */
    class Web(
        /** Id of [item]. */
        val id: String,
        val item: () -> fr.tvremote.core.Item,
        val onSet: ((id: String, value: Any?) -> fr.tvremote.core.Result)? = null,
        val onAction: ((id: String) -> fr.tvremote.core.Result)? = null,
    ) : MenuItem("")
}

fun <T : MenuItem> T.tvOnly(): T = apply { tvOnly = true }

/** A screen of the options; [id] names it for the web remote (see WebRemote). */
class MenuScreen(
    val id: String,
    val title: String,
    val subtitle: String = "",
    /** Runs when the screen is entered (on the TV or on a phone). */
    val onOpen: () -> Unit = {},
    val build: () -> List<MenuItem>,
)

/**
 * Hierarchical options menu. ▲▼ move, ◀▶ change a value, OK validate / enter,
 * Back goes up one level, then closes.
 */
class MenuPanel(
    context: Context,
    root: MenuScreen,
    private val onClose: () -> Unit,
    /** Shows a text field (title, initial text, what to do with the text typed). */
    private val askText: (String, String, (String) -> Unit) -> Unit,
    /** A value changed from the TV's remote (the web remote refreshes). */
    private val onChanged: () -> Unit,
) : ListPanel(context, 15) {

    private val stack = ArrayList<Pair<MenuScreen, Int>>()
    private var screen = root
    private var items: List<MenuItem> = emptyList()

    init {
        footerView.text = context.getString(R.string.menu_footer)
        show(root, 0)
    }

    fun push(s: MenuScreen) {
        s.onOpen()
        stack.add(screen to selected)
        show(s, 0)
    }

    /** Rebuilds the current screen (values may have changed). */
    fun rebuild() {
        items = tvItems(screen)
        setCount(items.size)
    }

    /** Id of the screen shown. */
    val screenId: String get() = screen.id

    private fun tvItems(s: MenuScreen) = s.build().filter { it !is MenuItem.Web }

    private fun show(s: MenuScreen, sel: Int) {
        screen = s
        titleView.text = s.title
        subtitleView.text = s.subtitle
        subtitleView.visibility = if (s.subtitle.isEmpty()) GONE else VISIBLE
        items = tvItems(s)
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
            is MenuItem.Toggle -> context.getString(if (item.get()) R.string.yes else R.string.no)
            is MenuItem.Button -> item.value()
            is MenuItem.Info -> item.value()
            is MenuItem.Text -> item.get()
            is MenuItem.Web -> ""
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
                is MenuItem.Button -> {
                    val opens = item.opens
                    if (opens != null) push(opens()) else { item.onClick(); onChanged() }
                }
                is MenuItem.Text -> askText(item.dialogTitle, item.get()) { item.set(it); rebuild(); onChanged() }
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
                onChanged()
            }
            is MenuItem.Toggle -> { item.set(!item.get()); onChanged() }
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
