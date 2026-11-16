package fr.lactea.nowplaying

import android.service.notification.NotificationListenerService

/**
 * Notification listener: lets Lactea read the active media sessions (title, artist, state)
 * of any app — Spotify, Deezer, YouTube Music… The system keeps it running in the
 * background, so it also hosts [AutoLaunch].
 *
 * Google TV has no settings screen for this permission; grant it once from a PC:
 *   adb shell cmd notification allow_listener fr.lactea/fr.lactea.nowplaying.NowPlayingListener 0
 */
class NowPlayingListener : NotificationListenerService() {
    private var autoLaunch: AutoLaunch? = null

    override fun onListenerConnected() {
        autoLaunch = AutoLaunch(this).also { it.start() }
    }

    override fun onListenerDisconnected() {
        autoLaunch?.stop()
        autoLaunch = null
    }
}
