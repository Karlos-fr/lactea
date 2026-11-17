package fr.lactea.i18n

import android.content.Context
import android.content.res.Configuration
import android.content.res.Resources
import android.os.LocaleList
import java.util.Locale

/**
 * Interface language chosen in the options. "" follows the system language (Android then picks
 * res/values-<lang>, or English). Applied by wrapping the base context of the application, the
 * activity and the screensaver, so it works on every Android version Lactea supports.
 */
object Lang {
    /** Languages with a translation, as language tags, in menu order. */
    val SUPPORTED = listOf("en", "fr", "de", "it", "es", "sv")

    /** Each language's own name, the same in every language. */
    fun nativeName(tag: String): String = when (tag) {
        "en" -> "English"
        "fr" -> "Français"
        "de" -> "Deutsch"
        "it" -> "Italiano"
        "es" -> "Español"
        "sv" -> "Svenska"
        else -> tag
    }

    private fun chosen(context: Context): String =
        context.getSharedPreferences("lactea", Context.MODE_PRIVATE).getString("language", "") ?: ""

    /** [base] with the chosen language applied (the system's when none is chosen). */
    fun wrap(base: Context): Context {
        val tag = chosen(base)
        val locales = if (tag.isEmpty()) Resources.getSystem().configuration.locales
            else LocaleList(Locale.forLanguageTag(tag))
        if (base.resources.configuration.locales == locales) return base
        Locale.setDefault(locales[0])
        val config = Configuration(base.resources.configuration)
        config.setLocales(locales)
        return base.createConfigurationContext(config)
    }
}
