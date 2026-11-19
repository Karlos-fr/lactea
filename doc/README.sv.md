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

<p align="center"><img src="../images/screenshots.jpg" alt="Lactea på en tv" width="760" /></p>

# Lactea — musikvisualiserare för Android TV

Lactea förvandlar musiken som spelas på din tv till psykedeliska bilder, som det berömda
**MilkDrop**-pluginet till Winamp. Det reagerar på **alla appar som spelar ljud**: Spotify, Deezer,
YouTube Music, Tidal, Plex… Du använder din musikapp som vanligt. Lactea lyssnar på det tv:n spelar
och ritar.

Det bygger på [projectM](https://github.com/projectM-visualizer/projectm), den fria
nyskrivningen av MilkDrop, och spelar de ursprungliga MilkDrop-presetsen: nästan **10 000
visualiseringar** som MilkDrop-communityn har skapat under tjugo år.

Aktuell version: **1.0.0** — [ladda ner APK:n](https://github.com/Karlos-fr/lactea/releases/latest) · [nyheter](../CHANGELOG.md) (på engelska).

## Funktioner

- **Reagerar på all musik som spelas på tv:n**, oavsett app. Även när ljudet går till en soundbar
  via HDMI (ARC / eARC).
- **9 797 presets** i 11 paket som du väljer och laddar ner i appen; inget är inbyggt.
  Alla presets i samlingen laddas, tack vare rättningar i projectM (se längre ned).
- **Låttitel och skivomslag** på skärmen när låten byts. Omslaget kan också synas inuti vissa
  visualiseringar.
- **Öppnas av sig själv** när musik börjar spelas, och stängs när den slutar (valfritt).
- **Gjord för fjärrkontrollen**: varje funktion har en knapp, och varje knapp kan ändras.
- **Upp till 4K**, med ett Auto-läge som väljer bästa upplösning för varje preset.
- **Jämn även på enklare tv-apparater**: Lactea vet hur tunga presetsen är, kan hoppa över de
  långsamma och väljer mellan övertoning och klipp så att övergångarna inte hackar.
- **Favoriter och undantag**, en preset-bläddrare, ett blandat läge och låsning av aktuell preset.
- **Sex språk**: engelska, franska, tyska, italienska, spanska och svenska, enligt tv:ns språk
  (engelska för alla andra språk), eller valt under **Alternativ › Språk**.

## Systemkrav

- En tv eller box med **Android TV eller Google TV**, Android 8.0 eller senare, med OpenGL ES 3.0.
- Testad på en **Sony BRAVIA** (Google TV, grafikkrets Mali-G57).
- Cirka 8 MB för appen, och cirka 30 MB att ladda ner för alla preset-paket.

## Installation

1. Ladda ner `Lactea-….apk` från sidan [Releases](https://github.com/Karlos-fr/lactea/releases).
2. Installera den på tv:n, till exempel med en filhanterarapp från ett USB-minne, eller från en
   dator: `adb install Lactea-….apk`.
3. Starta **Lactea** från tv:ns applista.
4. Lactea ber om **behörighet för mikrofonen**: godkänn den. Android kräver det för att en app ska
   kunna följa ljudet som tv:n spelar. Lactea använder ingen mikrofon och spelar inte in något. Den
   läser bara ljudnivån, många gånger per sekund, för att animera bilderna.

Spela sedan musik i valfri app och gå tillbaka till Lactea.

### Valfria behörigheter

Vissa funktioner behöver behörigheter som Android TV inte frågar efter på skärmen. Ge dem i tv:ns
inställningar om tv:n erbjuder dem, eller en gång från en dator med
[adb](https://developer.android.com/tools/adb) (utvecklaralternativ och felsökning aktiverade på
tv:n):

| Funktion | Behörighet | Från en dator |
|---|---|---|
| Låttitel och skivomslag | Åtkomst till aviseringar | `adb shell cmd notification allow_listener fr.lactea/fr.lactea.nowplaying.NowPlayingListener` |
| Öppnas av sig själv när musik spelas | Visa ovanpå andra appar | `adb shell appops set fr.lactea SYSTEM_ALERT_WINDOW allow` |

## Fjärrkontroll

| Knapp | Funktion |
|---|---|
| ◀ ▶ | Föregående / nästa preset |
| ▲ ▼ | Ljudkänslighet + / − |
| OK | Visa låttiteln |
| **Håll in OK** | **Alternativ** (alltid, kan inte ändras) |
| 🔴 Röd | Lås aktuell preset |
| 🟢 Grön | Blandat läge på / av |
| 🟡 Gul | Lägg till i / ta bort från favoriter — håll in: undanta presetet |
| 🔵 Blå eller Meny | Alternativ — håll in Blå: preset-bläddrare |
| Info | Presetets namn — håll in: lista över knapparna |
| Kanal + / − | Nästa / föregående låt |
| Spela / Paus, ⏭ ⏮ | Skickas till din musikapp |
| 0 | Räknare för bilder per sekund |
| Tillbaka | Stäng en panel, och lämna sedan Lactea |

Varje knapp kan ändras i **Alternativ › Fjärrkontroll**, med ett kort eller långt tryck.

## Alternativ

Håll in **OK** för att öppna **Alternativ**. De är grupperade efter tema:

- **Bild**: renderingsupplösning (från 540p till 4K, eller Auto), högsta upplösning i
  Auto-läget, bilder per sekund (30 eller 60), detaljnivå för nätet, räknare för bilder per sekund.
- **Presets och övergångar**: när presetet byts (bara vid varje låt, eller även var 10:e sekund
  till var 5:e minut), stämningar (vilka presets som spelas: kategorier, minsta FPS), spellistor (handplockade presets, i ordning eller blandat), övertoning
  eller klipp (Auto väljer för varje preset), klipp i takt med musiken, blandat läge, bara
  favoriter, presetets namn vid byte.
- **Låttitel**: visas vid låtbyte, hur länge, var, hur stor.
- **Skivomslag**: med titeln, pulserar i takt, inuti visualiseringarna, sökning på nätet
  när musikappen inte ger något omslag.
- **Automatisk start**: öppnas när musik
  spelas (efter hur lång tid, bara musikappar), stängs när musiken slutar.
- **Hantera presets**: bläddrare, undanta långsamma presets, paket att
  ladda ner, testa presetsen på den här tv:n (se nedan), nollställ mätningarna, töm listan över
  undantag.
- **Ljud**: känslighet, inspelningsläge (konstant nivå, eller följer volymen).
- **Fjärrkontroll**: ändra knapparna.

## Preset-paket

APK:n innehåller inga presets: vid första starten föreslår Lactea paket att ladda ner.
**Essentials** (60 presets: det bästa ur samlingen) och **Lactea** (Lacteas egna presets) rekommenderas; resten
av samlingen är uppdelad i 10 tematiska paket (Dancer, Drawing, Fractal, Geometric, Hypnotic,
Particles, Reaction, Sparkle, Supernova, Waveform). Bilderna som presetsen använder finns i ett
paket **Textures**, som installeras automatiskt med det första paketet. Lägg till eller ta bort
paket när du vill under **Alternativ › Hantera presets › Preset-paket att ladda ner**.

## Långsamma presets

Vissa presets kräver mycket av grafikkretsen. På Sony BRAVIA-tv:n som användes för testerna, i 720p:

| | Presets | Andel |
|---|---|---|
| Jämna (55 bilder per sekund eller mer) | 6 318 | 64 % |
| Godkända (45–55) | 736 | 7 % |
| Tunga (20–45) | 1 364 | 14 % |
| För tunga (under 20) | 1 439 | 15 % |

Lactea levereras med dessa mätningar, så **Undanta långsamma presets**
fungerar från första start. När varje preset spelas mäter Lactea det igen på din tv och använder
sina egna siffror.

Vill du mäta alla presets på din tv på en gång? Välj **Alternativ › Hantera presets › Testa presets på
den här tv:n**. Välj allt eller ett tema. Det tar cirka
3 sekunder per preset (cirka 8 timmar för hela samlingen). Låt musiken spela. Valfri knapp stoppar
testet, och det kan fortsätta där det slutade.

## Kända begränsningar

- När ljudet går till hörlurar eller högtalare via **Bluetooth** kan det hända att Lactea inte tar
  emot det.
- Några presets tappar en del av sin animation: 7 på grund av fel som MilkDrop själv avvisade,
  7 på grund av en [skillnad som ännu ska rättas i projectM](https://github.com/projectM-visualizer/projectm-eval/issues/23).

## Hur Lactea gjordes

- **projectM, rättat.** Lactea använder en egen version av projectM 4.1.8:
  [Karlos-fr/projectm](https://github.com/Karlos-fr/projectm). När hela samlingen testades på en
  tv visade det sig att 67 av dem inte laddades alls. projectM läste deras ekvationer på ett annat
  sätt än MilkDrop. Rättningen följer MilkDrops källkod och har kontrollerats på samlingens
  144 906 ekvationsblock, utan någon regression. Den har föreslagits till projectM-teamet
  ([issue](https://github.com/projectM-visualizer/projectm/issues/1083),
  [pull request](https://github.com/projectM-visualizer/projectm/pull/1084)).
- **Mätt på tv:n.** Varje preset laddades och tidmättes på tv:n. Det ger siffrorna ovan.
- **AI, helt öppet.** Lactea skrevs med en AI-agent för programmering, **Anthropic Claude**
  (Claude Code), och logotypen ritades med **OpenAI Codex**. En människa leder projektet, testar på
  tv:n och fattar besluten.

Vill du bygga Lactea själv: [BUILDING.md](BUILDING.md) (på engelska).

## Tack och licenser

- [projectM](https://github.com/projectM-visualizer/projectm): LGPL 2.1. Lacteas ändrade version
  finns publicerad på [Karlos-fr/projectm](https://github.com/Karlos-fr/projectm).
- Presets: [Cream of the Crop](https://github.com/projectM-visualizer/presets-cream-of-the-crop),
  samlade och sorterade av ISOSCELES. Varje preset tillhör sina upphovspersoner, som nämns i
  filnamnet.
- Texturer: [MilkDrop texture pack](https://github.com/projectM-visualizer/presets-milkdrop-texture-pack).
- MilkDrop skapades av Ryan Geiss.
