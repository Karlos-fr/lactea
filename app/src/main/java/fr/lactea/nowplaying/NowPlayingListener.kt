package fr.lactea.nowplaying

import android.service.notification.NotificationListenerService

/**
 * Empty notification listener. Its only purpose is to let Lactea read the active
 * media sessions (title, artist, state) of any app — Spotify, Deezer, YouTube Music…
 *
 * Google TV has no settings screen for this permission; grant it once from a PC:
 *   adb shell cmd notification allow_listener fr.lactea/fr.lactea.nowplaying.NowPlayingListener
 */
class NowPlayingListener : NotificationListenerService()
