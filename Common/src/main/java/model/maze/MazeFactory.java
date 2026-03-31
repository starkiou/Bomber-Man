package model.maze;

public class MazeFactory {
    public enum Algorithm { EXHAUSTIVE, RANDOM_FUSION }

    public static CellType[][] createMaze(Algorithm algo, int width, int height) {
        MazeGenerator generator = switch (algo) {
            case EXHAUSTIVE -> new ExhaustiveGenerator();
            case RANDOM_FUSION -> new RandomFusionGenerator();
        };
        return generator.generate(width, height);
    }
}