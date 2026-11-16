// JNI bridge between the Kotlin app and libprojectM.
// Every call except the constructor must happen on the GL thread that owns the EGL context.

#include <jni.h>
#include <android/log.h>
#include <GLES3/gl3.h>
#include <projectM-4/projectM.h>

#include <atomic>
#include <mutex>
#include <string>
#include <vector>

#define LOG_TAG "LACTEA_NATIVE"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGW(...) __android_log_print(ANDROID_LOG_WARN, LOG_TAG, __VA_ARGS__)

namespace {

struct Engine {
    projectm_handle pm = nullptr;
    // 0 = none, 1 = soft cut (blend), 2 = hard cut
    std::atomic<int> switchRequest{0};
    std::mutex errorMutex;
    std::string lastError;
};

Engine* fromHandle(jlong h) { return reinterpret_cast<Engine*>(h); }

void onSwitchRequested(bool isHardCut, void* userData) {
    static_cast<Engine*>(userData)->switchRequest = isHardCut ? 2 : 1;
}

void onSwitchFailed(const char* file, const char* message, void* userData) {
    auto* e = static_cast<Engine*>(userData);
    std::lock_guard<std::mutex> lock(e->errorMutex);
    e->lastError = std::string(file ? file : "?") + " :: " + (message ? message : "?");
    LOGW("Preset failed: %s", e->lastError.c_str());
}

std::string toString(JNIEnv* env, jstring s) {
    if (!s) return {};
    const char* c = env->GetStringUTFChars(s, nullptr);
    std::string out(c);
    env->ReleaseStringUTFChars(s, c);
    return out;
}

}  // namespace

#define JNI_FN(name) Java_fr_lactea_engine_NativeEngine_##name

extern "C" {

JNIEXPORT jlong JNICALL JNI_FN(create)(JNIEnv* env, jobject, jstring texturePath) {
    auto* e = new Engine();
    e->pm = projectm_create();
    if (!e->pm) {
        LOGW("projectm_create failed (no GLES3 context?)");
        delete e;
        return 0;
    }
    std::string tex = toString(env, texturePath);
    const char* paths[] = {tex.c_str()};
    projectm_set_texture_search_paths(e->pm, paths, 1);
    projectm_set_preset_switch_requested_event_callback(e->pm, onSwitchRequested, e);
    projectm_set_preset_switch_failed_event_callback(e->pm, onSwitchFailed, e);
    projectm_set_aspect_correction(e->pm, true);

    char* version = projectm_get_version_string();
    LOGI("projectM %s created, GL_RENDERER=%s, GL_VERSION=%s", version,
         reinterpret_cast<const char*>(glGetString(GL_RENDERER)),
         reinterpret_cast<const char*>(glGetString(GL_VERSION)));
    projectm_free_string(version);
    return reinterpret_cast<jlong>(e);
}

JNIEXPORT void JNICALL JNI_FN(setTexturePaths)(JNIEnv* env, jobject, jlong h, jobjectArray paths) {
    jsize n = env->GetArrayLength(paths);
    std::vector<std::string> storage;
    storage.reserve(static_cast<size_t>(n));
    for (jsize i = 0; i < n; ++i) {
        auto s = static_cast<jstring>(env->GetObjectArrayElement(paths, i));
        storage.push_back(toString(env, s));
        env->DeleteLocalRef(s);
    }
    std::vector<const char*> ptrs;
    for (auto& s : storage) ptrs.push_back(s.c_str());
    projectm_set_texture_search_paths(fromHandle(h)->pm, ptrs.data(), ptrs.size());
}

JNIEXPORT void JNICALL JNI_FN(destroy)(JNIEnv*, jobject, jlong h) {
    auto* e = fromHandle(h);
    if (!e) return;
    projectm_destroy(e->pm);
    delete e;
}

JNIEXPORT void JNICALL JNI_FN(resize)(JNIEnv*, jobject, jlong h, jint w, jint hh) {
    projectm_set_window_size(fromHandle(h)->pm, static_cast<size_t>(w), static_cast<size_t>(hh));
}

JNIEXPORT void JNICALL JNI_FN(render)(JNIEnv*, jobject, jlong h) {
    glClearColor(0.f, 0.f, 0.f, 1.f);
    glClear(GL_COLOR_BUFFER_BIT);
    projectm_opengl_render_frame(fromHandle(h)->pm);
}

JNIEXPORT void JNICALL JNI_FN(addPcmU8)(JNIEnv* env, jobject, jlong h, jbyteArray data) {
    jsize n = env->GetArrayLength(data);
    jbyte* bytes = env->GetByteArrayElements(data, nullptr);
    projectm_pcm_add_uint8(fromHandle(h)->pm, reinterpret_cast<const uint8_t*>(bytes),
                           static_cast<unsigned int>(n), PROJECTM_MONO);
    env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);
}

JNIEXPORT void JNICALL JNI_FN(loadPreset)(JNIEnv* env, jobject, jlong h, jstring path, jboolean smooth) {
    std::string p = toString(env, path);
    projectm_load_preset_file(fromHandle(h)->pm, p.c_str(), smooth == JNI_TRUE);
}

JNIEXPORT void JNICALL JNI_FN(loadPresetData)(JNIEnv* env, jobject, jlong h, jstring data, jboolean smooth) {
    std::string d = toString(env, data);
    projectm_load_preset_data(fromHandle(h)->pm, d.c_str(), smooth == JNI_TRUE);
}

JNIEXPORT jint JNICALL JNI_FN(consumeSwitchRequest)(JNIEnv*, jobject, jlong h) {
    return fromHandle(h)->switchRequest.exchange(0);
}

JNIEXPORT jstring JNICALL JNI_FN(consumeError)(JNIEnv* env, jobject, jlong h) {
    auto* e = fromHandle(h);
    std::lock_guard<std::mutex> lock(e->errorMutex);
    if (e->lastError.empty()) return nullptr;
    jstring s = env->NewStringUTF(e->lastError.c_str());
    e->lastError.clear();
    return s;
}

JNIEXPORT void JNICALL JNI_FN(setFps)(JNIEnv*, jobject, jlong h, jint fps) {
    projectm_set_fps(fromHandle(h)->pm, fps);
}

JNIEXPORT void JNICALL JNI_FN(setMeshSize)(JNIEnv*, jobject, jlong h, jint w, jint hh) {
    projectm_set_mesh_size(fromHandle(h)->pm, static_cast<size_t>(w), static_cast<size_t>(hh));
}

JNIEXPORT void JNICALL JNI_FN(setPresetDuration)(JNIEnv*, jobject, jlong h, jdouble s) {
    projectm_set_preset_duration(fromHandle(h)->pm, s);
}

JNIEXPORT void JNICALL JNI_FN(setSoftCutDuration)(JNIEnv*, jobject, jlong h, jdouble s) {
    projectm_set_soft_cut_duration(fromHandle(h)->pm, s);
}

JNIEXPORT void JNICALL JNI_FN(setHardCut)(JNIEnv*, jobject, jlong h, jboolean enabled, jfloat sensitivity) {
    projectm_set_hard_cut_enabled(fromHandle(h)->pm, enabled == JNI_TRUE);
    projectm_set_hard_cut_sensitivity(fromHandle(h)->pm, sensitivity);
}

JNIEXPORT void JNICALL JNI_FN(setBeatSensitivity)(JNIEnv*, jobject, jlong h, jfloat s) {
    projectm_set_beat_sensitivity(fromHandle(h)->pm, s);
}

JNIEXPORT void JNICALL JNI_FN(setPresetLocked)(JNIEnv*, jobject, jlong h, jboolean locked) {
    projectm_set_preset_locked(fromHandle(h)->pm, locked == JNI_TRUE);
}

}  // extern "C"
