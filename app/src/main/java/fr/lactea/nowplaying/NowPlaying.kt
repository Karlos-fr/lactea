package fr.lactea.nowplaying

import android.content.ComponentName
import android.content.Context
import android.media.MediaMetadata
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log

/**
 * Follows the active media session (whatever app is playing) to get the current track and
 * playback state, and to control playback. Requires [NowPlayingListener] to be enabled.
 */
class NowPlaying(private val context: Context, private val listener: Listener) {

    data class Track(val title: String, val artist: String, val album: String, val source: String) {
        val key: String get() = "$source|$title|$artist"
    }

    interface Listener {
        fun onTrackChanged(track: Track)
        fun onPlaybackChanged(playing: Boolean)
        /** Cover provided by the source app for [track] (bitmap and/or URI), possibly none. */
        fun onArtwork(track: Track, bitmap: android.graphics.Bitmap?, uri: String?) = Unit
    }

    private val handler = Handler(Looper.getMainLooper())
    private val manager = context.getSystemService(MediaSessionManager::class.java)
    private val component = ComponentName(context, NowPlayingListener::class.java)
    private var controller: MediaController? = null
    private var started = false

    var track: Track? = null
        private set
    var playing = false
        private set
    var permissionGranted = false
        private set

    /** Package of the app owning the followed session (e.g. com.spotify.tv.android). */
    val sourcePackage: String? get() = controller?.packageName

    fun hasPermission(): Boolean {
        val enabled = Settings.Secure.getString(context.contentResolver, "enabled_notification_listeners") ?: return false
        return enabled.split(':').mapNotNull { ComponentName.unflattenFromString(it) }.any { it == component }
    }

    fun start() {
        if (started) return
        permissionGranted = hasPermission()
        if (!permissionGranted) {
            Log.w(TAG, "Notification listener not enabled: no track titles")
            return
        }
        try {
            manager.addOnActiveSessionsChangedListener(sessionsListener, component, handler)
            pick(manager.getActiveSessions(component))
            started = true
        } catch (e: SecurityException) {
            permissionGranted = false
            Log.w(TAG, "No access to media sessions", e)
        }
    }

    fun stop() {
        if (!started) return
        manager.removeOnActiveSessionsChangedListener(sessionsListener)
        controller?.unregisterCallback(callback)
        controller = null
        started = false
    }

    fun playPause() {
        val tc = controller?.transportControls ?: return
        if (playing) tc.pause() else tc.play()
    }

    fun nextTrack() {
        controller?.transportControls?.skipToNext()
    }

    fun previousTrack() {
        controller?.transportControls?.skipToPrevious()
    }

    private val sessionsListener = MediaSessionManager.OnActiveSessionsChangedListener { list ->
        pick(list ?: emptyList())
    }

    /** Prefers a session that is playing; otherwise keeps the first one. */
    private fun pick(sessions: List<MediaController>) {
        val chosen = sessions.firstOrNull { it.playbackState?.state == PlaybackState.STATE_PLAYING }
            ?: sessions.firstOrNull()
        if (chosen?.sessionToken == controller?.sessionToken) return
        controller?.unregisterCallback(callback)
        controller = chosen
        chosen?.registerCallback(callback, handler)
        Log.i(TAG, "Media session: ${chosen?.packageName ?: "none"}")
        updatePlayback(chosen?.playbackState)
        updateTrack(chosen?.metadata)
    }

    private val callback = object : MediaController.Callback() {
        override fun onMetadataChanged(metadata: MediaMetadata?) = updateTrack(metadata)
        override fun onPlaybackStateChanged(state: PlaybackState?) = updatePlayback(state)
        override fun onSessionDestroyed() {
            controller?.unregisterCallback(this)
            controller = null
            runCatching { pick(manager.getActiveSessions(component)) }
        }
    }

    private fun updatePlayback(state: PlaybackState?) {
        val now = state?.state == PlaybackState.STATE_PLAYING
        if (now != playing) {
            playing = now
            listener.onPlaybackChanged(now)
        }
    }

    private fun updateTrack(md: MediaMetadata?) {
        md ?: return
        val title = md.getString(MediaMetadata.METADATA_KEY_TITLE)
            ?: md.getString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE) ?: return
        if (title.isBlank()) return
        val artist = md.getString(MediaMetadata.METADATA_KEY_ARTIST)
            ?: md.getString(MediaMetadata.METADATA_KEY_ALBUM_ARTIST)
            ?: md.getString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE) ?: ""
        val album = md.getString(MediaMetadata.METADATA_KEY_ALBUM) ?: ""
        val t = Track(title.trim(), artist.trim(), album.trim(), sourceLabel(controller?.packageName))
        if (t.key != track?.key) {
            track = t
            Log.i(TAG, "TRACK \"${t.title}\" — ${t.artist} (${t.source})")
            listener.onTrackChanged(t)
        }
        // The cover provided by the app (often sent a moment after the title).
        val art = ART_KEYS.firstNotNullOfOrNull { md.getBitmap(it) }
        val artUri = ART_URI_KEYS.firstNotNullOfOrNull { md.getString(it) }?.takeIf { it.isNotBlank() }
        val signature = "${t.key}|$artUri|${art != null}"
        if (signature != artSignature) {
            artSignature = signature
            listener.onArtwork(t, art, artUri)
        }
    }

    private var artSignature: String? = null

    private fun sourceLabel(pkg: String?): String {
        pkg ?: return "?"
        return runCatching {
            context.packageManager.getApplicationLabel(context.packageManager.getApplicationInfo(pkg, 0)).toString()
        }.getOrDefault(pkg)
    }

    companion object {
        private const val TAG = "LACTEA"
        private val ART_KEYS = listOf(
            MediaMetadata.METADATA_KEY_ALBUM_ART, MediaMetadata.METADATA_KEY_ART, MediaMetadata.METADATA_KEY_DISPLAY_ICON,
        )
        private val ART_URI_KEYS = listOf(
            MediaMetadata.METADATA_KEY_ALBUM_ART_URI, MediaMetadata.METADATA_KEY_ART_URI, MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI,
        )
        const val ADB_GRANT =
            "adb shell cmd notification allow_listener fr.lactea/fr.lactea.nowplaying.NowPlayingListener 0"
    }
}
