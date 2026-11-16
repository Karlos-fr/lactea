# Lactea

Visualiseur musical façon MilkDrop pour **Android TV / Google TV**, basé sur
[projectM](https://github.com/projectM-visualizer/projectm). Il réagit à toute la musique jouée sur
la TV (Spotify, Deezer, YouTube Music…) en capturant le mixage audio global d'Android.

Voir [PLAN.md](PLAN.md) pour la feuille de route.

## Structure

| Dossier | Contenu |
|---|---|
| `app/` | Application Android (Kotlin) + pont JNI (`app/src/main/cpp`) |
| `third_party/projectm` | libprojectM 4.1.8 (sous-module, LGPL, non modifié) |
| `third_party/presets-cream-of-the-crop` | Collection de presets (sous-module) |
| `third_party/presets-milkdrop-texture-pack` | Textures utilisées par les presets (sous-module) |
| `spike-visualizer/` | Test initial de capture audio (étape 0) |
| `tools/` | Banc d'essai FPS (`bench.sh`, `bench_report.py`) |
| `logos/` | Pistes de logo |

## Compiler

```bash
git clone --recurse-submodules <repo>
./gradlew assembleDebug
```

Prérequis : SDK Android (plateforme 37), NDK 30.0.16248370, CMake 4.1.2, JDK 17+.

## Installer sur la TV

```bash
adb install -r -g app/build/outputs/apk/debug/app-debug.apk
```

Touches (étape 1) : ◀ ▶ preset · ▲ ▼ résolution de rendu · OK masquer l'overlay.

## Licences

- libprojectM : LGPL 2.1, utilisée sans modification, liée dynamiquement.
- Presets : voir la note de licence de *Cream of the Crop* (présumés libres, auteurs crédités dans les noms de fichiers).
