# Changelog

All notable changes to Lactea are listed here, newest first.

## [Unreleased]

Faster start-up and an APK without presets: every preset now comes from a pack the user chooses.

### Added

- Welcome screen on first launch: Lactea lists the packs to download, with **Essentials**
  (60 presets: the best of the collection) and **Lactea** (Lactea's own presets) ticked as
  recommended.
- **Essentials**, **Lactea** and **Textures** packs. Textures holds the images the presets use; it is
  installed automatically with the first pack, and for users upgrading from 1.0.
- Each pack holds an `index.json` listing its presets, those able to show the album cover and the
  frame rates measured on the reference TV.
- Ambiances (Options › Presets and transitions › Ambiances): saved selections of the presets to
  play, by category and minimum frame rate. The preset browser lists the presets of the ambiance
  in use; a remote action (no key by default) switches to the next ambiance.
- Change preset: "on each song only", and every 3 or 5 minutes.
- Flash and chill tags: every preset of the packs was rendered and analysed on a PC
  (tools/flashscan) for flashes (ITU-R BT.1702 / WCAG thresholds, general and red flashes) and
  for how much the image moves. Presets are tagged ⚡ flash, 🌙 chill or OK; the thresholds were
  calibrated on clips judged by eye. Ambiances can leave out flashing presets ("No flash") or keep
  only the calm ones ("Chill only"), and the green key of the preset browser sets a tag by hand.
- Previews: each pack holds a picture of every preset (4 moments in a 2 x 2 mosaic, about 9 KB),
  shown beside the list in the preset browser. When the selection rests on a preset for a second,
  it plays behind the browser, which turns see-through; closing the browser without choosing
  brings back the preset that was playing.
- Ambiances have a name, typed with the remote's keyboard (or voice) when creating one, and can be
  renamed. Four ambiances are offered once: Chill evening, No flash, Party (Dancer, Particles,
  Sparkle, Supernova) and Smooth (no flash, at least 50 FPS).
- New preset "Lactea - Spectre" (Lactea pack): Winamp's spectrum analyser, identical to the original
  (classic skin, default settings): same FFT equalisation, 19 bands over 75 columns, bars falling
  12/16 px per frame, peaks falling faster and faster, the 76 x 16 pixel grid with its dotted
  background and the skin's colours.
- New preset "Lactea - Spectre neon" (Lactea pack): 48 mirrored neon bars (bass in the centre)
  growing from a glowing middle line, with trails, bloom and chromatic aberration on the bass.
- New presets "Lactea - Trou de ver film" and "Lactea - Trou de ver serie" (Lactea pack): a trip
  through a wormhole, after the classic science-fiction sequences: a winding tunnel of blue and
  white plasma with a dark core, and a tunnel of star streaks slowly flattening into a corridor of
  light, with lens flares. Their reactions to the music are smoothed, so they do not flash.
- New preset "Lactea - Eau" (Lactea pack): the bottom of a sunny lagoon with moving caustics,
  under the glass and water animations of Proscenium (calm water, liquid, rain). Each attack of
  the bass drops a drop with spreading rings, the swell follows the sound level like a tide, the
  mids speed up the current and the treble makes the crests glint.
- The test of the presets on the TV keeps running behind the options: Options › Preset
  management › Test the presets on this TV shows its progress and can stop it.

### Changed

- Start-up: Lactea no longer scans nor reads the presets. It reads the index of each installed
  pack in the background (about 0.2 s for the whole collection), and the screen shows at once. On
  the test TV, the screen now appears in 1.4 s instead of about 5 s.
- The APK no longer holds presets, textures nor reference frame rates (5.3 MB instead of 9.4 MB).
- Packs are versioned from their content: an unchanged pack keeps its version, a changed one is
  offered as an update.
- "Preset duration" and "New preset on each song" are one setting, "Change preset": a new song
  always brings a new preset, plus every 10 s to 5 min, or never. A change applies at once. Cuts
  on the beat stay a setting of their own.
- The preset browser is wider (90 % of the screen), so long names show in full.

### Upgrading from 1.0

- The 66 built-in presets and the textures copied by 1.0 are removed; install Essentials and Lactea
  to get them back. Favourites, exclusions and measures of the 60 built-in presets of the collection move
  to their key in the packs.
- Packs installed by 1.0 have no index yet: Lactea analyses each of them once (about 30 s for the
  whole collection, in the background), until they are updated.

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
