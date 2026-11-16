package fr.lactea.presets

import android.content.Context
import android.util.Log
import java.io.File

/**
 * Copies the bundled presets and textures from assets to internal storage
 * (projectM needs real file paths) and exposes the preset list.
 */
class PresetLibrary(private val context: Context) {
    val presetDir = File(context.filesDir, "presets")
    val textureDir = File(context.filesDir, "textures")

    fun prepare(): List<File> {
        val stamp = File(context.filesDir, ".assets-version")
        val version = context.packageManager.getPackageInfo(context.packageName, 0).lastUpdateTime.toString()
        if (!stamp.exists() || stamp.readText() != version) {
            val start = System.currentTimeMillis()
            presetDir.deleteRecursively()
            textureDir.deleteRecursively()
            copyAssetDir("presets", presetDir)
            copyAssetDir("textures", textureDir)
            stamp.writeText(version)
            Log.i(TAG, "Assets copied in ${System.currentTimeMillis() - start} ms")
        }
        return presetDir.walkTopDown()
            .filter { it.isFile && it.extension.equals("milk", ignoreCase = true) }
            .sortedBy { it.path }
            .toList()
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

        /** "Fractal/Rovastar - VooV_s Light Pattern" style display name. */
        fun displayName(root: File, preset: File): String =
            preset.relativeTo(root).path.removeSuffix(".milk")
    }
}
