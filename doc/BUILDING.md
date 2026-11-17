# Building Lactea

## Repository layout

| Folder | Content |
|---|---|
| `app/` | Android app (Kotlin) and JNI bridge (`app/src/main/cpp`) |
| `third_party/projectm` | libprojectM 4.1.8, Lactea's corrected version ([Karlos-fr/projectm](https://github.com/Karlos-fr/projectm), branch `lactea/4.1.8`, LGPL 2.1) |
| `third_party/presets-cream-of-the-crop` | Preset collection (submodule) |
| `third_party/presets-milkdrop-texture-pack` | Textures used by the presets (submodule) |
| `tools/` | Preset packs, benchmark reports, reference FPS table, `eqcheck` (equation checker) |
| `docs/` | Screenshots, benchmark results, reports sent to projectM |
| `PLAN.md` | Roadmap and notes of each development step (French) |

## Build

```bash
git clone --recurse-submodules https://github.com/Karlos-fr/lactea.git
cd lactea
./gradlew assembleDebug
```

Requirements: Android SDK (platform 37), NDK 30.0.16248370, CMake 4.1.2, JDK 17 or later.
The APK is built for `armeabi-v7a` (it also runs on 64-bit ARM TVs).

## Install on the TV

```bash
adb install -r -g app/build/outputs/apk/debug/app-debug.apk
```

## Preset packs

```bash
python -I tools/make_packs.py        # -> dist/packs/*.zip + manifest.json
```

Attach the content of `dist/packs/` to a GitHub release: the app reads
`releases/latest/download/manifest.json` (the repository must be public).

## Reference FPS table

The benchmark of the whole collection on a TV (`files/bench/bench.tsv` in the app's data) becomes a
report and the table shipped in the app:

```bash
python -I tools/bench_presets_report.py docs/bench/bench-BRAVIA_4K_AE2.tsv BRAVIA_4K_AE2
python -I tools/make_reference.py
```

## Development extras

`am start -n fr.lactea/.VisualizerActivity` accepts: `--ei res 1080` (render height, 0 = Auto),
`--es preset "Category/name"` or `--es preset_b64 <base64>` (load a preset), `--es bench all` or a
category prefix (benchmark), `--ez bench_stop true`.
