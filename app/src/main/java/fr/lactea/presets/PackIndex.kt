package fr.lactea.presets

import android.util.JsonReader
import android.util.Log
import fr.lactea.cover.CoverPresets
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

/**
 * The index.json of an installed pack: its presets (with what Lactea needs to know about each
 * one) and its textures. Packs published by Lactea come with it (tools/make_packs.py), so the
 * presets are never scanned nor read on the TV. A pack without one (a pack Lactea does not know)
 * is analysed once, when it is first seen, and its index is written next to it.
 *
 * { "format": 1, "id": "dancer", "version": "2",
 *   "presets": [ { "file": "Dancer/x.milk", "image": true, "fps": 50 } ],
 *   "textures": { "dir": "textures", "names": [ "clouds", … ] },
 *   "reference": { "device": "…", "gpu": "…", "height": 720, "cap": 60, "date": "…" } }
 */
class PackIndex(
    val presets: List<Preset>,
    val textureDir: String?,
    val textureNames: List<String>,
    val reference: Reference?,
) {
    /**
     * @param image the preset samples a random texture: Lactea can show the album cover in it
     * @param fps frame rate measured on the reference TV, or null
     */
    data class Preset(val file: String, val image: Boolean, val fps: Float?)

    data class Reference(val device: String, val height: Int)

    companion object {
        const val FILE = "index.json"
        private const val TAG = "LACTEA"

        /** Reads the index of the pack installed in [dir], writing it first if the pack has none. */
        fun read(dir: File): PackIndex {
            val file = File(dir, FILE)
            if (file.exists()) {
                runCatching { return parse(file) }
                    .onFailure { Log.w(TAG, "Unreadable ${file.path}, analysing the pack again", it) }
            }
            return analyse(dir).also { file.writeText(toJson(it).toString()) }
        }

        /** Builds the index of an unknown pack by reading its files (once per pack). */
        fun analyse(dir: File): PackIndex {
            val start = System.currentTimeMillis()
            val presets = dir.walkTopDown()
                .filter { it.isFile && it.extension.equals("milk", ignoreCase = true) }
                .map { Preset(it.relativeTo(dir).invariantSeparatorsPath, CoverPresets.isCompatible(it), null) }
                .sortedBy { it.file.lowercase() }
                .toList()
            val textures = dir.walkTopDown().maxDepth(2).firstOrNull { it.isDirectory && it.name.equals("textures", true) }
            val names = textures?.let { CoverPresets.textureNames(listOf(it)) } ?: emptyList()
            Log.i(TAG, "Pack ${dir.name} analysed: ${presets.size} presets, ${names.size} textures " +
                "(${System.currentTimeMillis() - start} ms)")
            return PackIndex(presets, textures?.relativeTo(dir)?.invariantSeparatorsPath, names, null)
        }

        /** Streaming read: an index lists up to a few thousand presets. */
        private fun parse(file: File): PackIndex {
            val presets = ArrayList<Preset>()
            var textureDir: String? = null
            val names = ArrayList<String>()
            var reference: Reference? = null
            JsonReader(file.bufferedReader()).use { r ->
                r.beginObject()
                while (r.hasNext()) when (r.nextName()) {
                    "presets" -> {
                        r.beginArray()
                        while (r.hasNext()) {
                            var path = ""
                            var image = false
                            var fps: Float? = null
                            r.beginObject()
                            while (r.hasNext()) when (r.nextName()) {
                                "file" -> path = r.nextString()
                                "image" -> image = r.nextBoolean()
                                "fps" -> fps = r.nextDouble().toFloat()
                                else -> r.skipValue()
                            }
                            r.endObject()
                            presets += Preset(path, image, fps)
                        }
                        r.endArray()
                    }
                    "textures" -> {
                        r.beginObject()
                        while (r.hasNext()) when (r.nextName()) {
                            "dir" -> textureDir = r.nextString()
                            "names" -> { r.beginArray(); while (r.hasNext()) names += r.nextString(); r.endArray() }
                            else -> r.skipValue()
                        }
                        r.endObject()
                    }
                    "reference" -> {
                        var device = ""
                        var height = 720
                        r.beginObject()
                        while (r.hasNext()) when (r.nextName()) {
                            "device" -> device = r.nextString()
                            "height" -> height = r.nextInt()
                            else -> r.skipValue()
                        }
                        r.endObject()
                        reference = Reference(device, height)
                    }
                    else -> r.skipValue()
                }
                r.endObject()
            }
            return PackIndex(presets, textureDir, names, reference)
        }

        private fun toJson(i: PackIndex) = JSONObject().apply {
            put("format", 1)
            put("presets", JSONArray().apply {
                i.presets.forEach { p ->
                    put(JSONObject().put("file", p.file).apply { if (p.image) put("image", true) })
                }
            })
            i.textureDir?.let { put("textures", JSONObject().put("dir", it).put("names", JSONArray(i.textureNames))) }
        }
    }
}
