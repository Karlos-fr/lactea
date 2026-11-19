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

<p align="center"><img src="../images/screenshots.jpg" alt="Lactea sur une TV" width="760" /></p>

# Lactea — visualiseur musical pour Android TV

Lactea transforme la musique jouée sur votre TV en images psychédéliques, comme le célèbre plugin
**MilkDrop** de Winamp. Il réagit à **toute appli qui joue du son** : Spotify, Deezer, YouTube
Music, Tidal, Plex… Vous continuez à utiliser votre appli de musique comme d'habitude ; Lactea écoute
ce que joue la TV et dessine.

Il s'appuie sur [projectM](https://github.com/projectM-visualizer/projectm), la réécriture libre de
MilkDrop, et joue les presets MilkDrop d'origine : près de **10 000 visualisations** créées par la
communauté MilkDrop en vingt ans.

Version actuelle : **1.0.0** — [télécharger l'APK](https://github.com/Karlos-fr/lactea/releases/latest) · [nouveautés](../CHANGELOG.md) (en anglais).

## Fonctionnalités

- **Réagit à toute la musique jouée sur la TV**, quelle que soit l'appli, y compris quand le son
  part vers une barre de son en HDMI (ARC / eARC).
- **9 803 presets** en 11 packs à choisir et télécharger depuis l'appli ; aucun n'est intégré.
  Tous les presets de la collection se chargent, grâce aux corrections apportées à projectM (voir
  plus bas).
- **Titre et pochette d'album** à l'écran à chaque changement de morceau ; la pochette peut aussi
  apparaître dans certaines visualisations.
- **S'ouvre tout seul** quand la musique démarre, et se ferme quand elle s'arrête (au choix).
- **Pensé pour la télécommande** : chaque action a sa touche, et chaque touche se change.
- **Jusqu'en 4K**, avec un mode Auto qui choisit la meilleure résolution pour chaque preset.
- **Fluide même sur une TV modeste** : Lactea sait quels presets sont gourmands, peut écarter les plus
  lents, et choisit entre fondu et coupe pour que les transitions ne saccadent pas.
- **Favoris et exclusions**, navigateur de presets, mode aléatoire et verrouillage du preset en cours.
- **Six langues** : français, anglais, allemand, italien, espagnol et suédois, selon la langue de la
  TV (anglais pour toute autre langue), ou choisie dans **Options › Langue**.
- **Doux pour les yeux** : chaque preset est étiqueté ⚡ flash, 🌙 chill ou OK, et a un aperçu ;
  ne jouez que les calmes, ou aucun qui flashe.
- **Télécommande web** : scannez un QR code avec votre téléphone, ou ouvrez la page sur un
  ordinateur, pour parcourir les presets, faire des playlists et changer toutes les options.

## Configuration requise

- Une TV ou un boîtier sous **Android TV ou Google TV**, Android 8.0 ou plus, avec OpenGL ES 3.0.
- Testé sur une **Sony BRAVIA** (Google TV, puce graphique Mali-G57).
- Environ 8 Mo pour l'appli, et environ 120 Mo à télécharger pour tous les packs de presets.

## Installation

1. Téléchargez `Lactea-….apk` depuis la page [Releases](https://github.com/Karlos-fr/lactea/releases).
2. Installez-le sur la TV, par exemple avec une appli de gestion de fichiers depuis une clé USB, ou
   depuis un ordinateur : `adb install Lactea-….apk`.
3. Lancez **Lactea** depuis la liste des applis de la TV.
4. Lactea demande l'**autorisation du micro** : acceptez-la. Android l'exige pour qu'une appli puisse
   suivre le son joué par la TV. Lactea n'utilise aucun micro et n'enregistre rien : il lit seulement
   le niveau du son, plusieurs fois par seconde, pour animer les images.

Lancez ensuite de la musique dans n'importe quelle appli et revenez dans Lactea.

### Autorisations facultatives

Certaines fonctions demandent des autorisations qu'Android TV ne propose pas à l'écran. Donnez-les
depuis les paramètres de la TV si elle les propose, ou une fois depuis un ordinateur avec
[adb](https://developer.android.com/tools/adb) (options pour les développeurs et débogage activés
sur la TV) :

| Fonction | Autorisation | Depuis un ordinateur |
|---|---|---|
| Titre et pochette du morceau | Accès aux notifications | `adb shell cmd notification allow_listener fr.lactea/fr.lactea.nowplaying.NowPlayingListener` |
| Ouverture automatique quand la musique joue | Superposition aux autres applis | `adb shell appops set fr.lactea SYSTEM_ALERT_WINDOW allow` |

## Télécommande

| Touche | Action |
|---|---|
| ◀ ▶ | Preset précédent / suivant |
| ▲ ▼ | Sensibilité audio + / − |
| OK | Afficher le titre du morceau |
| **Appui long sur OK** | **Options** (toujours, non modifiable) |
| 🔴 Rouge | Verrouiller le preset en cours |
| 🟢 Vert | Mode aléatoire |
| 🟡 Jaune | Ajouter aux favoris / retirer — appui long : exclure le preset |
| 🔵 Bleu ou Menu | Options — appui long sur Bleu : navigateur de presets |
| Info | Nom du preset — appui long : liste des touches |
| Chaîne + / − | Morceau suivant / précédent |
| Lecture / Pause, ⏭ ⏮ | Transmis à votre appli de musique |
| 0 | Compteur d'images par seconde |
| Retour | Fermer un panneau, puis quitter Lactea |

Chaque touche se change dans **Options › Télécommande**, avec un appui court ou long.

## Télécommande web

Tant que Lactea est à l'écran, il sert une page sur votre réseau local. Dans **Options ›
Télécommande › Afficher le QR code**, scannez le code avec un téléphone, ou tapez l'adresse affichée
dessous dans le navigateur d'un ordinateur (même réseau). La page a l'allure des panneaux de Lactea
et suit la TV en direct :

- **En cours** : l'aperçu du preset, la chanson, précédent / suivant, favori, verrou, ambiance.
- **Presets** : tous les presets installés avec leur aperçu et une recherche ; ouvrez-en un pour le
  jouer, le marquer, l'étiqueter ou l'ajouter à des playlists.
- **Ambiances** : les ambiances et les playlists (réordonner les presets d'une playlist).
- **Options** : toutes les options de la TV, packs compris.

Sur un ordinateur, Échap revient en arrière et ← / → changent de preset. L'adresse reste **http://lactea.local:8771/** (ou l'adresse IP de la TV,
donnée par le QR code, pour les téléphones qui ne connaissent pas les noms .local). Le même menu
règle le nom sur le réseau, un mot de passe (demandé par le navigateur ; il passe en clair sur le
réseau local), une adresse aléatoire à chaque lancement à la place, et désactive la télécommande web.

## Options

Faites un appui long sur **OK** pour ouvrir les options. Elles sont regroupées par thème :

- **Visuel** : résolution de rendu (de 540p à 4K, ou Auto), résolution maximale en mode Auto, images
  par seconde (30 ou 60), finesse de la déformation, compteur d'images par seconde.
- **Presets et transitions** : quand changer de preset (à chaque morceau seulement, ou aussi
  toutes les 10 s à 5 min), ambiances (quels presets sont joués : catégories, FPS minimum), playlists (presets choisis à la main, dans l’ordre ou au hasard), fondu
  ou coupe (Auto choisit pour chaque preset), coupes sur le rythme, mode aléatoire, favoris
  seulement, nom du preset au changement.
- **Titre du morceau** : affiché au changement de morceau, durée, position, taille.
- **Pochette** : avec le titre, pulsation au rythme, dans les visualisations, recherche en ligne
  quand l'appli de musique ne fournit pas de pochette.
- **Lancement automatique** : ouverture quand la musique joue (après combien de
  temps, applis de musique seulement), fermeture quand la musique s'arrête.
- **Gestion des presets** : navigateur, exclure les presets lents, packs à télécharger, test des
  presets sur cette TV (voir plus bas), remise à zéro des mesures, liste des exclus.
- **Audio** : sensibilité, mode de capture (niveau constant, ou qui suit le volume).
- **Télécommande** : changer les touches.

## Packs de presets

L'APK ne contient aucun preset : au premier lancement, Lactea propose les packs à télécharger.
**Essentials** (60 presets : le meilleur de la collection) et **Lactea** (les presets propres à Lactea)
sont recommandés ; le reste de la collection est réparti en 10 packs thématiques (Dancer, Drawing,
Fractal, Geometric, Hypnotic, Particles, Reaction, Sparkle, Supernova, Waveform). Les images
utilisées par les presets forment un pack **Textures**, installé automatiquement avec le premier
pack. Ajoutez ou retirez des packs quand vous voulez dans
**Options › Gestion des presets › Packs de presets à télécharger**.

Le pack **Lactea** contient les presets faits pour Lactea : deux analyseurs de spectre (celui de
Winamp, et un néon), deux voyages dans un trou de ver, un lagon ensoleillé sous l'eau et le verre
(Eau), un feu d'artifice (Feux d'artifice), et six presets construits autour de la pochette.

Chaque preset des packs a été rendu et analysé sur un PC : il est étiqueté ⚡ flash, 🌙 chill ou OK,
et le navigateur de presets montre son aperçu (4 moments). Les ambiances peuvent écarter les presets
qui flashent (**Sans flash**) ou ne garder que les calmes (**Chill seulement**). Dans le navigateur,
🟢 change l'étiquette d'un preset, 🔵 l'ajoute à la playlist en cours, et le preset sélectionné joue
derrière le navigateur au bout d'une seconde.

## Presets lents

Certains presets demandent beaucoup à la puce graphique. Sur la Sony BRAVIA de test, en 720p :

| | Presets | Part |
|---|---|---|
| Fluides (55 images par seconde ou plus) | 6 318 | 64 % |
| Corrects (45 à 55) | 736 | 7 % |
| Lourds (20 à 45) | 1 364 | 14 % |
| Trop lourds (moins de 20) | 1 439 | 15 % |

Lactea est livré avec ces mesures : l'option **Exclure les presets lents** fonctionne dès le premier
lancement. À mesure que les presets passent, Lactea les mesure de nouveau sur votre TV et utilise ses
propres chiffres.

Pour mesurer tous les presets sur votre TV d'un coup : **Options › Gestion des presets › Tester les
presets sur cette TV**. Choisissez tout ou un seul thème ; environ 3 secondes par preset (environ
8 heures pour toute la collection). Laissez la musique jouer ; n'importe quelle touche arrête le test,
qui pourra reprendre là où il s'est arrêté.

## Limites connues

- Quand le son part vers un casque ou une enceinte **Bluetooth**, Lactea peut ne pas le recevoir.
- Quelques presets perdent une partie de leur animation : 7 à cause d'erreurs que MilkDrop refusait
  lui-même, 7 à cause d'une [différence encore à corriger dans projectM](https://github.com/projectM-visualizer/projectm-eval/issues/23).

## Comment Lactea a été fait

- **projectM, corrigé.** Lactea utilise sa propre version de projectM 4.1.8 :
  [Karlos-fr/projectm](https://github.com/Karlos-fr/projectm). Le test de toute la collection sur une TV a
  montré que 67 d'entre eux ne se chargeaient pas du tout, parce que projectM lisait leurs équations
  autrement que MilkDrop. La correction suit le code source de MilkDrop et a été vérifiée sur les
  144 906 blocs d'équations de la collection, sans aucune régression. Elle a été proposée à l'équipe
  de projectM ([issue](https://github.com/projectM-visualizer/projectm/issues/1083),
  [pull request](https://github.com/projectM-visualizer/projectm/pull/1084)).
- **Mesuré sur la TV.** Chaque preset a été chargé et chronométré sur la TV, d'où les chiffres
  ci-dessus.
- **L'IA, en toute transparence.** Lactea a été écrit avec un agent de programmation IA,
  **Anthropic Claude** (Claude Code), et son logo a été dessiné avec **OpenAI Codex**. Un humain
  dirige le projet, teste sur la TV et décide.

Pour compiler Lactea vous-même : [BUILDING.md](BUILDING.md) (en anglais).

## Crédits et licences

- [projectM](https://github.com/projectM-visualizer/projectm) : LGPL 2.1. La version modifiée par
  Lactea est publiée sur [Karlos-fr/projectm](https://github.com/Karlos-fr/projectm).
- Presets : [Cream of the Crop](https://github.com/projectM-visualizer/presets-cream-of-the-crop),
  rassemblés et classés par ISOSCELES ; chaque preset appartient à ses auteurs, cités dans le nom du
  fichier.
- Textures : [MilkDrop texture pack](https://github.com/projectM-visualizer/presets-milkdrop-texture-pack).
- MilkDrop a été créé par Ryan Geiss.
