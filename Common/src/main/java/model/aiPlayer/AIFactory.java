package model.aiPlayer;


public class AIFactory {

    public static AIPlayer create(Strategy strategy, int id, int x, int y, int hp, double speed, int maxBombs) {
        return switch (strategy) {
            case AGGRESSIVE -> {
                var ai = new AggressiveAI(id, x, y, hp, speed, maxBombs);
                ai.setMoveCooldownMs(150);
                yield ai;
            }
            case SURVIVALIST -> {
                var ai = new SurvivalistAI(id, x, y, hp, speed, maxBombs);
                ai.setMoveCooldownMs(250);
                yield ai;
            }
            case TACTICAL -> {
                var ai = new TacticalAI(id, x, y, hp, speed, maxBombs);
                ai.setMoveCooldownMs(200);
                yield ai;
            }
        };
    }

}
