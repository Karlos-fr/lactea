# Changelog

All notable changes to Lactea are listed here, newest first.

## [1.0.0] — 2026-11-17

First release: MilkDrop-style music visuals for Android TV and Google TV, built on a corrected
version of projectM 4.1.8.

### Added

- Visualizer: the original MilkDrop presets rendered with projectM, reacting to the music played
  on the TV by any app (Spotify, Deezer, YouTube Music, Tidal, Plex…), including when the sound goes
  to a soundbar over HDMI (ARC / eARC).
- Presets: 66 built in (60 from the Cream of the Crop collection and 6 made for Lactea) and 10 themed
  packs to download from the options, 9,857 presets in all. The packs come from the `packs/` folder
  of the GitHub repository; the list refreshes by itself at start-up and when the packs screen
  opens, and a message says when an installed pack has an update.
- Track title and album cover on song change, with a glow of the cover's colour and a pulse on the
  beat. The cover comes from the music app, or from Deezer and iTunes when the app gives none, and
  also appears inside the presets that can show an image, and in 6 presets made for it.
- Automatic launch: Lactea opens by itself when music has been playing for a while (music apps only,
  delay to choose), and closes when the music stops.
- Remote control: every action has a key (next / previous preset, lock, shuffle, favourite,
  exclusion, title, preset name, audio sensitivity, options, preset browser, key help, FPS counter,
  next / previous song) and every key can be changed, with a short or a long press. Holding OK always
  opens the options.
- Options panel: render resolution from 540p to 4K or Auto, frames per second, mesh detail, preset
  duration, fade or cut transitions (Auto chooses for each preset), cuts on the beat, shuffle,
  favourites only, title and cover settings, automatic launch, audio sensitivity and capture mode.
- Smooth on modest TVs: Lactea ships with the frame rate of every preset measured on a reference TV
  (Sony BRAVIA, Mali-G57), can exclude the slow presets from the first launch, and measures the
  presets again on the TV as they play.
- Preset test: Options › Preset management › Test the presets on this TV measures every preset, or one
  theme, and can resume where it stopped.
- Preset browser with categories, favourites, excluded and slow presets.
- Six languages: English, French, German, Italian, Spanish and Swedish, following the TV's language
  (English otherwise) or chosen in Options › Language.
- 32-bit and 64-bit ARM in one APK (armeabi-v7a, arm64-v8a); Android 8.0 or later.

### Fixed in projectM

Lactea uses [Karlos-fr/projectm](https://github.com/Karlos-fr/projectm), projectM 4.1.8 with
MilkDrop compatibility fixes, proposed to the projectM team
([issue](https://github.com/projectM-visualizer/projectm/issues/1083),
[pull request](https://github.com/projectM-visualizer/projectm/pull/1084)):

- Preset equations are read like MilkDrop reads them: lines glued together without a separator.
- A faulty block of equations no longer rejects the whole preset; it is skipped, as in MilkDrop.
- Error messages are no longer lost (`std::exception`).

With these fixes, all 9,857 presets of the collection load, instead of 9,790.

### Known limitations

- Track title and automatic launch need permissions that Android TV does not ask for on screen:
  give them from the TV settings when offered, or once from a computer with adb (see the README).
- When the sound goes to Bluetooth headphones or speakers, Lactea may not receive it.
- A few presets lose part of their animation: 7 because of errors MilkDrop itself rejected, 7
  because a lone `.` is not read as 0 by projectm-eval
  ([projectm-eval#23](https://github.com/projectM-visualizer/projectm-eval/issues/23)).
- Tested on one TV (Sony BRAVIA, Google TV); the 64-bit version is not tested on a real device yet.

[1.0.0]: https://github.com/Karlos-fr/lactea/releases/tag/v1.0.0
