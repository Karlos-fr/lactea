package fr.lactea.input

import android.os.SystemClock
import android.view.KeyEvent

/**
 * Turns raw key events into [Action]s using [KeyBindings], with short / long press support.
 * A key that has a long binding fires its short action on release; otherwise immediately.
 */
class RemoteInput(private val bindings: KeyBindings, private val onAction: (Action) -> Unit) {
    private var trackedKey = -1
    private var trackedDown = 0L
    private var longFired = false

    /** @return true if the event was consumed. */
    fun handle(e: KeyEvent): Boolean {
        val code = e.keyCode
        if (!bindings.isBound(code)) return false
        when (e.action) {
            KeyEvent.ACTION_DOWN -> {
                if (e.repeatCount == 0) {
                    if (bindings.hasLongBinding(code)) {
                        trackedKey = code
                        trackedDown = SystemClock.uptimeMillis()
                        longFired = false
                    } else {
                        fire(Trigger(code, false))
                    }
                } else if (code == trackedKey && !longFired &&
                    (e.isLongPress || SystemClock.uptimeMillis() - trackedDown >= LONG_PRESS_MS)
                ) {
                    longFired = true
                    fire(Trigger(code, true))
                } else if (code != trackedKey && isRepeatable(code)) {
                    fire(Trigger(code, false))
                }
            }
            KeyEvent.ACTION_UP -> {
                if (code == trackedKey) {
                    if (!longFired) fire(Trigger(code, false))
                    trackedKey = -1
                }
            }
        }
        return true
    }

    private fun isRepeatable(code: Int): Boolean {
        val a = bindings.actionFor(Trigger(code, false))
        return a == Action.SENSITIVITY_UP || a == Action.SENSITIVITY_DOWN
    }

    private fun fire(t: Trigger) {
        bindings.actionFor(t)?.let(onAction)
    }

    companion object {
        const val LONG_PRESS_MS = 500L
    }
}
