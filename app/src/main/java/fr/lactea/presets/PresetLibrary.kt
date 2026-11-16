package fr.lactea.presets

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Bundled presets/textures (copied from assets, projectM needs real files) plus
 * any downloaded packs. Produces the [Playlist.Entry] list and texture search paths.
 */
class PresetLibrary(private val context: Context) {
    val bundledPresetDir = File(context.filesDir, "presets")
    val bundledTextureDir = File(context.filesDir, "textures")

    /** Copies assets on first launch / after an update. */
    fun prepareBundled() {
        val stamp = File(context.filesDir, ".assets-version")
        val version = context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime.toString()
        if (stamp.exists() && stamp.readText() == version) return
        val start = System.currentTimeMillis()
        bundledPresetDir.deleteRecursively()
        bundledTextureDir.deleteRecursively()
        copyAssetDir("presets", bundledPresetDir)
        copyAssetDir("textures", bundledTextureDir)
        stamp.writeText(version)
        Log.i(TAG, "Assets copied in ${System.currentTimeMillis() - start} ms")
    }

    /** Scans the bundled presets and every pack root. Duplicate keys keep the first one. */
    fun scan(packRoots: List<File>): List<Playlist.Entry> {
        val seen = HashSet<String>()
        val out = ArrayList<Playlist.Entry>()
        for (root in listOf(bundledPresetDir) + packRoots) {
            root.walkTopDown()
                .filter { it.isFile && it.extension.equals("milk", ignoreCase = true) }
                .forEach { f ->
                    val key = f.relativeTo(root).invariantSeparatorsPath.removeSuffix(".milk")
                    if (seen.add(key)) out.add(Playlist.Entry(key, f))
                }
        }
        out.sortBy { it.key.lowercase() }
        return out
    }

    private fun copyAssetDir(assetPath: String, target: File) {
        val children = context.assets.list(assetPath) ?: return
        if (children.isEmpty()) {
            target.parentFile?.mkdirs()
            context.assets.open(assetPath).use { input -> target.outputStream().use { input.copyTo(it) } }
            return
        }
        target.mkdirs()
        for (child in children) copyAssetDir("$assetPath/$child", File(target, child))
    }

    companion object {
        private const val TAG = "LACTEA"
    }
}
