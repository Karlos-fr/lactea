# Lactea

Visualiseur musical façon MilkDrop pour **Android TV / Google TV**, basé sur
[projectM](https://github.com/projectM-visualizer/projectm). Il réagit à toute la musique jouée sur
la TV (Spotify, Deezer, YouTube Music…) en capturant le mixage audio global d'Android.

Voir [PLAN.md](PLAN.md) pour la feuille de route.

## Structure

| Dossier | Contenu |
|---|---|
| `app/` | Application Android (Kotlin) + pont JNI (`app/src/main/cpp`) |
| `third_party/projectm` | libprojectM 4.1.8, notre version corrigée ([Karlos-fr/projectm](https://github.com/Karlos-fr/projectm), branche `lactea/4.1.8`, LGPL) : équations lues comme MilkDrop, blocs invalides ignorés, messages d’erreur |
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

Touches par défaut (reconfigurables dans Options › Télécommande) : ◀ ▶ preset · ▲ ▼ sensibilité audio ·
🔴 verrou · 🟢 aléatoire · 🟡 favori (long : exclure) · 🔵 options (long : navigateur) · Info nom du preset
(long : aide) · 0 compteur FPS · **appui long OK : options (toujours)**.

## Packs de presets

```bash
python -I tools/make_packs.py          # → dist/packs/*.zip + manifest.json
```

Joindre le contenu de `dist/packs/` à une release GitHub : l'appli lit
`releases/latest/download/manifest.json` (dépôt public requis).

## Licences

- libprojectM : LGPL 2.1, utilisée sans modification, liée dynamiquement.
- Presets : voir la note de licence de *Cream of the Crop* (présumés libres, auteurs crédités dans les noms de fichiers).
