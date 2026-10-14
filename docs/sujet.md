# SAÉ 4

*Java/Kotlin/JavaFX*
**Développement d'un jeu réseau en temps réel**

## 1 Introduction

L'objectif de cette (SAE) est de concevoir une version personnalisée du célèbre jeu Bomberman. Le projet doit impérativement fonctionner en temps réel et en réseau, en utilisant les technologies Java (ou Kotlin) ainsi que JavaFX. Il est tout à fait possible, au sein du même projet, d'avoir du code Java et Kotlin selon les préférences du développeur.

Une attention particulière sera portée à l'architecture logicielle : vous devrez intégrer de manière pertinente les **Design Patterns** étudiés lors du premier semestre.

Le principe du jeu est simple : vous (et les autres joueurs/IA présentLe.s) êtes un personnage se déplaçant dans un labyrinthe en deux dimensions avec la possibilité d'y placer des bombes. Ces bombes explosent après un certain laps de temps dans un rayon prédéfini. Vous en possédez un stock limité qui se recharge automatiquement au fur et à mesure. L'objectif est donc d'éliminer vos adversaires en les touchant grâce à l'explosion de vos bombes.

Les différentes métriques (rayon de l'explosion, nombre de bombes, durée de rechargement, etc.) ne sont pas imposées.

Dans un premier temps, le jeu se limitera aux règles décrites ci-dessus, puis vous l'améliorerez en y ajoutant des objets octroyant des bonus au personnage, des murs destructibles, etc.

Un launcher devra permettre de choisir entre deux possibilités :
— Jouer en local contre des ordinateurs
— Jouer en ligne contre des joueurs

-----

**Les différentes parties de ce sujet sont interconnectées, il ne faut pas voir le sujet comme un sujet linéaire progressif, mais comme un descriptif général vous donnant des pistes. Prenez le temps de tout lire avant de réfléchir à une répartition des tâches.**

-----

**Vous verrez dans ce sujet des parties écrites en rouge, ces parties sont des extensions qui, cumulées, représenteront un tier de la note. Vous pouvez donc les traiter en dernier.**

-----

## 2 Aspect général

Votre logiciel doit être conçu selon les standards et le respect des conventions de nommage Java.

Votre projet devra être scindé en trois **modules** distincts :

— **ClientSide** : Gère l'interface utilisateur et la partie graphique.
— **Common** : Regroupe les éléments partagés entre le client et le serveur (modèle de données, protocoles de communication réseau, etc.).
— **ServerSide** : Assure la gestion de la logique de jeu, des sessions actives et des salons (lobbys).

Lors d'une partie **hors ligne**, avant de la lancer, un écran de configuration permettant de choisir la taille de la carte et le nombre d'ennemi ainsi que leur difficulté sera proposé.

En mode **en ligne**, une fois connecté au serveur, un écran listant les différents salons ainsi que leur état sera proposé. Il sera interdit de rejoindre un salon plein ou commencé. Un salon se lance quand il possède au moins deux joueurs et que chaque joueur présent aura indiqué être prêt.

## 3 Déroulement d'une partie

Le jeu se déroule en temps réel. Le système s'articule autour de deux axes majeurs : la survie et la gestion des ressources.

Il sera impossible de passer au travers d'une bombe ou d'un mur.

### 3.1 Système de santé et conditions de victoire

Chaque personnage dispose d'un capital de points de vie défini au lancement de la partie.
— **Dégâts** : Dès qu'un joueur entre en contact avec une explosion, il perd une vie.
— **Élimination** : Lorsqu'un compteur de vie tombe à zéro, le joueur est considéré comme mort et est retiré de la partie.
— **Objectif** : Être le dernier survivant dans le labyrinthe.

### 3.2 Gestion des bombes et rechargement

Pour éliminer ses adversaires, le joueur doit gérer son stock de munitions :
— **Capacité maximale** : Le joueur possède un nombre limité de bombes simultanées (par exemple 3). Il peut les déposer librement sur le terrain.
— **Mécanique de détonation** : Une fois posée, la bombe n'explose qu'après un délai prédéfini, laissant le temps aux autres joueurs de s'enfuir ou de piéger le poseur.
— **Système de recharge** : Les bombes ne sont pas perdues définitivement. Le stock se régénère automatiquement au bout d'un certain temps.

## 4 Communication Client-Serveur

La communication entre un client et le serveur, pour un jeu en temps réel, va se faire via une **Socket**. Lorsque le client accède au serveur, une connexion est créée entre les deux (la **Socket**) qui perdure jusqu'à sa fermeture (perte de réseau, déconnexion volontaire, arrêt du serveur, etc).

### 4.1 Côté serveur

Le serveur est composé de plusieurs `Threads` :
— **Thread principal** : Un `Thread` attendant les nouvelles demandes de connexion.
— **Threads clients** : Une multitude de `Thread` gérant chacun la connexion avec un client, voir la partie \<span style="color:blue"\>ClientHandler\</span\>
— **Threads de jeu** : Une autre multitude de `Thread` qui géreront chacun une partie de jeu

#### 4.1.1 L'écoute des demandes de connexion (*Thread principal*)

Au démarrage du serveur, une instance de \<span style="color:blue"\>ServerSocket\</span\> doit être créée. Elle attend toute demande de connexion dans une boucle infinie avec `ServerSocket.accept()`. Cette fonction étant bloquante tant que personne ne s'est connecté.

Lors d'une connexion, la fonction `accept()` renvoie une instance de `Socket`. Un `ClientHandler` (classe que vous allez créer et qui hérite de `Thread`) devra alors être instanciée, et lancée, pour gérer la réception des messages de ce client sur le serveur.

Le serveur stocke une collection de ces `ClientHandler`, chacun correspondant à un client connecté. Cette collection pourra permettre d'envoyer des messages aux clients.

#### 4.1.2 Ressources partagées

**Problématique : Accès simultané à une même ressource**

Un gros danger lors de la manipulation d'un même objet par plusieurs `Thread`, est son accès simultané par deux `Thread` différents. La collection de `ClientHandler` du serveur pourrait être sollicitée par différents `Thread` simultanément.

Par exemple, si on tente d'envoyer un message aux différents clients et qu'un nouveau client se connecte en même temps, deux `Thread` auraient donc besoin d'accéder à cette liste, ce qui peut provoquer des comportements imprévisibles ou des crashs (*ConcurrentModificationException*).

**La solution : Rendre le code "Thread-Safe"**

Pour éviter ces conflits, on utilise le mot-clé **synchronized**. Il permet de définir un verrou (appelé *lock*) sur un objet. Tant qu'un `Thread` exécute un bloc synchronisé, tout autre `Thread` tentant d'accéder à un bloc protégé par le même verrou devra attendre la fin de l'opération.

Voici deux portions de code, celle de gauche est **ThreadSafe**, celle de droite ne l'est pas :

**Code ThreadSafe :**

```java
val socket = server.accept()
val out = new ClientHandler(socket, true);
synchronized (Server.clients) {
    Server.clients.add(out);
}
```

**Code NON ThreadSafe :**

```java
val socket = server.accept()
val out = new PrintWriter(socket.getOutputStream(), true);
Server.clients.add(out);
```

Le mot clef **synchronized** peut s'employer avec en paramètre un objet qui servira de verrou. Ici, l'objet **Server.clients** est le verrou. La JVM va s'assurer qu'un seul `Thread` à la fois puisse exécuter les portions de code synchronisées, afin d'éviter des comportements imprévus ou des exceptions (ici, liés à des accès concurrents au **Server.clients**).

Exemple :

1.  Un Thread T1 est lancé
2.  Un Thread T2 est lancé
3.  Les deux threads s'exécutent en parallèle
4.  Si T1 commence à exécuter la portion de code synchronized, le thread T2 ne pourra pas exécuter cette dernière avant que T1 ait terminé et inversement.

Dans le cas d'une liste, il existe depuis Java 19 la fonction **Collections.synchronizedList** qui s'en occupe pour vous.

Il est possible de noter une fonction en tant que **synchronized**.

De la même manière, tous vos objets étant exposés à différents `Thread` devront voir leur accès être **ThreadSafe**.

#### 4.1.3 ClientHandler (*Threads clients*)

La classe `ClientHandler` devra héritée de `Thread` et posséder la socket liée au client.

Chaque client connecté possède un flux entrant (l'`InputStream` de la socket : les données envoyées **par** le client) et un flux sortant (l'`OutputStream` de la Socket : les données envoyées **au** client).

Le rôle du `ClientHandler` est d'attendre et de lire les messages envoyés sur l'`InputStream` jusqu'à sa fermeture, et de faire l'action adéquate. Ces messages peuvent être de natures différentes : Demande de connexion à une salle de jeu, un message dans le chat, commandes de déplacement, etc.

### 4.2 Côté client

Le client lui, s'occupe d'ouvrir la connexion en créant une **Socket** paramétrée avec la bonne ip et le bon port.

A l'inverse du serveur, l'`InputStream` de la socket correspond aux données envoyées **par** le serveur et son flux sortant, l'`OutputStream`, aux données envoyées **au** serveur.

Le client doit donc posséder un `Thread` secondaire, tournant en permanence et écoutant les messages reçus **par le serveur**. Les messages envoyés **vers** le serveur seront eux envoyés en fonction des actions de l'utilisateur sur l'interface.

Plus de détails dans la partie 7.3.

## 5 Common

Le module **Common** se devra de stocker tout le code partagé par le client et le serveur.

### 5.1 Un logger

Un **Logger** affichant en console les différentes actions effectuées ainsi que l'heure à laquelle elles ont eu lieu doit être implémenté : Lancement d'un lobby, connexion/déconnexion d'un client, etc. Les erreurs comme la perte de connexion doivent être mises en avant.

Un pattern **Singleton** est attendu.

/*Red font*/ : Les messages les plus importants doivent être enregistrés dans des fichiers de log, et non simplement affichés en console. Un pattern**Chaîne de responsabilité** peut être envisagé pour répondre à ce besoin. /*fin red font*/

### 5.2 Communication réseaux

#### 5.2.1 Mise en place d'un protocole de communication

Les échanges entre le serveur et les clients sont de natures diverses : requêtes de connexion, gestion des salons, messagerie instantanée ou encore événements de jeu en temps réel.

Il faut donc mettre au point un système permettant une identification du type de chaque message.

Une solution agréable consiste à créer une classe par type de message qui héritent tous d'une même interface. Chaque classe contient des données liée à sa nature ainsi qu'un identificateur permettant de connaître cette dernière.

Pour assurer une communication fluide, il est primordial de concevoir un système de sérialisation / désérialisation de ces objets. Ce mécanisme permet de transformer des données structurées en un format transportable (comme du JSON / protobuf / octets ou autre), tout en permettant au destinataire d'identifier sans ambiguïté la nature du message et les données qu'il transporte.

/*red font*/ Afin de complexifier d'éventuelles tentatives de **Reverse engineering** par une personne malveillante sur votre programme, l'idéal est de ne pas envoyer du texte brut (JSON ou XML) mais un flux binaire. /*fin red font*/

/*fin red font*/ Deux avantages :
1. Le protocole devient illisible sans une connaissance précise de la structure des classes.
2. Meilleure performance liée à une quantité de données allégée. /*fin red font*/

/*red font*/ L'utilisation de **WireShark** afin d'intercepter vos paquets envoyés et reçus pour en connaître le contenu peut-être intéressant./*red font*/

#### 5.2.2 DTO

Dans le cadre d'un jeu en réseau, il est rarement pertinent de transférer l'intégralité d'un objet "Métier" à chaque mise à jour.

L'utilisation de DTO (Data Transfer Objects) est donc fortement recommandée pour adapter le message au contexte :
— **Initialisation** : Lors de la création d'une partie, un DTO complet peut être envoyé : pseudo, identifiant, l'apparence choisie et la position initiale.
— **Mise à jour en temps réel** : Pendant la partie, pour minimiser la latence, on privilégiera des DTO "légers" ne contenant que les informations strictement nécessaires (par exemple, uniquement l'ID du joueur et ses nouvelles coordonnées x et y).

Ces DTO pourront être le contenu de certains types de messages.

### 5.3 Partie modèle

#### 5.3.1 Génération du terrain : Labyrinthe parfait

Au début d'une partie, le terrain de jeu se devra d'être un **Labyrinthe parfait**. Un labyrinthe dit parfait est un labyrinthe où chaque endroit est accessible depuis n'importe quel endroit.

La création d'un labyrinthe devra se faire via un pattern **Fabrique**.

Il existe de nombreuses façons de générer un labyrinthe parfait, notamment **l'exploration exhaustive** et la **fusion aléatoire de chemin**. Mettez les en place.

/*red font*/Vous pouvez implémenter autant de nouveaux algorithmes / façons d'en générer qu'il vous plaît \! /*fin red font*/

#### 5.3.2 Intelligence Artificielle

Dans le mode hors ligne, ou dans des parties en ligne incomplète, des IA pourront faire partie du jeu. Il vous ait demandé de mettre en place au moins trois stratégies, comme par exemple :
— Une IA cherchant à tout prix à tuer, quitte à y perdre la vie
— Une IA cherchant à survivre le plus longtemps jusqu'à être en 1 contre 1
— etc.

#### 5.3.3 Amélioration du jeu

Une fois le jeu jouable en ligne, l'implémentation de fonctionnalités rendant le jeu plus intéressant sont possibles :
— Rendre certains murs destructibles après un certain nombre d'explosion
— Pouvoir augmenter le nombre de bombe disponible en tuant un adversaire
— Ramasser des éléments qui donnent différents boost : vitesse de déplacement, réduction du timer des bombes, etc.

Il est attendu d'avoir au moins deux améliorations.

/*red font*/ Vous pouvez en faire plus pour un max de fun \! /*fin red font*/

## 6 Module ServerSide

### 6.1 Identification des messages clients

Comme expliqué dans la partie 5.2, le serveur reçoit en permanence des flux de données variées (demandes de déplacement, demande de connexion, pose de bombes, messages de chat). Mapper ces messages à des actions spécifiques via une succession de blocs if/else ou switch sur le type de message rendrait rapidement le code illisible et difficile à maintenir.

Mettez en place un pattern pour déléguer efficacement le traitement de chaque message.

### 6.2 Gestion des différentes salles de jeux

Une fois le client connecté, la liste des salles de jeu disponible doit lui être présenté, voici une possibilité de visuel :

**FIGURE 1 – Liste des lobby disponible**

Si une salle est en cours de jeu ou si elle est pleine, la connexion doit être refusée et un message d'erreur envoyé à l'utilisateur.

Si la connexion réussie, tout autre client, dans la liste des lobbys, doit voir la mise à jour en temps réel du nombre de joueur dans chaque salon.

Une fois connecté, le joueur arrive dans un salon d'attente :

**FIGURE 2 – Exemple d'une salle d'attente possible**

Le joueur doit pouvoir envoyer des messages dans un chat interne au salon, indiquer s'il est prêt ou pouvoir retourner à la liste des salons.

Une fois tous les joueurs prêt, un compte à rebours se lance. Si aucun joueur n'annule sa préparation ou ne se déconnecte d'ici la fin de ce dernier, la partie se lance.

### 6.3 Lancement d'une partie et synchronisation (*Threads de jeu*)

Dès que tous les joueurs sont prêts et que le compte à rebours expire, le serveur procède à l'initialisation de la partie de jeu.

**Phase d'initialisation**

Le serveur diffuse toutes les informations nécessaires à tous les clients (Positions des joueurs, forme du terrain, apparence choisie, etc). Un mécanisme d'acquittement (ACK) est mis en place : chaque client doit confirmer la réception des données. Pour éviter tout blocage infini, un timeout déclenche le début de la partie même si un client tarde à répondre (en cas de latence élevée ou de déconnexion).

**La boucle de jeu**

Une fois la partie commencée, le serveur possède une boucle de jeu mettant à jour les informations à une fréquence de 60 itérations par seconde. Toutes les $x$ itérations, le serveur s'occupe d'envoyer une mise à jours aux clients du nouvel état du jeu.

**Attention** : Les **ClientHandler** reçoivent des instructions de manière asynchrone, selon votre implémentation, plusieurs **ClientHandler** pourraient essayer d'accéder aux mêmes ressources du modèle simultanément. Pensez bien à synchroniser ces dernières si besoin.

### /*red font*/ 6.4 Amélioration : Ajout de l'UDP (A faire en tout dernier)

Actuellement, toutes les communications entre un client et le serveur passent par une **Socket** (le protocole **TCP**), qui garantit la réception de chaque paquet. Cela provoque une latence accrue (retransmission des paquets perdus). Pour un jeu en temps réel, nous allons introduire l'UDP.

Selon le type de l'information, la rapidité prime sur la fiabilité. Pour les données importantes (Chat, entrées clavier, événements de jeu), le protocole **TCP** doit être employé. Pour les données dont la priorité est la fraîcheur de l'information (coordonnées des joueurs), le protocole **UDP** doit être employé.

Plusieurs points à prendre en compte dans l'UDP :
1. Les paquets ne sont plus certains d'arriver dans l'ordre, il se peut que la position à la frame $X$ arrive après la position à la frame $X + 1$. Il faut traiter ce cas.\</span\>
2. Il n'y a plus de connexion constante comme avec un socket. Chaque paquet doit pouvoir être identifié comme venant de tel ou tel client, il faut donc un nouvel ID unique pour chaque client. Cet ID ne doit pas être diffusé aux autres joueurs pour éviter une usurpation. /*fin red font*/

## 7 Module ClientSide

La partie client s'occupe principalement de tout le côté graphique de l'application. Chaque vue devra être associée à son fichier FXML.

Vous trouverez sur eCampus une archive contenant plusieurs sous-dossiers :
— **Characters** : Une trentaine de dossiers avec à l'intérieur les *sprites* des déplacement d'un personnage
— **Ground** : Différents *sprites* pour représenter un sol
— **Walls** : Différents *sprites* pour représenter un mur
— **Bomb** : Des *sprites* pour l'animation d'une bombe ou d'un collectable de bombe.
— **Explosions** : Différents *sprites* pour représenter une explosion.

Vous êtes libre d'ajouter d'autres *sprites* si l'envie vous prend.

### 7.1 Données modèle côté client

Les classes du modèle auront des informations supplémentaires ou des comportement légèrement différent côté client : tel que l'index du *sprite* de l'animation courante pour les entités, le modèle redéfini sa fonction de mise à jour pour ne mettre à jour que les *sprites*, etc.

Un patron se prête à cette notion afin d'éviter des informations inutiles dans les entités côté serveur, mettez le en place.

### 7.2 Représentation du jeu et animations

Une fois la partie lancée, le client à besoin de faire tourner sa propre version du modèle dans lequel il pioche les informations nécessaires.

Cependant, contrairement au modèle du serveur, ce dernier s'occupe uniquement de la gestion visuelle des *sprites*. Dans ce système, chaque entité du jeu, qu'il s'agisse d'un joueur, d'une bombe ou d'une explosion, dispose de sa propre animation composée d'une suite de *sprite* spécifique. Les informations modèle, tel que l'emplacement des joueurs, le moment de l'explosion d'une bombe, etc. sont des infos reçues par le serveur, que le modèle **client** injecte en parallèle pour synchroniser l'état de son monde.

À chaque cycle de mise à jour du modèle **client**, toute entité passe automatiquement à l'image suivante de l'animation qui lui est lié. Ce processus fonctionne en continue : une fois la dernière image de la liste atteinte, l'animation reprend instantanément au premier *sprite*. Ce cycle se poursuit aussi longtemps que l'action associée, telle qu'un déplacement vers la droite, est validée par les données en provenance du serveur.

Une façon de représenter cela, est grâce à un *Canvas* JavaFX, sur lequel sont dessiné les différents éléments du jeu. Chaque mise à jour du modèle redessine les différents éléments modifiés.

### 7.3 Gestion de la communication réseau : Pattern singleton, observateur

Dans votre application cliente, l'interface graphique (JavaFX) est composée de plusieurs scènes et contrôleurs distincts : la fenêtre de connexion, le lobby de recherche de partie, et enfin le terrain de jeu.

Malgré cette multiplicité d'écrans, le joueur ne doit posséder qu'une seule et unique source d'information du serveur : sa Socket de connexion.

L'idée est donc de créer un **NetworkManager** unique qui s'occupe de récupérer tous les messages arrivant du serveur.

Chaque contrôleur se doit de récupérer et d'écouter ce **NetworkManager** pour exécuter le comportement adéquat si le message reçu le concerne.

### 7.4 Synchronisation de l'image

Si un joueur a perdu sa connexion pendant quelques secondes, il se peut que l'état soit drastiquement différent. Pour que le rendu soit agréable à l'oeil, on ne va pas simplement 'téléporter' les entités à l'endroit où elles sont censé se situer.

Mettez en place un mécanisme de *threshold* permettant de voir la différence de distance entre la position réel et la position calculée par le modèle du client. Si la distance ne dépasse pas ce seuil (*threshold*), on fait 'glisser' l'image jusqu'à sa postion réel, sinon là on peut la téléporter.

## 8 Recommandations

Mettre en place, avant de commencer à coder, les interfaces de communications Client-Serveur et Modèle-Vue vous permet de plus facilement séparer le travail sans être dépendant les uns des autres.

Les blocs **finally** doivent être utilisés pour garantir la fermeture des connexions réseau.