package fr.lactea.nowplaying

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.util.Log
import fr.lactea.VisualizerActivity
import fr.lactea.settings.Settings

/**
 * Opens Lactea by itself once music has been playing for a while (plan B when the TV's
 * screensaver does not start during playback). Runs inside [NowPlayingListener], which the
 * system keeps alive in the background.
 *
 * Starting an activity from the background needs the "display over other apps" permission:
 *   adb shell appops set fr.lactea SYSTEM_ALERT_WINDOW allow
 */
class AutoLaunch(private val context: Context) : NowPlaying.Listener {

    private val settings = Settings(context)
    private val handler = Handler(Looper.getMainLooper())
    private val nowPlaying = NowPlaying(context, this)

    fun start() = nowPlaying.start()

    fun stop() {
        handler.removeCallbacks(launch)
        nowPlaying.stop()
    }

    override fun onTrackChanged(track: NowPlaying.Track) = Unit

    override fun onPlaybackChanged(playing: Boolean) {
        handler.removeCallbacks(launch)
        if (!playing) {
            dismissedWhilePlaying = false // next listening session may launch again
            return
        }
        val delay = settings.autoLaunchDelay
        if (delay > 0) handler.postDelayed(launch, delay * 1000L)
    }

    private val launch = Runnable {
        val pkg = nowPlaying.sourcePackage
        val reason = when {
            !nowPlaying.playing -> "music stopped"
            visualizerVisible -> "already visible"
            dismissedWhilePlaying -> "user left Lactea during this listening session"
            settings.autoLaunchMusicAppsOnly && pkg !in MUSIC_APPS -> "not a music app ($pkg)"
            !context.getSystemService(PowerManager::class.java).isInteractive -> "screen off"
            else -> null
        }
        if (reason != null) {
            Log.i(TAG, "Auto-launch skipped: $reason")
            return@Runnable
        }
        Log.i(TAG, "Auto-launch: music playing in $pkg")
        runCatching {
            context.startActivity(
                Intent(context, VisualizerActivity::class.java)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    .putExtra(VisualizerActivity.EXTRA_AUTO_LAUNCHED, true)
            )
        }.onFailure { Log.w(TAG, "Auto-launch failed", it) }
    }

    companion object {
        private const val TAG = "LACTEA"

        /** Set by the activity / dream while Lactea is on screen. */
        @Volatile var visualizerVisible = false

        /** The user left Lactea while music was playing: don't reopen it until the music stops. */
        @Volatile var dismissedWhilePlaying = false

        val MUSIC_APPS = setOf(
            "com.spotify.tv.android", "com.spotify.music",
            "deezer.android.tv", "deezer.android.app",
            "com.google.android.youtube.tvmusic", "com.google.android.apps.youtube.music",
            "com.aspiro.tidal", "com.apple.android.music", "com.amazon.mp3",
            "com.qobuz.music", "com.soundcloud.android", "tv.plex.labs.plexamp",
            "com.pandora.android", "com.napster.android", "fm.last.android",
            "radio.fr", "de.radio.android", "tunein.player", "com.audials",
        )
    }
}
