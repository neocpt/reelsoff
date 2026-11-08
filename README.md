# ReelsOff

Application Android open source qui coupe les formats courts :

- **Instagram** : quitte l'onglet Reels et revient à l'accueil. Les reels reçus en message privé restent visibles.
- **YouTube** : ferme le lecteur Shorts plein écran.
- **Snapchat** : quitte l'onglet Spotlight. *(non testé)*

## Installation

1. Compile l'APK avec Android Studio, ou lance `gradle assembleDebug` dans ce dossier.
2. Installe l'APK sur ton téléphone.
3. Ouvre ReelsOff et touche **Ouvrir les réglages d'accessibilité**.
4. Active le service **ReelsOff** dans *Applications téléchargées*.

## Fonctionnement

ReelsOff utilise le service d'accessibilité d'Android pour lire la structure de l'écran des trois apps. Elle n'enregistre rien, n'envoie rien sur internet et ne lit pas le texte de tes messages.

Elle n'agit que si la fenêtre active appartient à l'app surveillée, et au plus une fois toutes les 1,5 seconde.

## Limites

- Les identifiants de vues d'Instagram et de YouTube changent avec les mises à jour : la détection peut cesser de marcher.
- Le blocage du Spotlight Snapchat n'est pas testé.
- Une app installée hors d'un magasin d'apps peut être bloquée par Android pour le service d'accessibilité.

## Crédits

Les identifiants de vues utilisés (`clips_tab`, `feed_tab`, `reel_progress_bar`) s'inspirent du projet [Shorts-Blocker](https://github.com/atick-faisal/Shorts-Blocker) (Apache-2.0). Aucun code n'a été copié.

## Licence

MIT, voir [LICENSE](LICENSE).
