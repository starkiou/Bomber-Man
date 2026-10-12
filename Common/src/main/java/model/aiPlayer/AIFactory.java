package model.aiPlayer;


public class AIFactory {

    /**
     * Tire au sort la stratégie d'un bot. Plus la difficulté est élevée,
     * plus les stratégies offensives sont probables.
     *
     * @param difficulty libellé choisi dans le menu (Facile, Normal, Difficile, Extrême)
     */
    public static Strategy randomStrategy(String difficulty) {
        // Poids en pourcentage : {survivaliste, tactique, agressif}
        int[] weights;
        if (difficulty == null)                    weights = new int[]{25, 50, 25};
        else if (difficulty.contains("Facile"))    weights = new int[]{70, 30, 0};
        else if (difficulty.contains("Difficile")) weights = new int[]{10, 40, 50};
        else if (difficulty.contains("Extr"))      weights = new int[]{0, 25, 75};
        else                                       weights = new int[]{25, 50, 25};

        int roll = java.util.concurrent.ThreadLocalRandom.current().nextInt(100);
        if (roll < weights[0]) return Strategy.SURVIVALIST;
        if (roll < weights[0] + weights[1]) return Strategy.TACTICAL;
        return Strategy.AGGRESSIVE;
    }
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
