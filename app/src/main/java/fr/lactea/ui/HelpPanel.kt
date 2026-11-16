package fr.lactea.ui

import android.content.Context
import android.view.KeyEvent
import android.widget.TextView
import fr.lactea.input.Action
import fr.lactea.input.KeyBindings

/** Shows the current key mapping (like MilkDrop's F1 screen). Any key closes it. */
class HelpPanel(context: Context, bindings: KeyBindings, private val onClose: () -> Unit) : ListPanel(context, 18) {

    private val lines: List<Pair<String, String>> =
        Action.entries.map { a ->
            a.label to bindings.triggersFor(a).joinToString(", ") { it.label }.ifEmpty { "—" }
        } + listOf("Retour" to "fermer un panneau / quitter")

    init {
        titleView.text = "Lactea — touches de la télécommande"
        subtitleView.text = "Modifiables dans Options › Télécommande"
        footerView.text = "Appuyez sur Retour pour fermer"
        setCount(lines.size)
    }

    override fun bindRow(index: Int, label: TextView, value: TextView, isSelected: Boolean) {
        label.text = lines[index].first
        value.text = lines[index].second
    }

    override fun onKey(e: KeyEvent): Boolean {
        if (e.keyCode == KeyEvent.KEYCODE_BACK || e.keyCode == KeyEvent.KEYCODE_DPAD_CENTER ||
            e.keyCode == KeyEvent.KEYCODE_ENTER || e.keyCode == KeyEvent.KEYCODE_INFO
        ) {
            if (e.action == KeyEvent.ACTION_UP) onClose()
        }
        return true
    }
}
