package model.aiPlayer;


public class AIFactory {

    public static AIPlayer create(Strategy strategy, int id, int x, int y, int hp, double speed, int maxBombs) {
        return switch (strategy) {
            case AGGRESSIVE  -> new AggressiveAI(id, x, y, hp, speed, maxBombs);
            case SURVIVALIST -> new SurvivalistAI(id, x, y, hp, speed, maxBombs);
            case TACTICAL    -> new TacticalAI(id, x, y, hp, speed, maxBombs);
        };
    }

}
