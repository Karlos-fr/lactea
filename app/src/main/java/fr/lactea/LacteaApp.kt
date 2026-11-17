package fr.lactea

import android.app.Application
import android.content.Context
import fr.lactea.i18n.L
import fr.lactea.i18n.Lang

/** Initializes the localized texts once, for the activity and the services. */
class LacteaApp : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(Lang.wrap(base))
    }

    override fun onCreate() {
        super.onCreate()
        L.init(this)
    }
}
