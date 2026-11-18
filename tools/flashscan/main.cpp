// flashscan: renders MilkDrop presets offline with projectM and records, for every frame, a
// coarse 16x9 grid of the image (relative luminance and red), for the flash analysis done by
// tools/flash_report.py. Optionally writes a video clip of chosen presets (calibration).
//
// Usage:
//   flashscan --root <presets dir> --list <file of preset paths, relative to root>
//             --audio <raw float32 stereo 44.1 kHz> --textures <dir> --out <dir>
//             [--seconds 14] [--fps 30] [--width 640] [--height 360]
//             [--clips <dir>] [--ffmpeg <ffmpeg.exe>] [--wav <audio for the clips>]
//
// Output, in <out>:
//   index.jsonl      one line per preset: {"key", "file", "frames", "status", "ms"}
//   thumbs/<n>.rgb   4 images (at 3, 6, 9 and 12 s) of 160x90 RGB, top-down, for the previews
//   frames/<n>.bin   frames x 16 x 9 cells x 3 bytes: luminance, red ratio, red value
//                    (WCAG 2.x definitions, sRGB):
//                      luminance = relative luminance x 255
//                      red ratio = R / (R + G + B) x 255
//                      red value = clamp((R - G - B) x 320, 0, 320) x 255 / 320
// Presets already in index.jsonl are skipped, so an interrupted run resumes.

#define NOMINMAX
#include <windows.h>
#include <timeapi.h>

#include <GL/glew.h>
#include <GLFW/glfw3.h>
#include <projectM-4/projectM.h>

#include <algorithm>
#include <array>
#include <chrono>
#include <cmath>
#include <cstdio>
#include <cstring>
#include <filesystem>
#include <fstream>
#include <iostream>
#include <set>
#include <sstream>
#include <string>
#include <vector>

namespace fs = std::filesystem;

// Ask hybrid laptops for the dedicated GPU.
extern "C" {
__declspec(dllexport) unsigned long NvOptimusEnablement = 1;
__declspec(dllexport) int AmdPowerXpressRequestHighPerformance = 1;
}

namespace {

constexpr int GRID_W = 16;
constexpr int GRID_H = 9;
constexpr int SAMPLE_RATE = 44100;
constexpr int THUMB_W = 160;
constexpr int THUMB_H = 90;
constexpr double THUMB_TIMES[] = {3.0, 6.0, 9.0, 12.0};

struct Options {
    fs::path root, list, audio, textures, out, clips, wav;
    std::string ffmpeg = "ffmpeg";
    double seconds = 14.0;
    int fps = 30, width = 640, height = 360;
};

std::string jsonEscape(const std::string& s) {
    std::string o;
    for (unsigned char c : s) {
        switch (c) {
            case '"': o += "\\\""; break;
            case '\\': o += "\\\\"; break;
            case '\n': o += "\\n"; break;
            case '\r': o += "\\r"; break;
            case '\t': o += "\\t"; break;
            default:
                if (c < 0x20) { char b[8]; std::snprintf(b, sizeof b, "\\u%04x", c); o += b; }
                else o += static_cast<char>(c);
        }
    }
    return o;
}

/** Keys already analysed, read back from index.jsonl. */
std::set<std::string> doneKeys(const fs::path& index) {
    std::set<std::string> keys;
    std::ifstream in(index);
    std::string line;
    const std::string tag = "{\"key\": \"";
    while (std::getline(in, line)) {
        if (line.rfind(tag, 0) != 0) continue;
        std::string key;
        for (size_t i = tag.size(); i < line.size(); i++) {
            if (line[i] == '\\' && i + 1 < line.size()) { key += line[++i]; continue; }
            if (line[i] == '"') break;
            key += line[i];
        }
        keys.insert(key);
    }
    return keys;
}

std::string lastError;

void onSwitchFailed(const char*, const char* message, void*) {
    lastError = message ? message : "?";
}

float srgbToLinear(float c) {
    return c <= 0.04045f ? c / 12.92f : std::pow((c + 0.055f) / 1.055f, 2.4f);
}

}  // namespace

int main(int argc, char** argv) {
    Options o;
    for (int i = 1; i + 1 < argc; i += 2) {
        std::string a = argv[i];
        std::string v = argv[i + 1];
        if (a == "--root") o.root = fs::u8path(v);
        else if (a == "--list") o.list = fs::u8path(v);
        else if (a == "--audio") o.audio = fs::u8path(v);
        else if (a == "--textures") o.textures = fs::u8path(v);
        else if (a == "--out") o.out = fs::u8path(v);
        else if (a == "--clips") o.clips = fs::u8path(v);
        else if (a == "--wav") o.wav = fs::u8path(v);
        else if (a == "--ffmpeg") o.ffmpeg = v;
        else if (a == "--seconds") o.seconds = std::stod(v);
        else if (a == "--fps") o.fps = std::stoi(v);
        else if (a == "--width") o.width = std::stoi(v);
        else if (a == "--height") o.height = std::stoi(v);
        else { std::cerr << "unknown option " << a << "\n"; return 2; }
    }
    // A long batch without a visible window: ask Windows not to slow it down to save energy.
    PROCESS_POWER_THROTTLING_STATE throttling{};
    throttling.Version = PROCESS_POWER_THROTTLING_CURRENT_VERSION;
    throttling.ControlMask = PROCESS_POWER_THROTTLING_EXECUTION_SPEED | PROCESS_POWER_THROTTLING_IGNORE_TIMER_RESOLUTION;
    throttling.StateMask = 0;
    SetProcessInformation(GetCurrentProcess(), ProcessPowerThrottling, &throttling, sizeof throttling);
    SetThreadExecutionState(ES_CONTINUOUS | ES_SYSTEM_REQUIRED);
    timeBeginPeriod(1);

    if (o.root.empty() || o.list.empty() || o.audio.empty() || o.out.empty()) {
        std::cerr << "usage: flashscan --root DIR --list FILE --audio FILE --out DIR [options]\n";
        return 2;
    }

    // Audio: interleaved float32 stereo; looped if shorter than a preset.
    std::vector<float> pcm;
    {
        std::ifstream in(o.audio, std::ios::binary);
        in.seekg(0, std::ios::end);
        pcm.resize(static_cast<size_t>(in.tellg()) / sizeof(float));
        in.seekg(0);
        in.read(reinterpret_cast<char*>(pcm.data()), static_cast<std::streamsize>(pcm.size() * sizeof(float)));
    }
    const size_t pcmFrames = pcm.size() / 2;
    if (pcmFrames == 0) { std::cerr << "empty audio\n"; return 1; }

    std::vector<std::string> presets;
    {
        std::ifstream in(o.list);
        std::string line;
        while (std::getline(in, line)) {
            while (!line.empty() && (line.back() == '\r' || line.back() == '\n')) line.pop_back();
            if (!line.empty()) presets.push_back(line);
        }
    }

    fs::create_directories(o.out / "frames");
    fs::create_directories(o.out / "thumbs");
    if (!o.clips.empty()) fs::create_directories(o.clips);
    const fs::path indexPath = o.out / "index.jsonl";
    const auto done = doneKeys(indexPath);
    std::ofstream index(indexPath, std::ios::app);

    // OpenGL 3.3 core context in a hidden window of the render size.
    if (!glfwInit()) { std::cerr << "glfwInit failed\n"; return 1; }
    glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 3);
    glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 3);
    glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
    glfwWindowHint(GLFW_VISIBLE, GLFW_FALSE);
    glfwWindowHint(GLFW_RESIZABLE, GLFW_FALSE);
    GLFWwindow* window = glfwCreateWindow(o.width, o.height, "flashscan", nullptr, nullptr);
    if (!window) { std::cerr << "no OpenGL 3.3 context\n"; return 1; }
    glfwMakeContextCurrent(window);
    glfwSwapInterval(0);
    glewExperimental = GL_TRUE;
    glewInit();
    std::cerr << "GL_RENDERER " << reinterpret_cast<const char*>(glGetString(GL_RENDERER)) << "\n";

    projectm_handle pm = projectm_create();
    if (!pm) { std::cerr << "projectm_create failed\n"; return 1; }
    projectm_set_window_size(pm, static_cast<size_t>(o.width), static_cast<size_t>(o.height));
    projectm_set_fps(pm, o.fps);
    projectm_set_mesh_size(pm, 48, 32);  // as on the TV
    projectm_set_preset_duration(pm, 1e7);
    projectm_set_hard_cut_enabled(pm, false);
    projectm_set_aspect_correction(pm, true);
    projectm_set_preset_switch_failed_event_callback(pm, onSwitchFailed, nullptr);
    std::string texDir = o.textures.u8string();
    const char* texPaths[] = {texDir.c_str()};
    if (!o.textures.empty()) projectm_set_texture_search_paths(pm, texPaths, 1);

    // sRGB -> linear lookup.
    std::array<float, 256> lin{};
    for (int i = 0; i < 256; i++) lin[i] = srgbToLinear(i / 255.0f);

    const int frames = static_cast<int>(std::lround(o.seconds * o.fps));
    const size_t samplesPerFrame = SAMPLE_RATE / static_cast<size_t>(o.fps);
    std::vector<unsigned char> pixels(static_cast<size_t>(o.width) * o.height * 4);
    std::vector<unsigned char> grid(static_cast<size_t>(frames) * GRID_W * GRID_H * 3);
    std::vector<float> chunk(samplesPerFrame * 2);
    std::vector<unsigned char> thumbs(std::size(THUMB_TIMES) * THUMB_W * THUMB_H * 3);
    double clock = 0.0;
    int n = static_cast<int>(done.size());

    for (const auto& rel : presets) {
        std::string key = rel.size() > 5 && rel.compare(rel.size() - 5, 5, ".milk") == 0 ? rel.substr(0, rel.size() - 5) : rel;
        if (done.count(key)) continue;
        const fs::path file = o.root / fs::u8path(rel);
        const auto start = std::chrono::steady_clock::now();
        lastError.clear();
        projectm_set_frame_time(pm, clock);
        projectm_load_preset_file(pm, file.u8string().c_str(), false);

        FILE* clip = nullptr;
        if (!o.clips.empty()) {
            std::ostringstream cmd;
            cmd << "\"\"" << o.ffmpeg << "\" -v error -y -f rawvideo -pix_fmt rgba -s " << o.width << "x" << o.height
                << " -r " << o.fps << " -i - ";
            if (!o.wav.empty()) cmd << "-i \"" << o.wav.u8string() << "\" -shortest -c:a aac -b:a 160k ";
            cmd << "-vf vflip -c:v libx264 -preset veryfast -crf 20 -pix_fmt yuv420p \""
                << (o.clips / (std::to_string(n) + ".mp4")).u8string() << "\"\"";
            clip = _popen(cmd.str().c_str(), "wb");
        }

        size_t audioPos = 0;
        for (int f = 0; f < frames; f++) {
            for (size_t s = 0; s < samplesPerFrame; s++) {
                const size_t src = (audioPos + s) % pcmFrames;
                chunk[s * 2] = pcm[src * 2];
                chunk[s * 2 + 1] = pcm[src * 2 + 1];
            }
            audioPos += samplesPerFrame;
            projectm_pcm_add_float(pm, chunk.data(), static_cast<unsigned int>(samplesPerFrame), PROJECTM_STEREO);
            projectm_set_frame_time(pm, clock);
            clock += 1.0 / o.fps;
            glViewport(0, 0, o.width, o.height);
            projectm_opengl_render_frame(pm);
            // projectM may leave its own framebuffer bound for reading (the image before the comp
            // shader): read the window, where the final image is.
            glBindFramebuffer(GL_READ_FRAMEBUFFER, 0);
            glReadBuffer(GL_BACK);
            glReadPixels(0, 0, o.width, o.height, GL_RGBA, GL_UNSIGNED_BYTE, pixels.data());
            if (clip) fwrite(pixels.data(), 1, pixels.size(), clip);

            // Preview images: box-filtered down to THUMB_W x THUMB_H.
            for (size_t t = 0; t < std::size(THUMB_TIMES); t++) {
                if (f != static_cast<int>(std::lround(THUMB_TIMES[t] * o.fps)) - 1) continue;
                const int bx = o.width / THUMB_W, by = o.height / THUMB_H;
                for (int ty = 0; ty < THUMB_H; ty++) {
                    for (int tx = 0; tx < THUMB_W; tx++) {
                        int acc[3] = {0, 0, 0};
                        for (int y = 0; y < by; y++) {
                            const unsigned char* px = &pixels[(static_cast<size_t>(o.height - 1 - (ty * by + y)) * o.width + tx * bx) * 4];
                            for (int x = 0; x < bx; x++, px += 4) { acc[0] += px[0]; acc[1] += px[1]; acc[2] += px[2]; }
                        }
                        unsigned char* d = &thumbs[((t * THUMB_H + ty) * THUMB_W + tx) * 3];
                        for (int c = 0; c < 3; c++) d[c] = static_cast<unsigned char>(acc[c] / (bx * by));
                    }
                }
            }

            // Grid: average over each cell (rows are bottom-up; the grid is stored top-down).
            for (int gy = 0; gy < GRID_H; gy++) {
                for (int gx = 0; gx < GRID_W; gx++) {
                    const int x0 = gx * o.width / GRID_W, x1 = (gx + 1) * o.width / GRID_W;
                    const int y0 = gy * o.height / GRID_H, y1 = (gy + 1) * o.height / GRID_H;
                    double lum = 0, r = 0, g = 0, b = 0;
                    for (int y = y0; y < y1; y++) {
                        const unsigned char* row = &pixels[(static_cast<size_t>(o.height - 1 - y) * o.width + x0) * 4];
                        for (int x = x0; x < x1; x++, row += 4) {
                            lum += 0.2126f * lin[row[0]] + 0.7152f * lin[row[1]] + 0.0722f * lin[row[2]];
                            r += row[0]; g += row[1]; b += row[2];
                        }
                    }
                    const double count = static_cast<double>(x1 - x0) * (y1 - y0);
                    lum /= count; r /= count * 255; g /= count * 255; b /= count * 255;
                    const double sum = r + g + b;
                    const double ratio = sum > 1e-6 ? r / sum : 0.0;
                    const double redValue = std::clamp((r - g - b) * 320.0, 0.0, 320.0);
                    unsigned char* cell = &grid[((static_cast<size_t>(f) * GRID_H + gy) * GRID_W + gx) * 3];
                    cell[0] = static_cast<unsigned char>(std::lround(std::clamp(lum, 0.0, 1.0) * 255));
                    cell[1] = static_cast<unsigned char>(std::lround(ratio * 255));
                    cell[2] = static_cast<unsigned char>(std::lround(redValue * 255 / 320));
                }
            }
            glfwPollEvents();
        }
        if (clip) _pclose(clip);

        {
            std::ofstream th(o.out / "thumbs" / (std::to_string(n) + ".rgb"), std::ios::binary);
            th.write(reinterpret_cast<const char*>(thumbs.data()), static_cast<std::streamsize>(thumbs.size()));
        }
        const std::string name = std::to_string(n) + ".bin";
        {
            std::ofstream bin(o.out / "frames" / name, std::ios::binary);
            bin.write(reinterpret_cast<const char*>(grid.data()), static_cast<std::streamsize>(grid.size()));
        }
        const auto ms = std::chrono::duration_cast<std::chrono::milliseconds>(std::chrono::steady_clock::now() - start).count();
        index << "{\"key\": \"" << jsonEscape(key) << "\", \"file\": \"" << name << "\", \"frames\": " << frames
              << ", \"status\": \"" << (lastError.empty() ? "ok" : "failed") << "\", \"error\": \""
              << jsonEscape(lastError) << "\", \"ms\": " << ms << "}\n";
        index.flush();
        n++;
        std::cerr << n << "/" << presets.size() << " " << ms << " ms " << key << "\n";
    }

    projectm_destroy(pm);
    glfwDestroyWindow(window);
    glfwTerminate();
    return 0;
}
