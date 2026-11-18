package fr.lactea.packs

import fr.lactea.R
import android.content.Context
import android.util.Log
import fr.lactea.presets.PackIndex
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
 * Packs published on GitHub (or any HTTP server): a manifest.json lists zip packs, which are
 * downloaded, checked (SHA-256) and extracted into filesDir/packs/<id>/. Each pack holds an
 * index.json describing its content (see [PackIndex]); it is written at install time for a pack
 * that has none.
 *
 * manifest.json (built by tools/make_packs.py):
 * { "version": 2, "packs": [ { "id": "fractal", "name": "Fractal", "kind": "presets",
 *   "version": "2", "url": "lactea-pack-fractal.zip", "size": 123, "presets": 1354,
 *   "sha256": "…", "description": "…", "recommended": false, "requires": [ "textures" ] } ] }
 * "url" may be relative to the manifest URL. "kind" is "presets" or "textures"; a pack is
 * installed after the packs it requires.
 */
class PackManager(private val context: Context, private val settings: Settings) {

    data class Pack(
        val id: String,
        val name: String,
        val version: String,
        val url: String,
        val size: Long,
        val presets: Int,
        val sha256: String,
        val description: String,
        val isTextures: Boolean,
        val recommended: Boolean,
        val requires: List<String>,
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
    @Volatile var catalogStatus: String = context.getString(R.string.catalog_not_loaded)
        private set
    @Volatile var state: State = State.Idle
        private set

    fun installedVersion(id: String): String? =
        File(packsDir, "$id/.version").takeIf { it.exists() }?.readText()?.trim()

    fun installedIds(): List<String> =
        packsDir.listFiles()?.filter { SAFE_ID.matches(it.name) && File(it, ".version").exists() }?.map { it.name }
            ?: emptyList()

    /** Folders of the installed packs. */
    fun installedDirs(): List<File> = installedIds().sorted().map { File(packsDir, it) }

    /** Space used by the packs, computed in the background after each change. */
    @Volatile var diskUsageBytes: Long = 0
        private set

    fun refreshDiskUsage() = executor.execute { updateDiskUsage() }

    private fun updateDiskUsage() {
        diskUsageBytes = packsDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    /** True when [pack] is installed in its catalogue version. */
    fun isUpToDate(pack: Pack): Boolean = installedVersion(pack.id) == pack.version

    /** Packs required by installed packs but not installed (e.g. textures, after an upgrade from 1.0). */
    fun missingRequirements(): List<Pack> {
        val installed = installedIds().toSet()
        val byId = catalog.associateBy { it.id }
        return catalog.filter { it.id in installed }.flatMap { it.requires }.distinct()
            .filter { it !in installed }.mapNotNull { byId[it] }
    }

    /**
     * [packs] plus the packs they require that are missing or outdated, required ones first,
     * without the packs already up to date.
     */
    fun withRequirements(packs: List<Pack>): List<Pack> {
        val byId = catalog.associateBy { it.id }
        val out = LinkedHashSet<Pack>()
        fun add(p: Pack) {
            p.requires.mapNotNull { byId[it] }.forEach(::add)
            if (!isUpToDate(p)) out += p
        }
        packs.forEach(::add)
        return out.toList()
    }

    /** Installed packs whose catalogue version differs from the installed one. */
    fun updatesAvailable(): List<Pack> = catalog.filter { p -> installedVersion(p.id)?.let { it != p.version } ?: false }

    fun refreshCatalog(onDone: () -> Unit) {
        catalogStatus = context.getString(R.string.catalog_loading)
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
                        isTextures = o.optString("kind") == "textures",
                        recommended = o.optBoolean("recommended"),
                        requires = o.optJSONArray("requires")?.let { a -> (0 until a.length()).map { a.getString(it) } }
                            ?: emptyList(),
                    )
                }
                catalogStatus = context.getString(R.string.catalog_count, catalog.size)
            } catch (t: Throwable) {
                Log.w(TAG, "Catalog failed", t)
                catalogStatus = context.getString(R.string.catalog_error, t.message ?: "?")
            }
            onDone()
        }
    }

    /**
     * Installs [packs] one after the other, with the packs they require (see [withRequirements]).
     * Stops at the first failure. [onDone] receives the packs installed.
     */
    fun install(packs: List<Pack>, onDone: (installed: List<Pack>, ok: Boolean) -> Unit) {
        val todo = withRequirements(packs)
        executor.execute {
            val installed = ArrayList<Pack>()
            var ok = true
            for (pack in todo) {
                try {
                    doInstall(pack)
                    installed += pack
                } catch (t: Throwable) {
                    Log.w(TAG, "Install ${pack.id} failed", t)
                    state = State.Error(context.getString(R.string.pack_error, pack.name, t.message ?: "?"))
                    ok = false
                    break
                }
            }
            if (ok) state = State.Idle
            updateDiskUsage()
            onDone(installed, ok)
        }
    }

    fun remove(id: String, onDone: () -> Unit) {
        executor.execute {
            File(packsDir, id).deleteRecursively()
            updateDiskUsage()
            onDone()
        }
    }

    private fun doInstall(pack: Pack) {
        require(SAFE_ID.matches(pack.id)) { context.getString(R.string.err_bad_id) }
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
            throw IOException(context.getString(R.string.err_sha))
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
                if (!out.canonicalPath.startsWith(stagingPath)) throw IOException(context.getString(R.string.err_path))
                if (e.isDirectory) out.mkdirs() else {
                    out.parentFile?.mkdirs()
                    out.outputStream().use { zin.copyTo(it) }
                }
            }
        }
        PackIndex.read(staging) // a pack without index.json is analysed now, once
        File(staging, ".version").writeText(pack.version)
        val target = File(packsDir, pack.id)
        target.deleteRecursively()
        if (!staging.renameTo(target)) throw IOException(context.getString(R.string.err_install))
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
        throw IOException(context.getString(R.string.err_redirects))
    }

    companion object {
        private const val TAG = "LACTEA"
        private val SAFE_ID = Regex("[a-z0-9_-]{1,40}")
    }
}
