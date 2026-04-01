package model.maze;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Random;

public class ExhaustiveGenerator implements MazeGenerator {
    private final Random random = new Random();

    @Override
    public CellType[][] generate(int width, int height) {
        CellType[][] grid = new CellType[height][width];

        // Remplissage initial : Tout est WALL
        for (int i = 0; i < height; i++) {
            Arrays.fill(grid[i], CellType.WALL);
        }

        // On creuse le chemin à partir de (1,1)
        backtrack(1, 1, grid, width, height);
        
        // On ajoute les briques destructibles à la fin
        addBricks(grid, width, height);

        return grid;
    }

    private void backtrack(int x, int y, CellType[][] grid, int w, int h) {
        grid[y][x] = CellType.EMPTY;

        Integer[] directions = {0, 1, 2, 3}; // North, South, East, West
        List<Integer> list = Arrays.asList(directions);
        Collections.shuffle(list);

        for (int dir : list) {
            int dx = 0, dy = 0;
            if (dir == 0) dy = -2;      // North
            else if (dir == 1) dy = 2;  // South
            else if (dir == 2) dx = 2;  // East
            else if (dir == 3) dx = -2; // West

            int nx = x + dx, ny = y + dy;

            // Vérification des limites et si la case destination est encore un mur
            if (nx > 0 && nx < w - 1 && ny > 0 && ny < h - 1 && grid[ny][nx] == CellType.WALL) {
                grid[y + dy / 2][x + dx / 2] = CellType.EMPTY; // On casse le mur entre les deux
                backtrack(nx, ny, grid, w, h);
            }
        }
    }

    private void addBricks(CellType[][] grid, int w, int h) {
        for (int x = 1; x < w - 1; x++) {
            for (int y = 1; y < h - 1; y++) {
                // On met des briques là où c'est encore WALL (sauf les piliers Bomberman)
                if (grid[y][x] == CellType.WALL && !(x % 2 == 0 && y % 2 == 0)) {
                    if (random.nextFloat() < 0.6) { // 60% de chance d'avoir une brique
                        grid[y][x] = CellType.BRICK;
                    }
                }
            }
        }
    }
}