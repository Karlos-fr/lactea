package fr.lactea.packs

import android.content.Context
import android.util.Log
import fr.lactea.settings.Settings
import org.json.JSONObject
import java.io.File
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.util.concurrent.Executors
import java.util.zip.ZipInputStream

/**
 * Preset packs published on GitHub (or any HTTP server): a manifest.json lists zip packs,
 * which are downloaded, checked (SHA-256) and extracted into filesDir/packs/<id>/.
 *
 * manifest.json:
 * { "version": 1, "packs": [ { "id": "fractal", "name": "Fractal", "version": "1",
 *   "url": "lactea-pack-fractal.zip", "size": 123, "presets": 1354, "sha256": "…",
 *   "description": "…" } ] }
 * "url" may be relative to the manifest URL.
 */
class PackManager(context: Context, private val settings: Settings) {

    data class Pack(
        val id: String,
        val name: String,
        val version: String,
        val url: String,
        val size: Long,
        val presets: Int,
        val sha256: String,
        val description: String,
    )

    sealed class State {
        data object Idle : State()
        data class Downloading(val packId: String, val done: Long, val total: Long) : State()
        data class Extracting(val packId: String) : State()
        data class Error(val message: String) : State()
    }

    val packsDir = File(context.filesDir, "packs")
    private val tmpDir = File(context.cacheDir, "packs")
    private val executor = Executors.newSingleThreadExecutor()

    @Volatile var catalog: List<Pack> = emptyList()
        private set
    @Volatile var catalogStatus: String = "Catalogue non chargé"
        private set
    @Volatile var state: State = State.Idle
        private set

    fun installedVersion(id: String): String? =
        File(packsDir, "$id/.version").takeIf { it.exists() }?.readText()?.trim()

    fun installedIds(): List<String> =
        packsDir.listFiles()?.filter { File(it, ".version").exists() }?.map { it.name } ?: emptyList()

    /** Directories scanned for presets. */
    fun presetRoots(): List<File> = installedIds().map { File(packsDir, it) }

    /** "textures" folders shipped inside packs, added to projectM's search paths. */
    fun textureDirs(): List<File> = presetRoots().mapNotNull { root ->
        root.walkTopDown().maxDepth(2).firstOrNull { it.isDirectory && it.name.equals("textures", true) }
    }

    fun diskUsageBytes(): Long = packsDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }

    fun refreshCatalog(onDone: () -> Unit) {
        catalogStatus = "Chargement du catalogue…"
        executor.execute {
            try {
                val text = httpGet(settings.manifestUrl).use { it.readBytes() }.toString(Charsets.UTF_8)
                val root = JSONObject(text)
                val arr = root.getJSONArray("packs")
                catalog = (0 until arr.length()).map { i ->
                    val o = arr.getJSONObject(i)
                    Pack(
                        id = o.getString("id"),
                        name = o.getString("name"),
                        version = o.getString("version"),
                        url = URL(URL(settings.manifestUrl), o.getString("url")).toString(),
                        size = o.optLong("size"),
                        presets = o.optInt("presets"),
                        sha256 = o.getString("sha256").lowercase(),
                        description = o.optString("description"),
                    )
                }
                catalogStatus = "${catalog.size} packs disponibles"
            } catch (t: Throwable) {
                Log.w(TAG, "Catalog failed", t)
                catalogStatus = "Catalogue indisponible : ${t.message}"
            }
            onDone()
        }
    }

    fun install(pack: Pack, onDone: (Boolean) -> Unit) {
        executor.execute {
            val ok = try {
                doInstall(pack)
                true
            } catch (t: Throwable) {
                Log.w(TAG, "Install ${pack.id} failed", t)
                state = State.Error("${pack.name} : ${t.message}")
                false
            }
            if (ok) state = State.Idle
            onDone(ok)
        }
    }

    fun remove(id: String, onDone: () -> Unit) {
        executor.execute {
            File(packsDir, id).deleteRecursively()
            onDone()
        }
    }

    private fun doInstall(pack: Pack) {
        require(SAFE_ID.matches(pack.id)) { "identifiant de pack invalide" }
        tmpDir.mkdirs()
        val zip = File(tmpDir, "${pack.id}.zip")
        val digest = MessageDigest.getInstance("SHA-256")
        state = State.Downloading(pack.id, 0, pack.size)
        httpGet(pack.url).use { input ->
            zip.outputStream().use { out ->
                val buf = ByteArray(64 * 1024)
                var done = 0L
                while (true) {
                    val n = input.read(buf)
                    if (n < 0) break
                    out.write(buf, 0, n)
                    digest.update(buf, 0, n)
                    done += n
                    state = State.Downloading(pack.id, done, pack.size)
                }
            }
        }
        val hash = digest.digest().joinToString("") { "%02x".format(it) }
        if (hash != pack.sha256) {
            zip.delete()
            throw IOException("empreinte SHA-256 incorrecte")
        }

        state = State.Extracting(pack.id)
        val staging = File(packsDir, "${pack.id}.new")
        staging.deleteRecursively()
        staging.mkdirs()
        val stagingPath = staging.canonicalPath + File.separator
        ZipInputStream(zip.inputStream().buffered()).use { zin ->
            while (true) {
                val e = zin.nextEntry ?: break
                val out = File(staging, e.name)
                if (!out.canonicalPath.startsWith(stagingPath)) throw IOException("chemin interdit dans le zip")
                if (e.isDirectory) out.mkdirs() else {
                    out.parentFile?.mkdirs()
                    out.outputStream().use { zin.copyTo(it) }
                }
            }
        }
        File(staging, ".version").writeText(pack.version)
        val target = File(packsDir, pack.id)
        target.deleteRecursively()
        if (!staging.renameTo(target)) throw IOException("installation impossible")
        zip.delete()
    }

    private fun httpGet(url: String): java.io.InputStream {
        var current = URL(url)
        repeat(5) { // follow redirects, including GitHub's release → CDN hop
            val c = current.openConnection() as HttpURLConnection
            c.connectTimeout = 15_000
            c.readTimeout = 30_000
            c.instanceFollowRedirects = false
            c.setRequestProperty("User-Agent", "Lactea")
            when (val code = c.responseCode) {
                in 200..299 -> return c.inputStream
                in 300..399 -> {
                    current = URL(current, c.getHeaderField("Location"))
                    c.disconnect()
                }
                else -> throw IOException("HTTP $code")
            }
        }
        throw IOException("trop de redirections")
    }

    companion object {
        private const val TAG = "LACTEA"
        private val SAFE_ID = Regex("[a-z0-9_-]{1,40}")
    }
}
