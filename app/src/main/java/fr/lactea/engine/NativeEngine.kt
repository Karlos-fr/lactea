package fr.lactea.engine

/** Thin JNI wrapper around libprojectM. All calls except loading must run on the GL thread. */
object NativeEngine {
    init {
        System.loadLibrary("projectM-4")
        System.loadLibrary("lactea")
    }

    external fun create(texturePath: String): Long
    external fun setTexturePaths(handle: Long, paths: Array<String>)
    external fun destroy(handle: Long)
    external fun resize(handle: Long, width: Int, height: Int)
    external fun render(handle: Long)
    external fun addPcmU8(handle: Long, data: ByteArray)
    external fun loadPreset(handle: Long, path: String, smooth: Boolean)

    /** 0 = none, 1 = soft cut requested, 2 = hard cut requested. */
    external fun consumeSwitchRequest(handle: Long): Int
    external fun consumeError(handle: Long): String?

    external fun setFps(handle: Long, fps: Int)
    external fun setMeshSize(handle: Long, width: Int, height: Int)
    external fun setPresetDuration(handle: Long, seconds: Double)
    external fun setSoftCutDuration(handle: Long, seconds: Double)
    external fun setHardCut(handle: Long, enabled: Boolean, sensitivity: Float)
    external fun setBeatSensitivity(handle: Long, sensitivity: Float)
    external fun setPresetLocked(handle: Long, locked: Boolean)
}
