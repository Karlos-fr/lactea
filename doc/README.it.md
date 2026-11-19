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

<p align="center"><img src="../images/screenshots.jpg" alt="Lactea su una TV" width="760" /></p>

# Lactea — visualizzatore musicale per Android TV

Lactea trasforma la musica riprodotta sulla tua TV in immagini psichedeliche, come il famoso plugin
**MilkDrop** di Winamp. Reagisce a **qualsiasi app che riproduce audio**: Spotify, Deezer, YouTube
Music, Tidal, Plex… Continui a usare la tua app musicale come al solito; Lactea ascolta ciò che
riproduce la TV e disegna.

Si basa su [projectM](https://github.com/projectM-visualizer/projectm), la riscrittura open source di
MilkDrop, e riproduce i preset MilkDrop originali: quasi **10.000 visualizzazioni** create dalla
comunità di MilkDrop in vent'anni.

Versione attuale: **1.0.0** — [scarica l'APK](https://github.com/Karlos-fr/lactea/releases/latest) · [novità](../CHANGELOG.md) (in inglese).

## Funzionalità

- **Reagisce a tutta la musica riprodotta sulla TV**, qualunque sia l'app, anche quando l'audio va
  a una soundbar via HDMI (ARC / eARC).
- **9.803 preset** in 11 pacchetti da scegliere e scaricare dall'app; nessuno è incluso di serie.
  Tutti i preset della raccolta si caricano, grazie alle correzioni apportate a projectM (vedi più
  sotto).
- **Titolo del brano e copertina dell'album** sullo schermo a ogni cambio di brano; la copertina può
  anche apparire dentro alcune visualizzazioni.
- **Si apre da solo** quando parte la musica, e si chiude quando si ferma (facoltativo).
- **Pensato per il telecomando**: ogni azione ha il suo tasto, e ogni tasto si può cambiare.
- **Fino al 4K**, con una modalità Auto che sceglie la risoluzione migliore per ogni preset.
- **Fluido anche su TV modeste**: Lactea sa quanto è pesante ogni preset, può saltare quelli lenti,
  e sceglie tra dissolvenza e stacco netto perché le transizioni non scattino.
- **Preferiti ed esclusioni**, un browser dei preset, una modalità casuale e il blocco del preset
  in corso.
- **Sei lingue**: inglese, francese, tedesco, italiano, spagnolo e svedese, secondo la lingua della
  TV (inglese per qualsiasi altra lingua), o scelta in **Opzioni › Lingua**.
- **Delicato per gli occhi**: ogni preset è etichettato ⚡ flash, 🌙 chill o OK e ha un'anteprima;
  riproduci solo quelli calmi, o nessuno con flash.
- **Telecomando web**: scansiona un codice QR con il telefono, o apri la pagina su un computer, per
  sfogliare i preset, creare playlist e cambiare tutte le opzioni.

## Requisiti

- Una TV o un box con **Android TV o Google TV**, Android 8.0 o successivo, con OpenGL ES 3.0.
- Provato su una **Sony BRAVIA** (Google TV, chip grafico Mali-G57).
- Circa 8 MB per l'app, e circa 120 MB da scaricare per tutti i pacchetti di preset.

## Installazione

1. Scarica `Lactea-….apk` dalla pagina [Releases](https://github.com/Karlos-fr/lactea/releases).
2. Installalo sulla TV, ad esempio con un'app di gestione file da una chiavetta USB, oppure da un
   computer: `adb install Lactea-….apk`.
3. Avvia **Lactea** dall'elenco delle app della TV.
4. Lactea chiede l'**autorizzazione al microfono**: accettala. Android la richiede perché un'app
   possa seguire l'audio riprodotto dalla TV. Lactea non usa alcun microfono e non registra nulla:
   legge solo il livello del suono, molte volte al secondo, per animare le immagini.

Poi metti un po' di musica in qualsiasi app e torna su Lactea.

### Autorizzazioni facoltative

Alcune funzioni richiedono autorizzazioni che Android TV non chiede sullo schermo. Concedile dalle
impostazioni della TV, se la tua TV le offre, oppure una volta sola da un computer con
[adb](https://developer.android.com/tools/adb) (opzioni sviluppatore e debug attivati sulla TV):

| Funzione | Autorizzazione | Da un computer |
|---|---|---|
| Titolo e copertina del brano | Accesso alle notifiche | `adb shell cmd notification allow_listener fr.lactea/fr.lactea.nowplaying.NowPlayingListener` |
| Apertura automatica quando suona la musica | Visualizzazione sopra altre app | `adb shell appops set fr.lactea SYSTEM_ALERT_WINDOW allow` |

## Telecomando

| Tasto | Azione |
|---|---|
| ◀ ▶ | Preset precedente / successivo |
| ▲ ▼ | Sensibilità audio + / − |
| OK | Mostra il titolo del brano |
| **Tieni premuto OK** | **Opzioni** (sempre, non modificabile) |
| 🔴 Rosso | Blocca il preset in corso |
| 🟢 Verde | Modalità casuale sì / no |
| 🟡 Giallo | Aggiungi ai preferiti / togli — tenuto premuto: escludi il preset |
| 🔵 Blu o Menu | Opzioni — Blu tenuto premuto: browser dei preset |
| Info | Nome del preset — tenuto premuto: elenco dei tasti |
| Canale + / − | Brano successivo / precedente |
| Play / Pausa, ⏭ ⏮ | Inviati alla tua app musicale |
| 0 | Contatore di FPS |
| Indietro | Chiude un pannello, poi esce da Lactea |

Ogni tasto si può cambiare in **Opzioni › Telecomando**, con una pressione breve o lunga.

## Telecomando web

Finché Lactea è sullo schermo, offre una pagina sulla rete locale. In **Opzioni › Telecomando ›
Mostra il codice QR**, scansiona il codice con un telefono, o digita l'indirizzo mostrato sotto nel
browser di un computer (stessa rete). La pagina ha l'aspetto dei pannelli di Lactea e segue la TV
in diretta:

- **In riproduzione**: anteprima del preset, brano, precedente / successivo, preferito, blocco,
  atmosfera.
- **Preset**: tutti i preset installati con anteprima e ricerca; aprine uno per riprodurlo,
  segnarlo, etichettarlo o aggiungerlo a delle playlist.
- **Atmosfere**: atmosfere e playlist (riordina i preset di una playlist).
- **Opzioni**: tutte le opzioni della TV, pacchetti compresi.

Su un computer, Esc torna indietro e ← / → cambiano preset. L'indirizzo cambia a ogni avvio di
Lactea (porta 8771); il telecomando web si disattiva nello stesso menu.

## Opzioni

Tieni premuto **OK** per aprire le opzioni. Sono raggruppate per tema:

- **Aspetto**: risoluzione di rendering (da 540p a 4K, oppure Auto), risoluzione
  massima in modalità Auto, fotogrammi al secondo (30 o 60), dettaglio della griglia, contatore di
  FPS.
- **Preset e transizioni**: quando cambiare preset (solo a ogni brano, o anche ogni 10 s - 5 min),
  atmosfere (quali preset vengono riprodotti: categorie, FPS minimi), playlist (preset scelti a mano, in ordine o a caso), dissolvenza o stacco netto
  (Auto sceglie per ogni preset), stacchi a tempo di musica, modalità casuale, solo preferiti, nome
  del preset al cambio.
- **Titolo del brano**: mostrato al cambio di brano, per quanto tempo, dove, quanto grande.
- **Copertina**: con il titolo, che pulsa a tempo, dentro le visualizzazioni, ricerca
  online quando l'app musicale non fornisce la copertina.
- **Avvio automatico**: apertura quando suona
  la musica (dopo quanto tempo, solo app musicali), chiusura quando la musica si ferma.
- **Gestione preset**: browser, escludi i preset lenti, pacchetti da
  scaricare, test dei preset su questa TV (vedi sotto), azzera le misure, svuota l'elenco delle
  esclusioni.
- **Audio**: sensibilità, modalità di cattura (livello costante, oppure che segue il volume).
- **Telecomando**: cambia i tasti.

## Pacchetti di preset

L'APK non contiene preset: al primo avvio, Lactea propone i pacchetti da scaricare. Sono
consigliati **Essentials** (60 preset: il meglio della raccolta) e **Lactea** (i preset propri di Lactea); il
resto della raccolta è diviso in 10 pacchetti a tema (Dancer, Drawing, Fractal, Geometric,
Hypnotic, Particles, Reaction, Sparkle, Supernova, Waveform). Le immagini usate dai preset formano
un pacchetto **Textures**, installato automaticamente con il primo pacchetto. Aggiungi o rimuovi
pacchetti quando vuoi da **Opzioni › Gestione preset › Pacchetti di preset da scaricare**.

Il pacchetto **Lactea** contiene i preset fatti per Lactea: due analizzatori di spettro (quello di
Winamp e uno al neon), due viaggi in un wormhole, una laguna soleggiata sotto acqua e vetro (Eau),
fuochi d'artificio (Feux d'artifice) e sei preset costruiti intorno alla copertina.

Ogni preset dei pacchetti è stato renderizzato e analizzato su un PC: è etichettato ⚡ flash, 🌙 chill
o OK, e il browser dei preset mostra la sua anteprima (4 momenti). Le atmosfere possono escludere i
preset con flash (**Senza flash**) o tenere solo quelli calmi (**Solo chill**). Nel browser, 🟢
cambia l'etichetta di un preset, 🔵 lo aggiunge alla playlist in uso, e il preset selezionato viene
riprodotto dietro il browser dopo un secondo.

## Preset lenti

Alcuni preset chiedono molto al chip grafico. Sulla Sony BRAVIA usata per i test, in 720p:

| | Preset | Quota |
|---|---|---|
| Fluidi (55 fotogrammi al secondo o più) | 6.318 | 64 % |
| Corretti (45–55) | 736 | 7 % |
| Pesanti (20–45) | 1.364 | 14 % |
| Troppo pesanti (meno di 20) | 1.439 | 15 % |

Lactea include queste misure, quindi **Escludi i preset lenti** funziona
fin dal primo avvio. Man mano che i preset vengono riprodotti, Lactea li misura di nuovo sulla tua
TV e usa i propri dati.

Per misurare tutti i preset sulla tua TV in una volta: **Opzioni › Gestione preset › Testa i preset su
questa TV**. Scegli tutto o un solo tema; circa 3 secondi
per preset (circa 8 ore per l'intera raccolta). Lascia la musica in riproduzione; qualsiasi tasto
ferma il test, che può riprendere da dove si era fermato.

## Limiti noti

- Quando l'audio va a cuffie o casse **Bluetooth**, Lactea potrebbe non riceverlo.
- Alcuni preset perdono parte della loro animazione: 7 per errori che MilkDrop stesso rifiutava,
  7 per una [differenza ancora da correggere in projectM](https://github.com/projectM-visualizer/projectm-eval/issues/23).

## Come è stato fatto Lactea

- **projectM, corretto.** Lactea usa una propria versione di projectM 4.1.8:
  [Karlos-fr/projectm](https://github.com/Karlos-fr/projectm). Il test di tutta la raccolta su una TV ha
  mostrato che 67 di essi non si caricavano affatto, perché projectM leggeva le loro equazioni in modo
  diverso da MilkDrop. La correzione segue il codice sorgente di MilkDrop ed è stata verificata sui
  144.906 blocchi di equazioni della raccolta, senza alcuna regressione. È stata proposta al team di
  projectM ([issue](https://github.com/projectM-visualizer/projectm/issues/1083),
  [pull request](https://github.com/projectM-visualizer/projectm/pull/1084)).
- **Misurato sulla TV.** Ogni preset è stato caricato e cronometrato sulla TV, da cui i dati qui
  sopra.
- **L'IA, in modo trasparente.** Lactea è stato scritto con un agente di programmazione IA,
  **Anthropic Claude** (Claude Code), e il suo logo è stato disegnato con **OpenAI Codex**. Una
  persona guida il progetto, fa i test sulla TV e decide.

Per compilare Lactea da te: [BUILDING.md](BUILDING.md) (in inglese).

## Crediti e licenze

- [projectM](https://github.com/projectM-visualizer/projectm): LGPL 2.1. La versione modificata da
  Lactea è pubblicata su [Karlos-fr/projectm](https://github.com/Karlos-fr/projectm).
- Preset: [Cream of the Crop](https://github.com/projectM-visualizer/presets-cream-of-the-crop),
  raccolti e ordinati da ISOSCELES; ogni preset appartiene ai suoi autori, citati nel nome del file.
- Texture: [MilkDrop texture pack](https://github.com/projectM-visualizer/presets-milkdrop-texture-pack).
- MilkDrop è stato creato da Ryan Geiss.
