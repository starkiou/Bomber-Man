package network.message;

import model.game.GameSnapshot;
import model.game.GameSnapshot.BombState;
import model.game.GameSnapshot.ExplosionState;
import model.game.GameSnapshot.PlayerState;
import model.maze.CellType;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * État complet de la partie, envoyé par le serveur à chaque client.
 * Le serveur est seul à simuler le jeu : les clients affichent ce qu'ils reçoivent.
 */
public class GameStateMessage implements Message {

    /** Une lettre par cellule, pour ne pas répéter le nom complet 165 fois par message. */
    private static final String CELL_CODES = "WBE";
    private static final CellType[] CELLS = {CellType.WALL, CellType.BRICK, CellType.EMPTY};

    private final GameSnapshot snapshot;

    public GameStateMessage(GameSnapshot snapshot) {
        this.snapshot = snapshot;
    }

    public GameStateMessage(JSONObject json) {
        List<PlayerState> players = new ArrayList<>();
        JSONArray playersArray = json.getJSONArray("players");
        for (int i = 0; i < playersArray.length(); i++) {
            JSONObject p = playersArray.getJSONObject(i);
            players.add(new PlayerState(p.getInt("id"), p.getInt("x"), p.getInt("y"), p.getBoolean("dead"),
                    p.getInt("hp"), p.getInt("bombs"), p.getInt("maxBombs"), p.getInt("kills")));
        }

        List<BombState> bombs = new ArrayList<>();
        JSONArray bombsArray = json.getJSONArray("bombs");
        for (int i = 0; i < bombsArray.length(); i++) {
            JSONObject b = bombsArray.getJSONObject(i);
            bombs.add(new BombState(b.getInt("id"), b.getInt("x"), b.getInt("y"),
                    b.getInt("radius"), b.getInt("owner"), b.getLong("remaining")));
        }

        List<ExplosionState> explosions = new ArrayList<>();
        JSONArray explosionsArray = json.getJSONArray("explosions");
        for (int i = 0; i < explosionsArray.length(); i++) {
            JSONObject e = explosionsArray.getJSONObject(i);
            explosions.add(new ExplosionState(e.getInt("x"), e.getInt("y"), e.getLong("age"), e.getInt("type")));
        }

        JSONArray rows = json.getJSONArray("grid");
        CellType[][] grid = new CellType[rows.length()][];
        for (int y = 0; y < rows.length(); y++) {
            String row = rows.getString(y);
            grid[y] = new CellType[row.length()];
            for (int x = 0; x < row.length(); x++) {
                grid[y][x] = CELLS[CELL_CODES.indexOf(row.charAt(x))];
            }
        }

        this.snapshot = GameSnapshot.of(players, bombs, explosions, grid,
                json.getBoolean("gameOver"), json.getInt("winnerId"), json.getLong("remainingSeconds"));
    }

    public GameSnapshot getSnapshot() {
        return snapshot;
    }

    @Override
    public MessageType getMessageType() {
        return MessageType.GAME_STATE;
    }

    @Override
    public JSONObject getData() {
        JSONObject obj = new JSONObject();

        JSONArray playersArray = new JSONArray();
        for (PlayerState p : snapshot.getPlayers()) {
            JSONObject o = new JSONObject();
            o.put("id", p.id());
            o.put("x", p.x());
            o.put("y", p.y());
            o.put("dead", p.isDead());
            o.put("hp", p.hp());
            o.put("bombs", p.currentBombs());
            o.put("maxBombs", p.maxBombs());
            o.put("kills", p.kills());
            playersArray.put(o);
        }
        obj.put("players", playersArray);

        JSONArray bombsArray = new JSONArray();
        for (BombState b : snapshot.getBombs()) {
            JSONObject o = new JSONObject();
            o.put("id", b.id());
            o.put("x", b.x());
            o.put("y", b.y());
            o.put("radius", b.radius());
            o.put("owner", b.ownerID());
            o.put("remaining", b.timeRemainingMs());
            bombsArray.put(o);
        }
        obj.put("bombs", bombsArray);

        JSONArray explosionsArray = new JSONArray();
        for (ExplosionState e : snapshot.getExplosions()) {
            JSONObject o = new JSONObject();
            o.put("x", e.x());
            o.put("y", e.y());
            o.put("age", e.ageMs());
            o.put("type", e.spriteType());
            explosionsArray.put(o);
        }
        obj.put("explosions", explosionsArray);

        JSONArray rows = new JSONArray();
        for (CellType[] row : snapshot.getGrid()) {
            StringBuilder sb = new StringBuilder(row.length);
            for (CellType cell : row) sb.append(CELL_CODES.charAt(cell.ordinal()));
            rows.put(sb.toString());
        }
        obj.put("grid", rows);

        obj.put("gameOver", snapshot.isGameOver());
        obj.put("winnerId", snapshot.getWinnerId());
        obj.put("remainingSeconds", snapshot.getRemainingSeconds());
        return obj;
    }
}
