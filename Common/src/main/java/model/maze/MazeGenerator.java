package model.maze;

public interface MazeGenerator {
    CellType[][] generate(int width, int height);
}