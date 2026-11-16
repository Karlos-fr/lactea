package fr.lactea.cover

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import fr.lactea.nowplaying.NowPlaying
import fr.lactea.settings.Settings
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

/**
 * Finds the album cover of the current track and stores it as a texture file for projectM.
 *
 * Priority: the cover provided by the source app (its URL — in high resolution when it is a
 * Spotify CDN link — then its bitmap), and only if the app provides nothing, an online lookup
 * (Deezer, then iTunes) when enabled in the settings.
 */
class CoverManager(private val context: Context, private val settings: Settings) {

    data class Cover(
        val trackKey: String,
        val bitmap: Bitmap,
        val file: File,
        val source: String,
        /** Vivid colour taken from the image, for the glow. */
        val color: Int,
    ) {
        /** Texture name projectM resolves to [file] (file name without extension). */
        val textureName: String get() = file.nameWithoutExtension
    }

    val dir = File(context.filesDir, "cover").apply { mkdirs() }
    private val executor = Executors.newSingleThreadExecutor()
    private val main = Handler(Looper.getMainLooper())
    private val generation = AtomicInteger()

    @Volatile var current: Cover? = null
        private set

    /** Looks for the cover of [track] in the background; [onDone] runs on the main thread. */
    fun request(track: NowPlaying.Track, appBitmap: Bitmap?, appUri: String?, onDone: (Cover?) -> Unit) {
        val gen = generation.incrementAndGet()
        executor.execute {
            val found = fromApp(track, appBitmap, appUri)
                ?: if (settings.coverOnline) online(track) else null
            if (gen != generation.get()) return@execute // a newer track arrived meanwhile
            val cover = found?.let { (bmp, source) -> store(track, bmp, source, appUri) }
            current = cover
            Log.i(TAG, "COVER ${cover?.let { "${it.source} → ${it.file.name}" } ?: "none"} for \"${track.title}\"")
            main.post { onDone(cover) }
        }
    }

    // ------------------------------------------------------------------ Sources

    private fun fromApp(track: NowPlaying.Track, bitmap: Bitmap?, uri: String?): Pair<Bitmap, String>? {
        if (uri != null) {
            val candidates = if (uri.contains(SPOTIFY_300)) listOf(uri.replace(SPOTIFY_300, SPOTIFY_640), uri) else listOf(uri)
            for (u in candidates) {
                loadUri(u)?.let { return it to track.source }
            }
        }
        bitmap?.takeIf { !it.isRecycled }?.let { return it.copy(Bitmap.Config.ARGB_8888, false) to track.source }
        return null
    }

    private fun loadUri(u: String): Bitmap? = runCatching {
        when {
            u.startsWith("http://") || u.startsWith("https://") -> download(u)
            else -> context.contentResolver.openInputStream(Uri.parse(u))?.use { BitmapFactory.decodeStream(it) }
        }
    }.getOrNull()

    private fun online(track: NowPlaying.Track): Pair<Bitmap, String>? {
        if (track.artist.isBlank()) return null
        deezer(track)?.let { url -> download(url)?.let { return it to "Deezer" } }
        itunes(track)?.let { url -> download(url)?.let { return it to "iTunes" } }
        return null
    }

    private fun deezer(t: NowPlaying.Track): String? = runCatching {
        val q = enc("artist:\"${t.artist}\" track:\"${t.title}\"")
        val data = JSONObject(httpText("https://api.deezer.com/search?q=$q&limit=1")).optJSONArray("data")
        data?.optJSONObject(0)?.optJSONObject("album")?.optString("cover_xl")?.takeIf { it.isNotBlank() }
    }.getOrNull()

    private fun itunes(t: NowPlaying.Track): String? = runCatching {
        val q = enc("${t.artist} ${t.title}")
        val results = JSONObject(httpText("https://itunes.apple.com/search?term=$q&entity=song&limit=1")).optJSONArray("results")
        results?.optJSONObject(0)?.optString("artworkUrl100")?.takeIf { it.isNotBlank() }
            ?.replace("100x100bb", "600x600bb")
    }.getOrNull()

    // ------------------------------------------------------------------ Storage

    private fun store(track: NowPlaying.Track, source: Bitmap, sourceName: String, appUri: String?): Cover? = runCatching {
        val bmp = scaleDown(source, 1024)
        val id = (if (track.album.isNotBlank()) "${track.artist}|${track.album}" else appUri ?: track.key)
        val file = File(dir, "cover_${md5(id).take(12)}.jpg")
        if (!file.exists()) {
            file.outputStream().use { bmp.compress(Bitmap.CompressFormat.JPEG, 92, it) }
            prune()
        }
        Cover(track.key, bmp, file, "$sourceName ${bmp.width}×${bmp.height}", vividColor(bmp))
    }.onFailure { Log.w(TAG, "Cover store failed", it) }.getOrNull()

    private fun prune() {
        dir.listFiles()?.sortedByDescending { it.lastModified() }?.drop(KEEP)?.forEach { it.delete() }
    }

    // ------------------------------------------------------------------ Helpers

    private fun download(url: String): Bitmap? = runCatching {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 4000
        c.readTimeout = 6000
        c.setRequestProperty("User-Agent", "Lactea")
        try {
            if (c.responseCode !in 200..299) return null
            c.inputStream.use { BitmapFactory.decodeStream(it) }
        } finally {
            c.disconnect()
        }
    }.getOrNull()

    private fun httpText(url: String): String {
        val c = URL(url).openConnection() as HttpURLConnection
        c.connectTimeout = 4000
        c.readTimeout = 6000
        c.setRequestProperty("User-Agent", "Lactea")
        try {
            return c.inputStream.use { it.readBytes().toString(Charsets.UTF_8) }
        } finally {
            c.disconnect()
        }
    }

    private fun enc(s: String) = URLEncoder.encode(s, "UTF-8")

    private fun md5(s: String) = MessageDigest.getInstance("MD5").digest(s.toByteArray())
        .joinToString("") { "%02x".format(it) }

    private fun scaleDown(b: Bitmap, max: Int): Bitmap {
        val m = maxOf(b.width, b.height)
        if (m <= max) return b
        val f = max.toFloat() / m
        return Bitmap.createScaledBitmap(b, (b.width * f).toInt(), (b.height * f).toInt(), true)
    }

    /** Most vivid region of the image, brightened, for the glow. */
    private fun vividColor(b: Bitmap): Int {
        val small = Bitmap.createScaledBitmap(b, 8, 8, true)
        var best = Color.rgb(124, 92, 255)
        var bestScore = -1f
        val hsv = FloatArray(3)
        for (y in 0 until 8) for (x in 0 until 8) {
            Color.colorToHSV(small.getPixel(x, y), hsv)
            val score = hsv[1] * hsv[2]
            if (score > bestScore) {
                bestScore = score
                best = Color.HSVToColor(floatArrayOf(hsv[0], maxOf(hsv[1], 0.55f), 1f))
            }
        }
        return best
    }

    companion object {
        private const val TAG = "LACTEA"
        private const val KEEP = 20
        private const val SPOTIFY_300 = "ab67616d00001e02"
        private const val SPOTIFY_640 = "ab67616d0000b273"
    }
}
