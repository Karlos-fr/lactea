package fr.lactea.i18n

import android.content.Context
import android.content.res.Resources

/**
 * Localized texts for code without a Context at hand (actions, key names, audio status…).
 * Android picks res/values-<lang> from the system language; res/values (English) otherwise.
 */
object L {
    private lateinit var res: Resources

    fun init(context: Context) {
        res = context.applicationContext.resources
    }

    fun s(id: Int): String = res.getString(id)

    fun s(id: Int, vararg args: Any): String = res.getString(id, *args)
}
