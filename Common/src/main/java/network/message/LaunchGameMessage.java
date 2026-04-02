package network.message;

import java.util.ArrayList;
import java.util.List;
import model.maze.CellType;
import org.json.JSONArray;
import org.json.JSONObject;

// envoyé par client -> serveur (vide) pour demander le lancement
// envoyé par serveur -> tous les clients (avec config complète)
public class LaunchGameMessage implements Message {
    private List<ClientInfoDTO> players = new ArrayList<>();
    private int botCount;
    private CellType[][] grid;

    // constructeur vide = trigger client -> serveur
    public LaunchGameMessage() {}

    // constructeur serveur -> clients
    public LaunchGameMessage(List<ClientInfoDTO> players, int botCount, CellType[][] grid) {
        this.players = players;
        this.botCount = botCount;
        this.grid = grid;
    }

    public LaunchGameMessage(JSONObject json) {
        this.players = new ArrayList<>();
        this.botCount = json.optInt("botCount", 0);

        if (json.has("players")) {
            JSONArray arr = json.getJSONArray("players");
            for (int i = 0; i < arr.length(); i++)
                players.add(new ClientInfoDTO(arr.getJSONObject(i)));
        }

        if (json.has("grid")) {
            JSONArray gridArr = json.getJSONArray("grid");
            int h = gridArr.length();
            int w = gridArr.getJSONArray(0).length();
            this.grid = new CellType[h][w];
            for (int y = 0; y < h; y++) {
                JSONArray row = gridArr.getJSONArray(y);
                for (int x = 0; x < w; x++)
                    this.grid[y][x] = CellType.valueOf(row.getString(x));
            }
        }
    }

    @Override
    public JSONObject getData() {
        JSONObject obj = new JSONObject();
        obj.put("botCount", botCount);

        JSONArray arr = new JSONArray();
        for (ClientInfoDTO p : players) arr.put(p.toJson());
        obj.put("players", arr);

        if (grid != null) {
            JSONArray gridArr = new JSONArray();
            for (CellType[] row : grid) {
                JSONArray rowArr = new JSONArray();
                for (CellType cell : row) rowArr.put(cell.name());
                gridArr.put(rowArr);
            }
            obj.put("grid", gridArr);
        }

        return obj;
    }

    @Override
    public MessageType getMessageType() { return MessageType.LAUNCH_GAME; }

    public List<ClientInfoDTO> getPlayers() { return players; }
    public int getBotCount() { return botCount; }
    public CellType[][] getGrid() { return grid; }
}
