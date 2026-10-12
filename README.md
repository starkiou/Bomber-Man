# Bomberman

Jeu Bomberman multijoueur en temps réel, développé en Java 25 / JavaFX 21. Projet d'équipe de 2e année de BUT Informatique (SAÉ S4, IUT de Caen).

## Présentation

Clone personnalisé de Bomberman jouable en solo contre des IA ou en ligne contre d'autres joueurs via une connexion TCP. Le projet est structuré en trois modules Maven et intègre plusieurs design patterns (Singleton, Factory, Strategy, Observer, Chaîne de responsabilité).

## Prérequis

- Java 25+
- Maven n'a pas besoin d'être installé : le wrapper `mvnw` est fourni
- JavaFX 21.0.6 (fourni via les dépendances Maven)

## Architecture

```
sae_s4_groupe_b/
├── Common/     # Modèle partagé : entités, IA, labyrinthe, réseau, logger
├── ServerSide/ # Serveur TCP : gestion des rooms, boucle de jeu, synchronisation
└── ClientSide/ # Interface JavaFX : vues FXML, réseau client, rendu sprites
```

---

## Compilation et lancement

### Tout compiler depuis la racine

Java 25 est requis. Maven n'a pas besoin d'être installé : le projet embarque son wrapper (`mvnw`, ou `mvnw.cmd` sous Windows).

```bash
./mvnw install -DskipTests
```

### Lancer le serveur

Le serveur écoute sur le **port 3000** par défaut.

```bash
./mvnw -f ServerSide/pom.xml exec:java -Dexec.mainClass=org.example.Main
```

### Lancer le client

```bash
./mvnw -f ClientSide/pom.xml javafx:run
```

`F11` bascule le plein écran.

---

## Contrôles du jeu

| Action | Touche |
| :--- | :--- |
| **Se déplacer** | Flèches directionnelles ($\uparrow, \downarrow, \leftarrow, \rightarrow$) |
| **Poser une bombe** | Barre d'espace (`Espace`) |

---

## Modes de jeu

### Mode solo (hors ligne)

Aucun serveur requis.

1. Lancer le client → **Play** → **Play Offline**
2. Configurer la partie : taille de la carte, nombre de bombes, durée limite, difficulté des bots
3. La partie se joue localement avec des bots IA

### Mode en ligne

1. Démarrer le serveur
2. Lancer le client → **Play** → renseigner IP, port (3000), pseudo
3. **Play Online** → liste des rooms disponibles
4. Créer ou rejoindre une room, indiquer "Prêt", attendre le countdown de 10s
5. La partie se lance automatiquement

---

## Fonctionnalités implémentées

### Gameplay
- **Mouvements** : Déplacement dans 4 directions avec gestion des collisions (murs et bombes).
- **Bombes** : Placement avec la touche Espace. Stock limité, rechargement automatique toutes les 5 s.
- **Explosions** : Radiales (rayon : 2 cases, délai : 3 s). Arrêtées par les murs, détruisent les briques.
- **Santé** : Système de HP (1 HP par défaut). Mort immédiate au contact d'une explosion.
- **Victoire** : Mode "Battle Royale", le dernier survivant l'emporte.

### Intelligence artificielle
Trois stratégies basées sur un algorithme BFS pour le pathfinding :

| Stratégie | Comportement |
|-----------|-------------|
| **Aggressive** | Chasse le joueur proche, pose des bombes si aligné, fuit les explosions. |
| **Survivalist** | Fuit les ennemis au début, devient agressif en 1v1. |
| **Tactical** | Privilégie la destruction de briques et vérifie toujours son chemin d'évasion. |

### Labyrinthes procéduraux
- **Exhaustive** : Backtracking récursif couplé à un remplissage aléatoire (60% briques).
- **Random Fusion** : Utilisation de l'algorithme Union-Find pour fusionner des chemins.

### Réseau
- Communication TCP binaire : `[1 byte type][4 bytes length][JSON UTF-8]`.
- Serveur faisant autorité : la partie est simulée sur le serveur à 60 ticks par seconde, bots compris. Les clients envoient leurs actions et affichent l'état qu'ils reçoivent, 20 fois par seconde.
- Un client ne commande que son propre personnage, et pas plus vite qu'une case toutes les 120 ms.
- 20 types de messages.
- Gestion des salons (rooms) : 4 personnages au plus, bots compris ; seul l'hôte lance la partie.
- Trois tailles de carte au choix à la création d'une room, et un chat dans la salle d'attente.
- Un joueur qui se déconnecte en pleine partie est éliminé, la partie continue pour les autres.

---

## Design patterns utilisés

| Pattern | Utilisation |
|---------|---------|
| **Singleton** | Gestionnaires uniques : `ServerManager`, `NetworkManager`, `SceneManager`. |
| **Factory** | Instanciation dynamique : `MazeFactory`, `AIFactory`, `MessageFactory`. |
| **Strategy** | Comportements interchangeables des IA (`AIPlayer`). |
| **Observer** | Diffusion des snapshots du jeu aux clients via `GameStateListener`. |
| **Chaîne de responsabilité** | Système de logs hiérarchique (`ConsoleLogger` → `FileLogger`). |

---

## Paramètres techniques

- **Grille par défaut** : 15 × 11 cases (40 px / case).
- **Capacité** : Jusqu'à 4 joueurs par room.
- **Personnalisation** : 35 skins de personnages disponibles.
- **Animations** : Sprites directionnels (3 frames), bombes (28 frames), explosions (14 frames).

---

## Équipe

Groupe B — SAÉ S4 — IUT Informatique de Caen. Le jeu a été écrit en équipe en quatre jours, du 29 mars au 2 avril 2026.

## Ma contribution

Ce dépôt est ma copie personnelle du projet.

**Pendant le projet**, je me suis occupé des entités du jeu (joueur, bombes, collisions, explosions) et de tout le module d'IA : les trois stratégies et leur recherche de chemin par parcours en largeur.

**Après le rendu**, j'ai repris le projet pour l'auditer et le corriger :

- **Juin 2026** : correction de bugs de réseau et de concurrence, factorisation du BFS des bots, suppression de messages dupliqués, remise en état des tests.
- **Octobre 2026** : la partie est désormais simulée par le serveur. Avant, il se contentait de relayer les touches et chaque client simulait sa propre partie, si bien que les écrans divergeaient.
- Corrections de jeu : bots qui contournent les bombes et fuient les leurs, vitesse de déplacement constante, sprites mal associés, écran de fin, clavier inopérant en ligne.
- Salons : hôte seul à lancer la partie, quatre personnages au plus, taille de carte, durée, nombre de bombes et chat.
- Tests ajoutés sur l'IA, le décompte des kills et la sérialisation de l'état de jeu.