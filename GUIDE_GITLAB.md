# Guide Git — SAE Bomberman

> **Lis ce fichier en entier avant de toucher au repo.**  
> Si t'as un doute, demande avant de faire n'importe quoi.

---

## Les branches — comment c'est organisé

```
main
└── development          ← base de tout, on ne push JAMAIS directement dessus
    ├── common           ← base du module Common
    │   ├── common/logger
    │   ├── common/network
    │   └── common/...
    ├── server           ← base du module Server
    │   ├── server/lobby
    │   └── server/...
    └── client           ← base du module Client
        ├── client/layout
        └── client/...
```

**Règle absolue : on ne push jamais directement sur `development`, `common`, `server` ou `client`.**  
Ces branches sont protégées. Si tu essaies, GitLab te bloquera.  
Tout passe par des branches feature + Merge Request.

---

## Workflow complet — ce que tu fais à chaque feature

### Étape 1 — Te mettre à jour avant de commencer

Ne commence JAMAIS à coder sans faire ça d'abord.

```bash
# Va sur la branche de ton module (celle qui te concerne)
git checkout common       # ou server, ou client
git pull origin common    # récupère les derniers changements
```

### Étape 2 — Créer ta branche feature

```bash
# Toujours créer la branche depuis la branche de ton module
git checkout -b common/logger
```

La convention de nommage des branches :
```
ton-module/nom-de-la-feature

ex : 

common/logger
common/network-protocol
server/lobby
server/game-loop
client/login-screen
client/game-canvas
```

Uniquement des minuscules, des tirets, pas d'espaces, pas d'accents.

### Étape 3 — Coder et commiter régulièrement

Commite souvent, pas tout à la fin. Après chaque truc qui fonctionne :

```bash
git add .
git commit -m "feat(logger): ajout ConsoleLogger avec timestamp"
```

Voir la section **Conventions de commit** plus bas pour les règles.

### Étape 4 — Pousser ta branche

```bash
git push origin common/logger
```

### Étape 5 — Créer la Merge Request sur GitLab

Quand ta feature est terminée et fonctionne :

1. Va sur **git.unicaen.fr/heyberg241/sae_s4_groupe_b**
2. GitLab affiche une bannière jaune "Create merge request" → clique dessus  
   *(sinon : menu gauche → Merge Requests → New merge request)*
3. Remplis le formulaire :
   - **Source branch** : ta branche (`common/logger`)
   - **Target branch** : la branche de ton module (`common`)
   - **Title** : même format que tes commits (`feat(logger): ConsoleLogger + FileLogger`)
   - **Assignee** : toi-même
   - **Reviewer** : quelqu'un de ton sous-groupe
4. Clique **Create merge request**

**C'est le reviewer qui merge, pas toi.**

### Étape 6 — Après la review

Deux cas possibles :

**Le reviewer approuve** → il clique Merge, c'est terminé, tu peux supprimer ta branche.

**Le reviewer demande des corrections** → il laisse des commentaires sur les lignes concernées.  
Tu corriges sur la même branche, tu commites, tu push — la MR se met à jour automatiquement, pas besoin d'en recréer une.

```bash
# Tu corriges le code
git add .
git commit -m "fix(logger): correction selon review"
git push origin common/logger
# La MR se met à jour toute seule
```

---

## Conventions de commit — obligatoires

Format :
```
type(scope): description courte en anglais
```

Les types disponibles :

| Type | Quand l'utiliser |
|---|---|
| `feat` | Tu ajoutes une nouvelle fonctionnalité |
| `fix` | Tu corriges un bug |
| `refactor` | Tu réorganises du code sans changer son comportement |
| `style` | Indentation, espaces, renommage de variables |
| `docs` | Tu modifies de la documentation ou des commentaires |
| `test` | Tu ajoutes des tests |
| `ci` | Tu modifies les fichiers GitLab CI |

Les scopes (à adapter) :
```
logger / network / model / maze / ai
lobby / game-loop / client-handler
layout / canvas / animations / network-manager
```

Exemples corrects :
```
feat(lobby): ajout de la liste des salons disponibles
fix(bomb): correction du rayon d'explosion
refactor(maze): extraction de la logique de génération
feat(logger): implémentation de la chaîne de responsabilité
fix(client-handler): correction accès concurrent sur la liste clients
```

Exemples interdits :
```
fix                        ← pas de description
WIP                        ← commit en cours de travail, à éviter
feat: plein de trucs       ← trop vague
Correction du bug          ← pas de type ni de scope
```

---

## Rattraper un retard — le rebase

Si pendant que tu codais tes collègues ont mergé des trucs dans votre branche commune, tu dois rattraper ce retard **avant** de créer ta MR.

```bash
# Tu es sur ta branche feature
git checkout ton-module/ta-feature

# Tu récupères l'état du remote sans modifier ton code
git fetch origin

# Tu rejoues tes commits par-dessus la version à jour
git rebase origin/ton-module
```

**Si Git signale des conflits** (ça arrive, c'est normal) :

```bash
# Git t'indique les fichiers en conflit, par exemple :
# CONFLICT: src/main/java/common/Logger.java

# 1. Ouvre le fichier dans VSCode
# Tu verras des marqueurs comme ça :
# <<<<<<< HEAD
# code de tes collègues
# =======
# ton code
# >>>>>>> ton commit

# 2. Modifie le fichier pour garder ce qui est juste
#    (parfois les deux, parfois un seul des deux)

# 3. Une fois le fichier corrigé :
git add src/main/java/common/Logger.java

# 4. Continue le rebase
git rebase --continue

# Si t'as plusieurs commits en conflit, répète 1→4 pour chacun

# Si tu veux tout annuler et recommencer :
git rebase --abort
```

Ensuite tu push. Comme le rebase a réécrit l'historique, il faut forcer :

```bash
git push origin ton-module/nom-de-la-feature --force-with-lease
```

> `--force-with-lease` est plus safe que `--force` : il échoue si quelqu'un d'autre a push sur ta branche entre temps.

**Ne fais JAMAIS `git rebase` sur `development`, `common`, `server` ou `client` directement — uniquement sur tes branches feature personnelles.**

---

## Synchro quotidienne — à faire chaque matin

```bash
git checkout ton-module      # ta branche de module
git pull origin ton-module   # tu récupères ce qui a été mergé

git checkout ton-module/ta-feature
git rebase origin/ton-module # tu mets ta feature à jour
```

---

## Ce qui est interdit — à ne jamais faire

❌ `git push origin development` — branche protégée, GitLab bloquera  
❌ `git push origin common` — idem  
❌ `git push origin server` — idem  
❌ `git push origin client` — idem  
❌ `git push --force` — utilise toujours `--force-with-lease` à la place  
❌ Commiter des fichiers de config IDE (`.idea/`, `.vscode/`) — le `.gitignore` s'en occupe  
❌ Commiter des fichiers compilés (`target/`, `.class`) — idem  
❌ S'approuver sa propre Merge Request  
❌ Merger sans que le pipeline CI soit vert  
❌ Travailler à plusieurs sur la même branche feature sans se coordonner  

---

## Les pipelines CI — c'est quoi les checks automatiques

À chaque MR, deux vérifications se lancent automatiquement :

**1. Compilation + Checkstyle**  
Vérifie que le code compile et respecte les conventions Java.  
Si ce check est rouge → la MR est bloquée, tu dois corriger avant de merger.

Erreurs fréquentes de Checkstyle et comment les corriger :

| Erreur | Ce que ça veut dire | Correction |
|---|---|---|
| `NeedBraces` | Un `if` sans accolades | Ajoute `{ }` |
| `MagicNumber` | Un nombre en dur dans le code | Crée une constante |
| `VisibilityModifier` | Attribut non privé | Passe-le en `private` |
| `UnusedImports` | Import inutilisé | Supprime-le |
| `MethodLength` | Méthode > 60 lignes | Découpe en sous-méthodes |

**2. Bot de review IA**  
Poste un commentaire automatique sur la MR pour signaler des problèmes potentiels.  
Ce check est informatif, il ne bloque pas la MR — mais lis quand même ce qu'il dit.

---

## Résumé en 7 commandes

```bash
# 1. Me mettre à jour
git checkout ton-module && git pull origin ton-module

# 2. Créer ma branche
git checkout -b ton-module/ma-feature

# 3. Coder...

# 4. Commiter
git add . && git commit -m "feat(scope): description"

# 5. Pousser
git push origin ton-module/ma-feature

# 6. Créer la MR sur GitLab (interface web)

# 7. Si retard à rattraper
git fetch origin && git rebase origin/ton-module
```
```

---

Message de commit pour ajouter ce fichier :

```
docs: ajout guide Git pour l'équipe
```