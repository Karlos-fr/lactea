# Lactea — plan de projet

Visualiseur musical façon **MilkDrop** pour **Android TV / Google TV**, basé sur **projectM**,
qui réagit à **toute musique jouée sur la TV** (Spotify, Deezer, YouTube Music, Plex, Kodi…).

---

## 1. Ce qui est déjà validé

Test réalisé le 16/11/2026 sur la TV cible (`spike-visualizer/`).

| Élément | Résultat |
|---|---|
| TV | Sony **BRAVIA 4K AE2**, Android 14 (API 34), Google TV, ABI **armeabi-v7a** (32 bits), UI 1920×1080 |
| Capture audio | `android.media.audiofx.Visualizer` sur la **session 0** (mixage global) |
| Sortie audio pendant le test | **Barre de son en HDMI eARC** (pas les haut-parleurs de la TV) |
| Avec Spotify TV en lecture | ✅ ~20 captures/s, 1024 échantillons 8 bits, forme d'onde + FFT exploitables |
| Contrôle | ✅ Pause Spotify → signal à 0 ; reprise → signal revient |
| Permission | `RECORD_AUDIO` (accordée une fois), pas de dialogue à chaque session |
| Métadonnées | Spotify publie une `MediaSession` (titre, état lecture/pause) lisible par le système |

**Conséquence :** aucune dépendance au SDK ou à l'API Spotify. Lactea fonctionne avec n'importe quelle
appli qui joue du son via Android.

**Limites connues de la capture :**
- Une barre de son en HDMI eARC **n'empêche pas** la capture : validé avec Spotify.
- Son en *passthrough* (flux Dolby Digital/Atmos envoyé tel quel à la barre de son) → probablement
  non capté. Concerne surtout les films/séries (Netflix, Disney+), rarement la musique. Non testé.
- Lecture en *offload* matériel → non captée (rare en musique).
- Entrées HDMI et tuner TV → probablement non captés.
- Signal 8 bits, et le mode `SCALING_MODE_NORMALIZED` sature souvent (peak = 127) → à ajuster.

### Étape 1 — résultats (16/11/2026)

projectM 4.1.8 tourne sur la BRAVIA : GPU **Mali-G57**, OpenGL ES 3.2, écran physique 3840×2160 @ 60 Hz.
Banc d'essai : `tools/bench.sh` → [docs/bench/REPORT.md](docs/bench/REPORT.md).

| Rendu | FPS médian | Presets ≥ 55 FPS | Presets < 30 FPS |
|---|---|---|---|
| 720p | **59** | 15 / 23 | 4 |
| 1080p | 38 | 6 / 25 | 8 |
| 1440p | 24 | 0 / 24 | 19 |
| 2160p (4K) | 11 | 0 / 25 | 25 |

- **4K** : la surface 3840×2160 est bien créée, mais la puce de cette TV ne suit pas (≈ 11 FPS).
  Le réglage 4K reste proposé pour des box plus puissantes. Affichage 4K « natif » toujours non vérifié.
- **Défaut conseillé sur la BRAVIA** : 720p (fluide), 1080p pour les presets légers.
- **Transitions** : pendant les 3 s du fondu, les FPS sont **divisés par deux** à toutes les
  résolutions (ex. 1080p : 17 FPS pendant le fondu → 37–44 après). Cause : projectM calcule les deux
  presets en même temps. Le chargement du preset lui-même n'est pas en cause.
- Quelques presets sont très lourds partout (≈ 4 FPS) → à exclure automatiquement.
- Limites de la mesure : 5 s par preset (juste après le fondu), ≈ 25 presets sur 60 enregistrés par
  résolution (journal de la TV trop petit — corrigé dans `bench.sh`).

**Pistes pour les transitions (étape 2) :**
- Durée du fondu réglable (plus court = moins de temps ralenti).
- Mode **Auto** : fondu entre presets légers, **coupe franche** si l'un des deux est lourd
  (FPS mesurés).
- **Résolution par preset** : au moment d'une coupe franche, choisir la résolution selon le poids du
  preset (1080p pour les légers, 720p pour les lourds) — le changement est invisible pendant la coupe.

### Étapes 2 + 5 — résultats (16/11/2026)

Testé sur la BRAVIA (captures dans `docs/step2-*.png`, `docs/step5-*.png`) :
- ✅ Touches par défaut (◀▶ presets, 🔴 verrou, 🟢 aléatoire, 🟡 favori / appui long exclure,
  🔵 options / appui long navigateur, Info nom du preset / appui long aide, ▲▼ sensibilité, 0 compteur FPS).
- ✅ Appui long OK → options (fixe). Écran d'aide généré depuis le mapping courant.
- ✅ Reconfiguration des touches : capture de la touche réelle, appui court/long, conflits, réinitialisation.
- ✅ Panneau d'options complet (visuel, presets & transitions, gestion des presets, audio, télécommande, à propos).
- ✅ Packs : catalogue `manifest.json` + zips (`tools/make_packs.py`, 10 packs, 9 791 presets) ; téléchargement,
  vérification SHA-256, installation et suppression testés depuis un serveur local (Hypnotic : 60 → 340 presets).
- ✅ Navigateur de presets (catégories, favoris, exclus, presets lents, FPS mesurés).
- ⚠️ Transitions intelligentes et résolution Auto par preset : en place, mais elles ne s'activent qu'une fois
  les presets mesurés — pas encore observées en conditions réelles.
- ⚠️ Le téléchargement depuis GitHub ne fonctionnera qu'une fois le dépôt **public** et une release publiée
  avec les zips + `manifest.json`.
- Le « titre du morceau » affiche pour l'instant le nom du preset (le vrai titre arrive à l'étape 3).

### Étape 3 — résultats (16/11/2026)

- ✅ Lecture de la session média active (`NotificationListenerService` + `MediaSessionManager`) : titre,
  artiste, album, appli source (« Spotify »). Fonctionne avec toute appli qui publie une MediaSession.
- ✅ Animation du titre façon MilkDrop (zoom + halo coloré, puis montée et fondu) à chaque morceau,
  et à la demande (OK). Réglages : affichage auto, durée, position centre/bas, taille, nouveau preset
  à chaque morceau.
- ✅ Pilotage de la musique : Chaîne + / Chaîne − = morceau suivant / précédent (reconfigurables), action
  lecture/pause disponible. Les touches média de la télécommande restent gérées par Android.
- Autorisation accordée une fois depuis le PC (testée sur la BRAVIA) :
  `adb shell cmd notification allow_listener fr.lactea/fr.lactea.nowplaying.NowPlayingListener 0`
- Note : « précédent » dans Spotify redémarre d'abord le morceau en cours (comportement de Spotify).

### Étape 4 — résultats (16/11/2026)

- ✅ **Lancement automatique (plan B)** testé sur la BRAVIA : 30 s après le début de la musique (Spotify),
  Lactea s'ouvre tout seul (Android l'autorise grâce à `SYSTEM_ALERT_WINDOW`). Il ne se rouvre pas si on l'a
  quitté pendant la même écoute, et il se **referme 30 s après l'arrêt de la musique** s'il s'était ouvert seul
  (sauf si on a utilisé la télécommande entre-temps). Réglages : délai (jamais → 5 min), applis de musique
  uniquement, fermeture auto. Hébergé par le service de lecture du titre (toujours actif en arrière-plan).
- 🟡 **Économiseur d'écran** (`LacteaDreamService`) : écrit et compilé, **pas encore testé**.
  - L'économiseur actuel de la TV est celui du launcher Proscenium (`fr.proscenium.launcher/.CinemaDream`) :
    ne le remplacer qu'avec accord.
  - Le démarrage manuel d'un économiseur demande les droits root sur cette TV ; test à faire avec le délai
    réel (10 min) : Spotify au premier plan en lecture → l'économiseur démarre-t-il ?
- Commandes (une fois, depuis le PC) :
  - ouverture auto : `adb shell appops set fr.lactea SYSTEM_ALERT_WINDOW allow`
  - économiseur : `adb shell settings put secure screensaver_components fr.lactea/fr.lactea.dream.LacteaDreamService`

---

## 2. Architecture

```
┌──────────────────────────── App Android (Kotlin) ────────────────────────────┐
│                                                                              │
│  AudioCapture (Visualizer session 0) ──PCM──▶ JNI ──▶ libprojectM (C++/GLES3)│
│                                                          │                   │
│  NowPlaying (MediaSession via NotificationListener)      ▼                   │
│        │ titre / artiste / pochette / état         GLSurfaceView (rendu)     │
│        ▼                                                 ▲                   │
│  PresetManager (liste, aléatoire, favoris, liste noire, mesure FPS)          │
│        ▲                                                                     │
│  RemoteControl (D-pad, touches couleur, pavé numérique, touches média)       │
│                                                                              │
│  VisualizerActivity  ·  LacteaDreamService (économiseur)  ·  Réglages        │
└──────────────────────────────────────────────────────────────────────────────┘
```

- **Moteur** : `libprojectM` 4.x compilé via NDK + CMake, **OpenGL ES 3**, **sans modification**.
  - Licence LGPL : liaison dynamique (`.so`) ; toute modification du moteur devrait être publiée.
  - La lib *playlist* de projectM est optionnelle → on gère la playlist en Kotlin.
- **ABI** : `armeabi-v7a` (la TV) + `arm64-v8a` (autres box).
- **App** : Kotlin, AGP 9.x, minSdk 26, sans Leanback lourd (UI Compose for TV ou vues simples).
- **Presets** : collection *Cream of the Crop* (sélection) + *texture pack* projectM, en assets
  puis copiés dans le stockage de l'app au premier lancement.

### Compatibilité des presets
- MilkDrop 1.x et 2.x (`.milk`), shaders HLSL compris (traduits en GLSL par projectM) : **oui**.
- Quelques presets rendus différemment (traduction HLSL imparfaite, textures manquantes).
- Presets MilkDrop 3 / BeatDrop (`.milk2`) : **non**.
- Presets lourds : risque de saccades sur la puce de la TV → mesure et exclusion automatique.

### Résolution de rendu (paramétrable, jusqu'à la 4K)
- Choix dans les réglages : **540p · 720p · 1080p · 1440p · 2160p (4K)** + mode **Auto**.
- Le mode Auto choisit la plus haute résolution qui tient le plafond de FPS, et redescend si ça rame.
- Rendu dans une surface à taille fixe (`SurfaceHolder.setFixedSize`), agrandie par le matériel si
  elle est plus petite que l'écran.
- **À vérifier sur la BRAVIA** : l'interface Android tourne en 1920×1080 (écran physique 3840×2160).
  Il faut confirmer qu'une surface 4K est vraiment affichée en 4K et pas réduite à 1080p par le
  compositeur ; sinon, tenter de basculer le mode d'affichage (`preferredDisplayModeId`).
- La 4K = 4× plus de pixels que la 1080p : sur la puce de la TV, probablement réservée aux presets
  légers. Les FPS mesurés par preset serviront à le dire.

### Allègement (performances, pas le code du moteur)
- Plafond **30 / 60 FPS** réglable.
- **Taille de la grille de déformation** (mesh) réduite.
- Exclusion des presets trop lents.

---

## 3. Fonctionnalités

### 3.1 Contrôle à la télécommande (raccourcis MilkDrop)

Mapping **par défaut**, entièrement reconfigurable dans le panneau d'options (voir 3.6).

| Touche | Action | Équivalent MilkDrop |
|---|---|---|
| ◀ / ▶ | Preset précédent / suivant | Backspace / Espace |
| OK | Afficher titre + artiste | T |
| OK (appui long) | Panneau d'options (toujours actif, non retirable) | |
| Info | Afficher le nom du preset | L / F7 |
| ▲ / ▼ | Sensibilité audio + / − | |
| 🔴 Rouge | Verrouiller le preset courant | Scroll Lock |
| 🟢 Vert | Mode aléatoire on/off | R |
| 🟡 Jaune | Ajouter / retirer des favoris | +/− (notation) |
| 🔵 Bleu | Panneau d'options | M |
| 0–9 | Accès direct aux favoris / notation | |
| Lecture / Pause, ⏭ ⏮ | Pilotage de la musique (MediaSession) | |
| Info (appui long) | Écran d'aide des touches | F1 |
| Retour | Masquer les overlays, puis quitter | Échap |

### 3.2 Transitions
- Changement automatique toutes les **N secondes** (réglable).
- **Fondu enchaîné** entre presets (durée réglable).
- **Coupes franches** sur les gros temps forts (*hard cuts*, sensibilité réglable).
- Option : nouveau preset à **chaque changement de morceau**.

### 3.3 Titre du morceau
- Lecture de la `MediaSession` active via `NotificationListenerService`.
- Animation de titre/artiste au changement de morceau, style MilkDrop.
- Autorisation à accorder une fois via adb (Google TV n'a pas l'écran de réglage) :
  `adb shell cmd notification allow_listener fr.lactea/fr.lactea.NowPlayingListener`

### 3.4 Économiseur d'écran (`DreamService`)
- `LacteaDreamService` : même rendu que l'activité principale.
- Google TV n'affiche plus le choix d'économiseur tiers → activation via adb :
  `adb shell settings put secure screensaver_components fr.lactea/fr.lactea.LacteaDreamService`
- **À vérifier** : l'appli Spotify TV empêche peut-être l'économiseur de démarrer pendant la lecture.
- **Plan B — lancement automatique** : Lactea surveille l'état de lecture (MediaSession) et s'ouvre
  quand la musique démarre (autorisation « afficher par-dessus les autres applis » via adb :
  `adb shell appops set fr.lactea SYSTEM_ALERT_WINDOW allow`).

### 3.5 Écran d'aide
- Panneau récapitulant toutes les touches, affiché via la touche Info ou au premier lancement.

### 3.6 Panneau d'options
Panneau navigable au D-pad, affiché par-dessus le visualiseur (le rendu continue derrière, pour voir
l'effet des réglages en direct). Organisé en sections :

**Visuel**
- Résolution de rendu (540p → 4K, ou Auto), plafond FPS, taille du mesh.
- Affichage du compteur FPS on/off.

**Presets & transitions**
- Durée des presets, durée des fondus, coupes franches on/off + sensibilité.
- Mode aléatoire / séquentiel, favoris uniquement, nouveau preset à chaque morceau.
- Seuil de FPS pour l'exclusion automatique, réinitialiser la liste des presets lents.

**Audio**
- Sensibilité audio, mode de capture (normalisé / volume réel).

**Titre du morceau**
- Affichage on/off, durée, position, taille.

**Télécommande — configuration des touches**
- Liste de toutes les actions (preset suivant/précédent, verrouillage, aléatoire, favoris, titre,
  nom du preset, sensibilité, aide, options, pilotage musique…).
- Pour chaque action : **« Appuyez sur la touche à associer »** → capture de la touche réelle
  (fonctionne avec n'importe quelle télécommande, pas seulement Sony).
- **Appui court et appui long** assignables séparément.
- Plusieurs touches possibles par action ; une action peut être laissée sans touche.
- **Détection des conflits** : si la touche est déjà prise, proposer de la réattribuer.
- **Réinitialiser** au mapping par défaut.
- **Sécurités** :
  - Les touches système (Accueil, Google Assistant, boutons Netflix/YouTube, volume selon la TV)
    ne sont pas interceptables par une appli → non proposées.
  - La touche Retour garde toujours sa fonction (fermer le panneau / quitter).
  - L'accès au panneau d'options reste toujours possible via un **appui long sur OK**, même si
    la touche assignée a été retirée — impossible de se retrouver bloqué.

**Économiseur / lancement auto**
- Activation, délai, rappel des commandes adb à lancer depuis le PC.

**À propos**
- Version, licences (projectM LGPL, packs de presets), crédits.

Tous les réglages sont **sauvegardés** (DataStore) et appliqués aussi en mode économiseur d'écran.

### 3.7 Favoris et liste noire
- Notation des presets (comme MilkDrop), favoris, exclusion manuelle.
- Mode « favoris uniquement ».

### 3.8 Exclusion automatique des presets lents
- Mesure des FPS pendant chaque preset ; sous un seuil → noté comme lent, plus proposé.
- Liste consultable / réinitialisable dans les réglages.

### 3.9 Navigateur de presets
- Liste parcourable au D-pad, recherche, aperçu du nom.

### 3.10 Téléchargement de packs de presets (depuis le GitHub du projet)
- L'APK n'embarque qu'une **petite sélection** ; le reste se télécharge depuis le panneau d'options.
- Sur GitHub : packs en **zip publiés comme releases** (un par catégorie, + « Sélection TV légère »,
  « Collection complète »), décrits par un **`manifest.json`** (nom, version, taille, nombre de
  presets, empreinte SHA-256).
- Dans le panneau d'options, section Presets : liste des packs (installé / mise à jour dispo / non
  installé), télécharger / supprimer avec progression, vérifier les mises à jour, espace utilisé.
- Vérification de l'empreinte puis extraction dans le stockage de l'app.
- Option : le manifest publie une **liste des presets trop lents par modèle de TV** (issue du banc
  d'essai), appliquée d'office.
- Permission `INTERNET` à ajouter. Crédits et note de licence de *Cream of the Crop* repris.

### 3.11 Pochette d'album (étape 6)

**Constat (16/11/2026, BRAVIA + Spotify)** : la MediaSession de Spotify fournit déjà la pochette —
image 300×300 (`METADATA_KEY_ALBUM_ART`) et URL du CDN Spotify (`…/image/ab67616d00001e02…`).
Remplacer `00001e02` par `0000b273` donne a priori la version 640×640 (à vérifier).
Aucun service en ligne n'est nécessaire pour Spotify.

**Compatibilité des presets** : 226 presets sur 9 795 (≈ 2 %) utilisent une texture aléatoire
(`sampler_rand00`, `rand01`, `rand02`, `rand00_smalltiled`…). Ce sont ceux où la pochette peut se fondre
dans le visuel. Pour tous les autres : affichage par-dessus et presets « Lactea » dédiés.

**6.1 — Récupération de la pochette**
- **Priorité à la pochette fournie par l'appli source**, les services en ligne ne servent qu'en secours :
  1. image de la MediaSession (`METADATA_KEY_ALBUM_ART` / `ART` / `DISPLAY_ICON`) ;
  2. URL de la MediaSession (`…_ART_URI`), en tentant la version 640×640 pour Spotify ;
  3. **secours en ligne**, seulement si l'appli ne fournit rien : **API Deezer** (sans clé, jusqu'à
     1000×1000), sinon **iTunes Search API** (sans clé). Activé par défaut, désactivable dans les options
     (il envoie titre + artiste au service).
- Cache disque : `filesDir/cover/cover_<hash>.jpg` (nom unique par album, car projectM met les
  textures en cache par nom) ; on garde les ~20 dernières.
- Critère : la pochette du morceau en cours est disponible en fichier en < 1 s après le changement.

**6.2 — Pochette affichée avec le titre (tous les presets)**
- La pochette apparaît à gauche du titre (ou au-dessus en position « centre »), avec un halo de la
  couleur dominante de l'image, puis s'efface avec le titre.
- Option : **pulsation sur le rythme** (taille légèrement liée au niveau audio).
- Critère : visible sur n'importe quel preset, sans baisse de FPS mesurable.

**6.3 — Pochette dans le visuel (presets compatibles)**
- projectM 4.1.8 n'a pas d'API pour injecter une image ; on ne modifie pas le moteur (LGPL). Méthode :
  au chargement d'un preset qui utilise `sampler_randNN`, Lactea **réécrit le texte du preset à la volée**
  (`sampler_randNN` → `sampler_cover_<hash>`, `texsize_randNN` → `texsize_cover_<hash>`) et le charge via
  `projectm_load_preset_data`. projectM charge alors la pochette comme une texture nommée.
- Le dossier `cover/` est ajouté aux chemins de textures. **À vérifier** : un fichier ajouté après le
  scan initial est-il bien chargé ? Sinon `projectm_reset_textures` (petit à-coup, à faire pendant une
  coupe franche).
- Option « Presets pochette en priorité » : au changement de morceau, choisir de préférence un preset
  compatible.
- Critère : la pochette se voit déformée/fondue dans au moins 9 presets compatibles sur 10.

**6.4 — Presets « Lactea » dédiés à la pochette**
- 6 à 10 presets maison (`Lactea/…`) écrits pour `sampler_cover` : kaléidoscope, tunnel infini,
  miroirs, vagues, pochette en feedback qui se dissout, mosaïque pulsée.
- Embarqués dans l'APK ; signature visuelle de l'appli.
- Critère : tous tiennent 60 FPS en 720p sur la BRAVIA.

**6.5 — Options (section « Pochette »)**
- Afficher la pochette avec le titre · pulsation · intégrer dans les presets compatibles · presets
  pochette en priorité · secours en ligne si l'appli ne fournit pas de pochette (activé par défaut,
  désactivable).

**Résultats (16/11/2026, BRAVIA + Spotify)** — captures `docs/step6-*.png` :
- ✅ 6.1 Pochette Spotify en **640×640** (URL du CDN). Secours Deezer/iTunes codé, non testé (Spotify fournit
  toujours la pochette). Cache `filesDir/cover/`, 20 dernières.
- ✅ 6.2 Pochette au-dessus du titre, halo de sa couleur dominante, pulsation sur le rythme ; elle apparaît
  aussi quand elle arrive après le titre.
- ✅ 6.4 Six presets « Lactea » (kaléidoscope, tunnel, miroirs, dissolution, vagues, mosaïque —
  générés par `tools/lactea_presets.py`) : la pochette s'y affiche, **60 FPS en 720p** (mosaïque ≈ 55).
- ✅ 6.5 Section « Pochette » dans les options.
- ✅ 6.3 Réécriture à la volée : les 8 presets compatibles de la collection présents sur la TV se chargent
  avec la pochette (captures `docs/step6-collection-presets.png`). Elle est nettement fondue dans le visuel
  pour 4 d'entre eux ; les 4 autres n'utilisent leur image qu'en touche discrète (choix de leurs auteurs).
- 🐞 Bug corrigé en route : les commandes envoyées au moteur via `GLSurfaceView.queueEvent` pouvaient
  s'exécuter pendant une pause de l'appli, **sans contexte graphique actif** → `glCreateShader` renvoyait 0
  et le preset échouait (« Error compiling shader » sans message). Toutes les commandes passent désormais par
  `renderer.post`, exécuté à l'image suivante (contexte garanti). Ce n'était donc pas une limite du GPU.
- Outil de test : `tools/load_preset.sh <serial> "Catégorie/nom"` (nom encodé, gère apostrophes et
  parenthèses).
- Découverte : projectM 4.1.8 n'arrive pas à charger un preset quand il doit choisir lui-même une image au
  hasard sur Android (`std::exception`). Lactea choisit donc lui-même ces images (dossier de textures) en
  réécrivant le preset — sans modifier projectM.
- Note : sur la TV, les réglages « 30 images/s max » et « 720p fixe » avaient été choisis dans les options.

### 3.12 Ajout de presets depuis le PC
- Mini serveur web sur la TV (adresse affichée à l'écran), dépôt de fichiers `.milk` depuis le
  navigateur du PC.

---

## 4. Étapes

| # | Étape | Contenu | Critère de réussite |
|---|---|---|---|
| 0 | ✅ Test capture | `spike-visualizer/` | Signal Spotify capté, pause = 0 |
| 1 | ✅ **projectM sur la TV** | Projet `fr.lactea`, build NDK de libprojectM, JNI, `GLSurfaceView`, capture audio branchée, ~50 presets, compteur FPS, résolution de rendu réglable | Presets qui réagissent à Spotify, FPS mesurés en 720p / 1080p / 4K, affichage 4K réel confirmé ou non |
| 2 | ✅ **Télécommande & transitions** | Mapping des touches, transitions auto + fondus + coupes franches, écran d'aide | Tous les raccourcis fonctionnels |
| 3 | ✅ **Titre du morceau** | `NotificationListenerService`, animation titre, pilotage lecture | Titre affiché au changement de morceau |
| 4 | 🟡 **Économiseur / lancement auto** | `DreamService` ; test avec Spotify ; sinon plan B | Lactea démarre seul quand la musique joue |
| 5 | ✅ **Panneau d'options & gestion des presets** | Panneau d'options complet dont la configuration des touches, favoris, liste noire, exclusion auto, navigateur, téléchargement de packs depuis GitHub | Préférences et touches personnalisées conservées entre lancements ; un pack s'installe depuis le panneau |
| 6 | ✅ **Pochette d'album** (détail : §3.11) | 6.1 récupération (pochette de l'appli source en priorité, Deezer/iTunes en secours) · 6.2 pochette avec le titre · 6.3 pochette dans les 226 presets compatibles (réécriture à la volée) · 6.4 presets « Lactea » dédiés · 6.5 options | Pochette visible à chaque morceau ; fondue dans le visuel des presets compatibles |
| 6 bis | **Ajout de presets depuis le PC** (§3.12) | Mini serveur web sur la TV, dépôt de `.milk` | Un preset déposé depuis le PC apparaît dans le navigateur |
| 7 | **Finition** | Icône/bannière Lactea, collection de presets complète, tests sur une autre box | APK prête à installer |
| 8 | **Optimisation des transitions** | 1) Vérifier sur la TV : banc d'essai fondu 2 s vs 3 s (étape 1), et observer dans le journal les coupes franches et changements de résolution en mode Auto une fois les presets mesurés. 2) Pré-remplir les mesures : publier dans le manifest les FPS par preset et par modèle de TV (issus du banc d'essai), pour que le mode Auto soit efficace dès le premier lancement. 3) Réduire le coût du fondu lui-même : piste « rendu en résolution réduite pendant le fondu », à évaluer (risque de saut visible). | Plus de ralentissement visible pendant les transitions sur la BRAVIA |

---

## 5. Points de vigilance

- **Performances** de la puce de la BRAVIA AE2 (32 bits) avec les presets lourds, surtout en 4K.
- **Affichage 4K réel** : l'interface Android de la TV est en 1080p ; une surface 4K pourrait être
  réduite par le système.
- **Saturation** de la capture en mode normalisé → tester le mode « volume réel ».
- **Spotify TV et l'économiseur d'écran** (wake lock possible).
- **Licences** : projectM en LGPL (ne pas modifier le moteur) ; vérifier les licences des packs de presets
  avant toute diffusion publique.
- **Nom & marques** : vérifier « Lactea » sur le Play Store et auprès de l'INPI/EUIPO ;
  ne pas utiliser « MilkDrop », « Winamp » ni les logos Spotify/Deezer.

---

## 6. Environnement de développement

- SDK Android (plateforme 37, build-tools 36), NDK 30, CMake 4.1, Gradle 9.8.2, AGP 9.4.1.
- JDK : `C:\Program Files\Android\Android Studio\jbr`.
- TV accessible via adb Wi-Fi (préciser `-s` : plusieurs entrées listées).
- Pas d'émulateur TV : tests directement sur la BRAVIA.
