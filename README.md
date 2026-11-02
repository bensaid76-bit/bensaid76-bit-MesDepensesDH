# Mes Dépenses DH — Android natif

Application Android Kotlin, sans dépendances tierces, destinée notamment au Samsung Galaxy A24.

## Fonctionnalités
- Budget mensuel initial de 1 998 DH, modifiable
- Dépenses datées, catégories, commentaires, édition et suppression
- Alertes 50 %, 75 %, 90 % et 100 % ; aucun blocage de saisie
- Historique, statistiques par catégorie, export CSV compatible Excel
- Catégories personnalisées, thème sombre, stockage SQLite local, aucun accès Internet requis

## Compilation
Ouvrir ce dossier dans Android Studio avec Android SDK 35 et JDK 17.
Laisser Android Studio synchroniser Gradle, puis choisir Build > Build APK(s).
Le fichier généré sera normalement `app/build/outputs/apk/debug/app-debug.apk`.
Le SDK et Gradle n'étant pas disponibles dans l'environnement de création, aucun APK compilé n'est inclus.

## Installation
Transférer l'APK compilé sur le Samsung A24, l'ouvrir et autoriser l'installation depuis cette source si Android le demande. Installer uniquement l'APK que vous avez compilé ou obtenu d'une source de confiance.

## Remarques
Les données sont stockées localement dans la base privée de l'application. Un effacement des données de l'application peut les supprimer ; exporter régulièrement le CSV. L'application n'a pas été testée sur un appareil physique.
