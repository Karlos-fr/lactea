package fr.lactea

import android.app.Application
import fr.lactea.i18n.L

/** Initializes the localized texts once, for the activity, the screensaver and the services. */
class LacteaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        L.init(this)
    }
}
