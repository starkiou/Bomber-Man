import os
import requests

GITLAB_URL     = os.environ["CI_SERVER_URL"]
PROJECT_ID     = os.environ["CI_PROJECT_ID"]
GITLAB_TOKEN   = os.environ["GITLAB_TOKEN"]
OPENROUTER_KEY = os.environ["OPENROUTER_API_KEY"]
COMMIT_SHA     = os.environ["CI_COMMIT_SHA"]
COMMIT_MESSAGE = os.environ.get("CI_COMMIT_MESSAGE", "")

PROJECT_CONTEXT = """
Tu reviews le code d'un jeu Bomberman multijoueur en Java/JavaFX.

─── RÈGLES DU JEU ───────────────────────────────────────────
- Chaque joueur se déplace dans un labyrinthe 2D
- Un joueur ne peut pas traverser un mur ou une bombe
- Chaque joueur a un stock limité de bombes (ex: 3 max simultanées)
- Une bombe posée explose après un délai prédéfini dans un rayon prédéfini
- L'explosion blesse tout joueur dans son rayon
- Le stock de bombes se recharge automatiquement après un délai
- Un joueur perd une vie quand il est touché par une explosion
- Un joueur est éliminé quand ses points de vie tombent à 0
- Objectif : être le dernier survivant

─── MODES DE JEU ────────────────────────────────────────────
Hors ligne :
- Écran de config : choix de la taille de la carte, nombre d'ennemis, difficulté
- Partie contre des IA (au moins 3 comportements différents)

En ligne :
- Connexion à un serveur
- Liste des salons avec leur état (en attente / plein / en cours)
- Impossible de rejoindre un salon plein ou déjà commencé
- Salon se lance quand min 2 joueurs sont présents et tous marqués prêts
- Compte à rebours avant le lancement, annulable si un joueur se déconnecte
- Chat dans le salon d'attente

─── TERRAIN ─────────────────────────────────────────────────
- Labyrinthe parfait (tout endroit accessible depuis n'importe quel autre)
- Deux algorithmes de génération disponibles : exploration exhaustive + fusion aléatoire
- Murs destructibles (se détruisent après un certain nombre d'explosions)

─── AMÉLIORATIONS (items ramassables) ───────────────────────
- Au moins 2 types de bonus : ex. vitesse de déplacement, réduction timer bombes,
  augmentation du stock de bombes en tuant un adversaire

─── COMMUNICATION RÉSEAU ────────────────────────────────────
- Serveur envoie l'état initial à tous les clients avec accusé de réception (ACK)
- Timeout si un client ne répond pas à l'ACK (la partie se lance quand même)
- Boucle de jeu serveur à 60 itérations/seconde
- Mises à jour envoyées aux clients à intervalles réguliers
- Si un joueur reconnecte après une coupure, son personnage est repositionné
  progressivement (pas de téléportation si distance faible)
"""

def get_all_java_files():
    """
    Récupère récursivement tous les fichiers Java/Kotlin du repo
    via l'API GitLab (pas besoin de cloner).
    """
    url = f"{GITLAB_URL}/api/v4/projects/{PROJECT_ID}/repository/tree"
    headers = {"PRIVATE-TOKEN": GITLAB_TOKEN}

    # Récupère la liste de tous les fichiers récursivement
    all_files = []
    page = 1
    while True:
        response = requests.get(
            url,
            headers=headers,
            params={"recursive": True, "per_page": 100, "page": page},
            timeout=15
        )
        data = response.json()
        if not data:
            break
        all_files.extend(data)
        page += 1

    # Filtre uniquement les .java et .kt, ignore les tests et le dossier scripts
    java_files = [
        f for f in all_files
        if f["type"] == "blob"
        and (f["path"].endswith(".java") or f["path"].endswith(".kt"))
        and "test" not in f["path"].lower()
        and "scripts" not in f["path"]
    ]

    return java_files


def get_file_content(file_path: str) -> str:
    """Récupère le contenu d'un fichier via l'API GitLab."""
    import urllib.parse
    encoded_path = urllib.parse.quote(file_path, safe="")
    url = f"{GITLAB_URL}/api/v4/projects/{PROJECT_ID}/repository/files/{encoded_path}/raw"
    headers = {"PRIVATE-TOKEN": GITLAB_TOKEN}
    response = requests.get(
        url,
        headers=headers,
        params={"ref": "development"},
        timeout=15
    )
    if response.status_code == 200:
        return response.text
    return ""


def build_codebase_summary(java_files: list) -> str:
    """
    Construit un résumé du codebase en priorisant les fichiers importants.
    Limite stricte de caractères pour rester dans le context window.
    """
    MAX_TOTAL_CHARS = 20000
    MAX_PER_FILE    = 2000

    # Priorité : on lit d'abord les fichiers les plus importants
    PRIORITY_KEYWORDS = [
        "Logger", "Network", "Message", "Serializ",
        "Factory", "Strategy", "Handler", "Manager",
        "Entity", "Player", "Bomb", "Maze", "DTO"
    ]

    def priority_score(path: str) -> int:
        score = 0
        for kw in PRIORITY_KEYWORDS:
            if kw.lower() in path.lower():
                score += 1
        return score

    sorted_files = sorted(java_files, key=lambda f: priority_score(f["path"]), reverse=True)

    result = ""
    total_chars = 0
    files_included = 0
    files_skipped = []

    for file_info in sorted_files:
        if total_chars >= MAX_TOTAL_CHARS:
            files_skipped.append(file_info["path"])
            continue

        content = get_file_content(file_info["path"])
        if not content:
            continue

        # Tronque si le fichier est trop long
        if len(content) > MAX_PER_FILE:
            content = content[:MAX_PER_FILE] + "\n// [... fichier tronqué ...]"

        block = f"\n\n{'='*50}\n// {file_info['path']}\n{'='*50}\n{content}"
        result += block
        total_chars += len(block)
        files_included += 1

    # Résumé des fichiers ignorés
    if files_skipped:
        result += f"\n\n[{len(files_skipped)} fichiers non inclus par limite de tokens : "
        result += ", ".join(f["path"].split("/")[-1] for f in sorted_files if f["path"] in files_skipped)
        result += "]"

    print(f"{files_included} fichiers inclus, {len(files_skipped)} ignorés ({total_chars} chars)")
    return result


def post_commit_comment(body: str):
    """Poste un commentaire sur le commit dans development."""
    url = f"{GITLAB_URL}/api/v4/projects/{PROJECT_ID}/repository/commits/{COMMIT_SHA}/comments"
    headers = {"PRIVATE-TOKEN": GITLAB_TOKEN}
    requests.post(url, headers=headers, json={"note": body}, timeout=15)


def call_openrouter(prompt: str) -> str:
    response = requests.post(
        "https://openrouter.ai/api/v1/chat/completions",
        headers={
            "Authorization": f"Bearer {OPENROUTER_KEY}",
            "Content-Type": "application/json",
            "HTTP-Referer": GITLAB_URL,
            "X-Title": "Bomberman Full Review Bot"
        },
        json={
            "model": "openai/gpt-oss-120b:free",
            "messages": [
                {
                    "role": "system",
                    "content": "Tu es un architecte logiciel senior spécialisé Java. "
                               "Tu fais des audits de code complets, concis et actionnables. "
                               "✅ = bien, ⚠️ = à améliorer, ❌ = problème critique."
                },
                {
                    "role": "user",
                    "content": prompt
                }
            ],
            "max_tokens": 2048,
            "temperature": 0.1
        },
        timeout=90  # plus long car plus de tokens à traiter
    )
    response.raise_for_status()
    return response.json()["choices"][0]["message"]["content"]


def main():
    print("=== Full review du projet ===")

    java_files = get_all_java_files()
    print(f"{len(java_files)} fichiers Java/Kotlin trouvés")

    if not java_files:
        print("Aucun fichier Java trouvé, abandon.")
        return

    codebase = build_codebase_summary(java_files)

    prompt = f"""
{PROJECT_CONTEXT}

─── ÉTAT ACTUEL DU CODEBASE (branche development) ──────────
{codebase}

─── CONSIGNES D'AUDIT ───────────────────────────────────────
Fais un audit global du projet en 5 sections :

1. **Patterns manquants ou mal implémentés**
   Liste les patterns attendus qui ne sont pas encore là ou qui sont incorrects.

2. **Problèmes Thread-safety**
   Collections partagées non protégées, accès concurrents suspects.

3. **Violations d'architecture**
   Dépendances entre modules interdites, logique métier dans les mauvaises classes, etc.

4. **Dette technique**
   Code dupliqué, classes trop longues, méthodes trop complexes.

5. **Priorités pour la suite**
   Les 3 choses les plus importantes à corriger ou implémenter en premier.

Sois direct. Max 30 lignes au total.
"""

    print("Appel à OpenRouter pour l'audit complet...")
    review = call_openrouter(prompt)
    print("Audit reçu.")

    comment = f"""## 🤖 Audit complet du projet — `development`

> Déclenché par le commit : _{COMMIT_MESSAGE.strip()}_

{review}

---
*Audit automatique sur l'ensemble du codebase — modèle `gpt-oss-120b`. Les fichiers volumineux ont pu être tronqués.*
"""

    post_commit_comment(comment)
    print("Commentaire posté sur le commit.")


if __name__ == "__main__":
    main()