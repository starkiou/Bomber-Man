import os
import requests

# ─── Variables GitLab CI (injectées automatiquement) ────────────────────────
GITLAB_URL    = os.environ["CI_SERVER_URL"]
PROJECT_ID    = os.environ["CI_PROJECT_ID"]
MR_IID        = os.environ["CI_MERGE_REQUEST_IID"]
GITLAB_TOKEN  = os.environ["GITLAB_TOKEN"]
OPENROUTER_KEY = os.environ["OPENROUTER_API_KEY"]

# ─── Contexte du projet injecté dans le prompt ──────────────────────────────
PROJECT_CONTEXT = """
Tu review le code d'un projet étudiant : un jeu Bomberman multijoueur en Java/JavaFX.

Architecture en 3 modules Maven :
- Common    : Logger (Singleton + Chaîne de responsabilité), Messages réseau,
              Sérialisation, DTOs, Modèle (Entity, Player, Bomb, Wall, Explosion,
              Maze), Génération de labyrinthe (pattern Fabrique), IA (pattern Stratégie)
- ServerSide: ServerSocket, ClientHandler (Thread), boucle de jeu 60fps,
              gestion des lobbys, synchronisation Thread-safe
- ClientSide: JavaFX, FXML, Canvas, animations sprites,
              NetworkManager (Singleton + Observateur), seuil de synchronisation

Patterns de conception ATTENDUS et leur emplacement :
- Singleton           → Logger, NetworkManager
- Chaîne de responsabilité → ConsoleLogger → FileLogger
- Fabrique abstraite  → MazeFactory (ExhaustiveFactory, RandomFusionFactory)
- Stratégie           → AIStrategy (AggressiveStrategy, SurvivalStrategy, RandomStrategy)
- Commande            → Messages réseau (ConnectMessage, ChatMessage, etc.)
- Observateur         → NetworkManager notifie les contrôleurs JavaFX
- DTO                 → PlayerDTO, BombDTO, GameStateDTO (séparés des classes métier)

Règles critiques :
- Tout accès à une collection partagée entre Threads doit être synchronized
  ou utiliser Collections.synchronizedList / ConcurrentHashMap
- Les connexions réseau doivent être fermées dans des blocs finally
- Aucune logique métier dans les contrôleurs JavaFX (MVC strict)
- Les classes du module Common ne doivent pas avoir de dépendances vers
  ClientSide ou ServerSide
"""

def get_mr_diff():
    """Récupère le diff de la MR depuis l'API GitLab."""
    url = f"{GITLAB_URL}/api/v4/projects/{PROJECT_ID}/merge_requests/{MR_IID}/diffs"
    headers = {"PRIVATE-TOKEN": GITLAB_TOKEN}
    response = requests.get(url, headers=headers, timeout=15)
    response.raise_for_status()

    diffs = response.json()
    result = ""
    total_chars = 0
    max_chars = 12000  # limite pour rester dans le context window

    for d in diffs:
        if total_chars >= max_chars:
            result += "\n[... diff tronqué pour respecter la limite de tokens ...]"
            break
        # Ignore les fichiers non-Java et les fichiers de config
        path = d.get("new_path", "")
        if not path.endswith(".java") and not path.endswith(".kt"):
            continue

        header = f"\n\n{'='*60}\nFichier : {path}\n{'='*60}\n"
        diff_content = d.get("diff", "")[:3000]  # max 3000 chars par fichier

        result += header + diff_content
        total_chars += len(header) + len(diff_content)

    return result if result else "Aucun fichier Java/Kotlin modifié dans cette MR."


def get_mr_info():
    """Récupère le titre et la description de la MR."""
    url = f"{GITLAB_URL}/api/v4/projects/{PROJECT_ID}/merge_requests/{MR_IID}"
    headers = {"PRIVATE-TOKEN": GITLAB_TOKEN}
    response = requests.get(url, headers=headers, timeout=15)
    response.raise_for_status()
    data = response.json()
    return data.get("title", ""), data.get("description", ""), data.get("source_branch", "")


def call_openrouter(prompt: str) -> str:
    """Appelle l'API OpenRouter avec le modèle gratuit."""
    response = requests.post(
        "https://openrouter.ai/api/v1/chat/completions",
        headers={
            "Authorization": f"Bearer {OPENROUTER_KEY}",
            "Content-Type": "application/json",
            "HTTP-Referer": GITLAB_URL,  # requis par OpenRouter
            "X-Title": "Bomberman SAE Review Bot"
        },
        json={
            "model": "openai/gpt-oss-120b:free",
            "messages": [
                {
                    "role": "system",
                    "content": "Tu es un reviewer de code senior spécialisé en Java, "
                               "patterns de conception et architecture logicielle. "
                               "Tes reviews sont concises, factuelles et constructives. "
                               "Tu utilises des emojis pour la lisibilité : "
                               "✅ bon, ⚠️ à améliorer, ❌ problème critique."
                },
                {
                    "role": "user",
                    "content": prompt
                }
            ],
            "max_tokens": 1024,
            "temperature": 0.2  # peu de créativité, on veut du factuel
        },
        timeout=60
    )
    response.raise_for_status()
    return response.json()["choices"][0]["message"]["content"]


def post_comment(body: str):
    """Poste un commentaire sur la MR GitLab."""
    url = f"{GITLAB_URL}/api/v4/projects/{PROJECT_ID}/merge_requests/{MR_IID}/notes"
    headers = {"PRIVATE-TOKEN": GITLAB_TOKEN}
    requests.post(url, headers=headers, json={"body": body}, timeout=15)


def delete_old_bot_comments():
    """Supprime les anciens commentaires du bot pour éviter le spam."""
    url = f"{GITLAB_URL}/api/v4/projects/{PROJECT_ID}/merge_requests/{MR_IID}/notes"
    headers = {"PRIVATE-TOKEN": GITLAB_TOKEN}
    response = requests.get(url, headers=headers, timeout=15)

    for note in response.json():
        if "🤖 Review automatique" in note.get("body", ""):
            note_id = note["id"]
            requests.delete(
                f"{url}/{note_id}",
                headers=headers,
                timeout=15
            )


def main():
    print("=== Démarrage du bot de review ===")

    title, description, branch = get_mr_info()
    print(f"MR : {title} (branche : {branch})")

    diff = get_mr_diff()
    print(f"Diff récupéré ({len(diff)} caractères)")

    if diff == "Aucun fichier Java/Kotlin modifié dans cette MR.":
        print("Aucun fichier Java/Kotlin — review ignorée.")
        return

    prompt = f"""
{PROJECT_CONTEXT}

─── MERGE REQUEST ───────────────────────────────────────────
Titre   : {title}
Branche : {branch}
Description : {description or '(aucune description)'}

─── DIFF ────────────────────────────────────────────────────
{diff}

─── CONSIGNES DE REVIEW ─────────────────────────────────────
Fais une review structurée en 4 sections MAX, sois concis (20 lignes max) :

1. **Patterns de conception** : les patterns attendus sont-ils bien utilisés ?
   Des patterns manquent-ils ou sont-ils mal implémentés ?

2. **Thread-safety** : y a-t-il des accès concurrents non protégés ?
   Des collections partagées sans synchronisation ?

3. **Architecture** : le code respecte-t-il la séparation Common/Server/Client ?
   Y a-t-il des dépendances circulaires ou du code mal placé ?

4. **Points positifs** : ce qui est bien fait.

Si le diff est trop court ou trivial pour une review approfondie, dis-le simplement.
"""

    print("Appel à OpenRouter...")
    review = call_openrouter(prompt)
    print("Review reçue.")

    # Supprime l'ancienne review du bot si elle existe
    delete_old_bot_comments()

    comment = f"""## 🤖 Review automatique — {branch}

{review}

---
*Bot de review automatique — modèle `gpt-oss-120b`. Ne pas suivre aveuglément, c'est un outil d'aide.*
"""

    post_comment(comment)
    print("Commentaire posté sur la MR.")


if __name__ == "__main__":
    main()