<p align="center">
  <img src="../images/header.png" alt="Lactea" width="760" />
</p>

<p align="center">
  <a href="../README.md"><img src="../images/flags/flag-gb.svg" alt="" width="18" height="12" /> English</a>
  /
  <a href="README.de.md"><img src="../images/flags/flag-de.svg" alt="" width="18" height="12" /> Deutsch</a>
  /
  <a href="README.it.md"><img src="../images/flags/flag-it.svg" alt="" width="18" height="12" /> Italiano</a>
  /
  <a href="README.es.md"><img src="../images/flags/flag-es.svg" alt="" width="18" height="12" /> Español</a>
  /
  <a href="README.sv.md"><img src="../images/flags/flag-se.svg" alt="" width="18" height="12" /> Svenska</a>
  /
  <a href="README.fr.md"><img src="../images/flags/flag-fr.svg" alt="" width="18" height="12" /> Français</a>
</p>

<p align="center"><img src="../images/screenshots.jpg" alt="Lactea auf einem Fernseher" width="760" /></p>

# Lactea — Musikvisualisierung für Android TV

Lactea verwandelt die Musik, die auf Ihrem Fernseher läuft, in psychedelische Bilder, wie das
berühmte **MilkDrop**-Plugin von Winamp. Es reagiert auf **jede App, die Ton abspielt**: Spotify,
Deezer, YouTube Music, Tidal, Plex… Sie nutzen Ihre Musik-App wie gewohnt weiter; Lactea hört, was
der Fernseher abspielt, und zeichnet dazu.

Es basiert auf [projectM](https://github.com/projectM-visualizer/projectm), der freien
Neuumsetzung von MilkDrop, und spielt die originalen MilkDrop-Presets: fast **10.000
Visualisierungen**, die die MilkDrop-Community in zwanzig Jahren erstellt hat.

Aktuelle Version: **1.0.0** — [APK herunterladen](https://github.com/Karlos-fr/lactea/releases/latest) · [Neuigkeiten](../CHANGELOG.md) (auf Englisch).

## Funktionen

- **Reagiert auf alle Musik, die auf dem Fernseher läuft**, egal aus welcher App, auch wenn der Ton
  über HDMI (ARC / eARC) an eine Soundbar geht.
- **9.797 Presets** in 11 Paketen, die Sie in der App auswählen und herunterladen; keines ist
  fest eingebaut. Jedes Preset der Sammlung lädt, dank Korrekturen an projectM (siehe weiter
  unten).
- **Songtitel und Albumcover** auf dem Bildschirm, wenn der Song wechselt; das Cover kann auch in
  manchen Visualisierungen erscheinen.
- **Öffnet sich von selbst**, wenn Musik startet, und schließt sich, wenn sie endet (optional).
- **Für die Fernbedienung gemacht**: Jede Aktion hat eine Taste, und jede Taste lässt sich ändern.
- **Bis zu 4K**, mit einem Auto-Modus, der für jedes Preset die beste Auflösung wählt.
- **Flüssig auch auf einfachen Fernsehern**: Lactea weiß, wie anspruchsvoll jedes Preset ist, kann
  die langsamen überspringen und wählt zwischen Überblendung und Schnitt, damit Übergänge nicht
  ruckeln.
- **Favoriten und Ausschlüsse**, ein Preset-Browser, ein Zufallsmodus und eine Sperre für das
  aktuelle Preset.
- **Sechs Sprachen**: Englisch, Französisch, Deutsch, Italienisch, Spanisch und Schwedisch, je nach
  Sprache des Fernsehers (Englisch für jede andere Sprache), oder wählbar unter **Optionen › Sprache**.

## Voraussetzungen

- Ein Fernseher oder eine Box mit **Android TV oder Google TV**, Android 8.0 oder neuer, mit
  OpenGL ES 3.0.
- Getestet auf einem **Sony BRAVIA** (Google TV, Grafikchip Mali-G57).
- Etwa 8 MB für die App und etwa 30 MB Download für alle Preset-Pakete.

## Installation

1. Laden Sie `Lactea-….apk` von der Seite [Releases](https://github.com/Karlos-fr/lactea/releases)
   herunter.
2. Installieren Sie die Datei auf dem Fernseher, zum Beispiel mit einer Dateimanager-App von einem
   USB-Stick, oder von einem Computer aus: `adb install Lactea-….apk`.
3. Starten Sie **Lactea** aus der App-Liste des Fernsehers.
4. Lactea fragt nach der **Mikrofon-Berechtigung**: Erlauben Sie sie. Android verlangt sie, damit
   eine App den Ton verfolgen kann, den der Fernseher abspielt. Lactea nutzt kein Mikrofon und nimmt
   nichts auf: Es liest nur die Lautstärke, viele Male pro Sekunde, um die Bilder zu animieren.

Spielen Sie dann in einer beliebigen App Musik ab und kehren Sie zu Lactea zurück.

### Optionale Berechtigungen

Manche Funktionen brauchen Berechtigungen, die Android TV nicht auf dem Bildschirm abfragt. Erteilen
Sie diese in den Einstellungen des Fernsehers, falls er sie anbietet, oder einmalig von einem
Computer aus mit [adb](https://developer.android.com/tools/adb) (Entwickleroptionen und Debugging
auf dem Fernseher aktiviert):

| Funktion | Berechtigung | Vom Computer aus |
|---|---|---|
| Songtitel und Albumcover | Zugriff auf Benachrichtigungen | `adb shell cmd notification allow_listener fr.lactea/fr.lactea.nowplaying.NowPlayingListener` |
| Öffnet sich von selbst, wenn Musik läuft | Über anderen Apps einblenden | `adb shell appops set fr.lactea SYSTEM_ALERT_WINDOW allow` |

## Fernbedienung

| Taste | Aktion |
|---|---|
| ◀ ▶ | Vorheriges / nächstes Preset |
| ▲ ▼ | Audio-Empfindlichkeit + / − |
| OK | Songtitel anzeigen |
| **OK gedrückt halten** | **Optionen** (immer, nicht änderbar) |
| 🔴 Rot | Aktuelles Preset sperren |
| 🟢 Grün | Zufallsmodus ein / aus |
| 🟡 Gelb | Zu Favoriten hinzufügen / entfernen — gedrückt halten: Preset ausschließen |
| 🔵 Blau oder Menü | Optionen — Blau gedrückt halten: Preset-Browser |
| Info | Name des Presets — gedrückt halten: Liste der Tasten |
| Kanal + / − | Nächster / vorheriger Song |
| Wiedergabe / Pause, ⏭ ⏮ | Werden an Ihre Musik-App weitergegeben |
| 0 | FPS-Anzeige |
| Zurück | Ein Fenster schließen, dann Lactea beenden |

Jede Taste lässt sich unter **Optionen › Fernbedienung** ändern, mit kurzem oder
langem Druck.

## Optionen

Halten Sie **OK** gedrückt, um die Optionen zu öffnen. Sie sind nach Themen gruppiert:

- **Darstellung**: Render-Auflösung (von 540p bis 4K, oder Auto), höchste Auflösung im
  Auto-Modus, Bilder pro Sekunde (30 oder 60), Detailgrad des Gitters, FPS-Anzeige.
- **Presets und Übergänge**: wie lange jedes Preset bleibt, Überblendung
  oder Schnitt (Auto wählt für jedes Preset), Schnitte im Takt, Zufallsmodus, nur Favoriten,
  Preset-Name beim Wechsel.
- **Songtitel**: Anzeige beim Songwechsel, wie lange, wo, wie groß; neues Preset
  bei jedem Song.
- **Albumcover**: mit dem Titel, im Takt pulsierend, in den Visualisierungen,
  Online-Suche, wenn die Musik-App kein Cover liefert.
- **Autostart**: öffnen, wenn
  Musik läuft (nach welcher Zeit, nur Musik-Apps), schließen, wenn die Musik endet.
- **Preset-Verwaltung**: Browser, langsame Presets ausschließen, Pakete zum
  Herunterladen, die Presets auf diesem Fernseher testen (siehe unten), Messwerte zurücksetzen,
  Ausschlussliste leeren.
- **Audio**: Empfindlichkeit, Aufnahmemodus (konstanter Pegel oder der Lautstärke folgend).
- **Fernbedienung**: Tasten ändern.

## Preset-Pakete

Die APK enthält keine Presets: Beim ersten Start schlägt Lactea die Pakete zum Herunterladen
vor. Empfohlen wird **Essentials** (66 Presets: das Beste der Sammlung und Lacteas eigene
Presets); der Rest der Sammlung ist auf 10 Themenpakete verteilt (Dancer, Drawing, Fractal,
Geometric, Hypnotic, Particles, Reaction, Sparkle, Supernova, Waveform). Die Bilder, die die
Presets verwenden, bilden ein Paket **Textures**, das mit dem ersten Paket automatisch installiert
wird. Pakete fügen Sie jederzeit unter **Optionen › Preset-Verwaltung › Preset-Pakete
herunterladen** hinzu oder entfernen sie dort.

## Langsame Presets

Manche Presets verlangen dem Grafikchip viel ab. Auf dem Sony BRAVIA, der zum Testen diente, in
720p:

| | Presets | Anteil |
|---|---|---|
| Flüssig (55 Bilder pro Sekunde oder mehr) | 6.318 | 64 % |
| Ordentlich (45–55) | 736 | 7 % |
| Schwer (20–45) | 1.364 | 14 % |
| Zu schwer (unter 20) | 1.439 | 15 % |

Lactea wird mit diesen Messwerten ausgeliefert, daher funktioniert **Langsame Presets ausschließen**
schon beim ersten Start. Während die Presets laufen, misst Lactea
jedes erneut auf Ihrem Fernseher und verwendet dann seine eigenen Werte.

Um alle Presets auf Ihrem Fernseher in einem Durchgang zu messen: **Optionen › Preset-Verwaltung ›
Presets auf diesem TV testen**. Wählen Sie alles oder ein
Thema; etwa 3 Sekunden pro Preset (etwa 8 Stunden für die ganze Sammlung). Lassen Sie die Musik
weiterlaufen; jede Taste beendet den Test, und er kann dort weitermachen, wo er aufgehört hat.

## Bekannte Einschränkungen

- Wenn der Ton an **Bluetooth**-Kopfhörer oder -Lautsprecher geht, empfängt Lactea ihn
  möglicherweise nicht.
- Einige Presets verlieren einen Teil ihrer Animation: 7 wegen Fehlern, die schon MilkDrop selbst
  abgelehnt hat, 7 wegen eines [noch zu behebenden Unterschieds in projectM](https://github.com/projectM-visualizer/projectm-eval/issues/23).

## Wie Lactea entstanden ist

- **projectM, korrigiert.** Lactea verwendet eine eigene Version von projectM 4.1.8:
  [Karlos-fr/projectm](https://github.com/Karlos-fr/projectm). Der Test der ganzen Sammlung auf einem
  Fernseher hat gezeigt, dass 67 davon überhaupt nicht luden, weil projectM ihre Gleichungen anders
  las als MilkDrop. Die Korrektur folgt dem Quellcode von MilkDrop und wurde an den 144.906
  Gleichungsblöcken der Sammlung geprüft, ohne jede Regression. Sie wurde dem projectM-Team
  vorgeschlagen ([Issue](https://github.com/projectM-visualizer/projectm/issues/1083),
  [Pull Request](https://github.com/projectM-visualizer/projectm/pull/1084)).
- **Auf dem Fernseher gemessen.** Jedes Preset wurde auf dem Fernseher geladen und gemessen; daher
  stammen die Zahlen oben.
- **KI, ganz offen.** Lactea wurde mit einem KI-Programmieragenten geschrieben, **Anthropic Claude**
  (Claude Code), und sein Logo wurde mit **OpenAI Codex** gezeichnet. Ein Mensch leitet das Projekt,
  testet auf dem Fernseher und entscheidet.

Um Lactea selbst zu bauen: [BUILDING.md](BUILDING.md) (auf Englisch).

## Danksagungen und Lizenzen

- [projectM](https://github.com/projectM-visualizer/projectm): LGPL 2.1. Die von Lactea geänderte
  Version ist unter [Karlos-fr/projectm](https://github.com/Karlos-fr/projectm) veröffentlicht.
- Presets: [Cream of the Crop](https://github.com/projectM-visualizer/presets-cream-of-the-crop),
  gesammelt und sortiert von ISOSCELES; jedes Preset gehört seinen Autoren, die im Dateinamen
  genannt sind.
- Texturen: [MilkDrop texture pack](https://github.com/projectM-visualizer/presets-milkdrop-texture-pack).
- MilkDrop wurde von Ryan Geiss entwickelt.
