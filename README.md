<p align="center">
  <img src="images/header.png" alt="Lactea" width="760" />
</p>

<p align="center">
  <a href="README.md"><img src="images/flags/flag-gb.svg" alt="" width="18" height="12" /> English</a>
  /
  <a href="doc/README.de.md"><img src="images/flags/flag-de.svg" alt="" width="18" height="12" /> Deutsch</a>
  /
  <a href="doc/README.it.md"><img src="images/flags/flag-it.svg" alt="" width="18" height="12" /> Italiano</a>
  /
  <a href="doc/README.es.md"><img src="images/flags/flag-es.svg" alt="" width="18" height="12" /> Español</a>
  /
  <a href="doc/README.sv.md"><img src="images/flags/flag-se.svg" alt="" width="18" height="12" /> Svenska</a>
  /
  <a href="doc/README.fr.md"><img src="images/flags/flag-fr.svg" alt="" width="18" height="12" /> Français</a>
</p>

<p align="center"><img src="images/screenshots.jpg" alt="Lactea on a TV" width="760" /></p>

# Lactea — music visualizer for Android TV

Lactea turns the music played on your TV into psychedelic visuals, like the famous **MilkDrop**
plugin of Winamp. It reacts to **any app that plays sound**: Spotify, Deezer, YouTube Music, Tidal,
Plex… You keep using your music app as usual; Lactea listens to what the TV plays and draws.

It is built on [projectM](https://github.com/projectM-visualizer/projectm), the open-source
reimplementation of MilkDrop, and plays the original MilkDrop presets: almost **10,000
visualizations** made by the MilkDrop community over twenty years.

Current version: **1.0.0** — [download the APK](https://github.com/Karlos-fr/lactea/releases/latest) · [what's new](CHANGELOG.md).

## Features

- **Reacts to all the music played on the TV**, whatever the app, including when the sound goes to
  a soundbar over HDMI (ARC / eARC).
- **9,797 presets** in 11 packs you choose and download from the app; nothing is built in.
  Every preset of the collection loads, thanks to [fixes made to projectM](#how-lactea-was-made).
- **Track title and album cover** on screen when the song changes; the cover can also appear
  inside some visualizations.
- **Opens by itself** when music starts playing, and closes when it stops (optional).
- **Made for the remote**: every action has a key, and every key can be changed.
- **Up to 4K**, with an Auto mode that picks the best resolution for each preset.
- **Smooth on modest TVs**: Lactea knows how heavy each preset is, can skip the slow ones, and
  chooses between a fade and a cut so that transitions don't stutter.
- **Favourites and exclusions**, a preset browser, a shuffle mode and a lock on the current preset.
- **Six languages**: English, French, German, Italian, Spanish and Swedish, following the TV's
  language (English for any other language), or chosen in **Options › Language**.

## Requirements

- A TV or box running **Android TV or Google TV**, Android 8.0 or later, with OpenGL ES 3.0.
- Tested on a **Sony BRAVIA** (Google TV, Mali-G57 graphics chip).
- About 8 MB for the app, and about 30 MB to download for all the preset packs.

## Install

1. Download `Lactea-….apk` from the [Releases](../../releases) page.
2. Install it on the TV, for example with a file manager app from a USB stick, or from a computer:
   `adb install Lactea-….apk`.
3. Start **Lactea** from the TV's app list.
4. Lactea asks for the **microphone permission**: accept it. Android requires it to let an app
   follow the sound the TV plays. Lactea uses no microphone and records nothing: it only reads
   the sound level, many times per second, to animate the visuals.

Then play some music in any app and come back to Lactea.

### Optional permissions

Some features need permissions that Android TV does not ask for on screen. Give them from the TV
settings if your TV offers them, or once from a computer with
[adb](https://developer.android.com/tools/adb) (developer options and debugging enabled on the
TV):

| Feature | Permission | From a computer |
|---|---|---|
| Track title and album cover | Notification access | `adb shell cmd notification allow_listener fr.lactea/fr.lactea.nowplaying.NowPlayingListener` |
| Opens by itself when music plays | Display over other apps | `adb shell appops set fr.lactea SYSTEM_ALERT_WINDOW allow` |

## Remote control

| Key | Action |
|---|---|
| ◀ ▶ | Previous / next preset |
| ▲ ▼ | Audio sensitivity + / − |
| OK | Show the track title |
| **Hold OK** | **Options** (always, cannot be changed) |
| 🔴 Red | Lock the current preset |
| 🟢 Green | Shuffle on / off |
| 🟡 Yellow | Add to / remove from favourites — hold: exclude the preset |
| 🔵 Blue or Menu | Options — hold Blue: preset browser |
| Info | Name of the preset — hold: list of the keys |
| Channel + / − | Next / previous song |
| Play / Pause, ⏭ ⏮ | Sent to your music app |
| 0 | FPS counter |
| Back | Close a panel, then leave Lactea |

Every key can be changed in **Options › Remote control**, with a short or a long press.

## Options

Hold **OK** to open the options. They are grouped by theme:

- **Visual**: render resolution (from 540p to 4K, or Auto), highest resolution in Auto mode,
  frames per second (30 or 60), mesh detail, FPS counter.
- **Presets and transitions**: when to change preset (on each song only, or also every 10 s to
  5 min), ambiances (which presets play: categories, minimum FPS), fade or cut (Auto chooses for
  each preset), cuts on the beat, shuffle, favourites only, preset name on change.
- **Track title**: shown on song change, how long, where, how big.
- **Album cover**: with the title, pulsing with the beat, inside the visualizations, online search
  when the music app gives no cover.
- **Automatic launch**: open when music plays (after how long, music apps only),
  close when the music stops.
- **Preset management**: browser, exclude slow presets, packs to download, test the presets on this
  TV (see below), reset the measures, empty the exclusion list.
- **Audio**: sensitivity, capture mode (constant level, or following the volume).
- **Remote control**: change the keys.

## Preset packs

The APK holds no preset: on first launch, Lactea offers the packs to download. **Essentials**
(60 presets: the best of the collection) and **Lactea** (Lactea's own presets) are recommended; the rest of the
collection is split into 10 themed packs (Dancer, Drawing, Fractal, Geometric, Hypnotic, Particles,
Reaction, Sparkle, Supernova, Waveform). The images the presets use come in a **Textures** pack,
installed automatically with the first pack. Add or remove packs at any time in
**Options › Preset management › Preset packs to download**.

## Slow presets

Some presets ask a lot of the graphics chip. On the Sony BRAVIA used for testing, in 720p:

| | Presets | Share |
|---|---|---|
| Smooth (55 frames per second or more) | 6,318 | 64 % |
| Correct (45–55) | 736 | 7 % |
| Heavy (20–45) | 1,364 | 14 % |
| Too heavy (under 20) | 1,439 | 15 % |

Lactea ships with these measures, so **Exclude slow presets** works from the first launch. As each
preset plays, Lactea measures it again on your TV and uses its own figures.

To measure every preset on your TV in one go: **Options › Preset management › Test the presets on
this TV**. Choose everything or one theme; about 3 seconds per preset (about 8 hours for the whole
collection). Leave the music playing; any key stops the test, and it can resume where it stopped.

## Known limitations

- When the sound goes to **Bluetooth** headphones or speakers, Lactea may not receive it.
- A few presets lose part of their animation: 7 because of errors that MilkDrop itself rejected,
  7 because of a [difference still to be fixed in projectM](https://github.com/projectM-visualizer/projectm-eval/issues/23).

## How Lactea was made

- **projectM, corrected.** Lactea uses its own version of projectM 4.1.8:
  [Karlos-fr/projectm](https://github.com/Karlos-fr/projectm). Testing the whole collection on a TV
  showed that 67 of them did not load at all, because projectM read their equations differently
  from MilkDrop. The fix follows MilkDrop's source code and was checked on the 144,906 equation
  blocks of the collection, without any regression. It has been proposed to the projectM team
  ([issue](https://github.com/projectM-visualizer/projectm/issues/1083),
  [pull request](https://github.com/projectM-visualizer/projectm/pull/1084)).
- **Measured on the TV.** Each preset was loaded and timed on the TV, which gives the figures above.
- **AI, openly.** Lactea was written with an AI coding agent, **Anthropic Claude** (Claude Code),
  and its logo was drawn with **OpenAI Codex**. A human leads the project, tests on the TV and
  decides.

To build Lactea yourself: [doc/BUILDING.md](doc/BUILDING.md).

## Credits and licences

- [projectM](https://github.com/projectM-visualizer/projectm): LGPL 2.1. Lactea's modified
  version is published at [Karlos-fr/projectm](https://github.com/Karlos-fr/projectm).
- Presets: [Cream of the Crop](https://github.com/projectM-visualizer/presets-cream-of-the-crop),
  collected and sorted by ISOSCELES; each preset belongs to its authors, named in its file name.
- Textures: [MilkDrop texture pack](https://github.com/projectM-visualizer/presets-milkdrop-texture-pack).
- MilkDrop was created by Ryan Geiss.
