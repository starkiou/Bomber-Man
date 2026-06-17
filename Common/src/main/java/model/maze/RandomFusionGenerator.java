package model.maze;

import java.util.*;

public class RandomFusionGenerator implements MazeGenerator {
    
    // Classe interne pour gérer les ensembles (Union-Find)
    private static class SetCell {
        int x, y;
        SetCell parent;
        SetCell(int x, int y) { this.x = x; this.y = y; this.parent = this; }
        SetCell find() {
            if (parent == this) return this;
            return parent = parent.find();
        }
        void union(SetCell other) {
            SetCell root1 = this.find();
            SetCell root2 = other.find();
            if (root1 != root2) root1.parent = root2;
        }
    }

    @Override
    public CellType[][] generate(int width, int height) {
        CellType[][] grid = new CellType[height][width];
        for (int i = 0; i < height; i++) Arrays.fill(grid[i], CellType.WALL);

        List<int[]> walls = new ArrayList<>();
        SetCell[][] sets = new SetCell[width][height];

        // On initialise les cellules de passage (coordonnées impaires)
        for (int x = 1; x < width; x += 2) {
            for (int y = 1; y < height; y += 2) {
                grid[y][x] = CellType.EMPTY;
                sets[x][y] = new SetCell(x, y);
                // On ajoute les murs potentiellement cassables à une liste
                if (x + 2 < width) walls.add(new int[]{x + 1, y, x, y, x + 2, y});
                if (y + 2 < height) walls.add(new int[]{x, y + 1, x, y, x, y + 2});
            }
        }

        Collections.shuffle(walls);

        for (int[] w : walls) {
            SetCell s1 = sets[w[2]][w[3]].find();
            SetCell s2 = sets[w[4]][w[5]].find();

            if (s1 != s2) {
                grid[w[1]][w[0]] = CellType.EMPTY; // On casse le mur
                s1.union(s2);
            }
        }

        // On ajoute les briques comme dans l'autre algo
        fillWithBricks(grid, width, height);
        return grid;
    }

    private void fillWithBricks(CellType[][] grid, int w, int h) {
        Random r = new Random();
        for (int x = 1; x < w - 1; x++) {
            for (int y = 1; y < h - 1; y++) {
                if (grid[y][x] == CellType.WALL && !(x % 2 == 0 && y % 2 == 0)) {
                    if (r.nextFloat() < 0.6) grid[y][x] = CellType.BRICK;
                }
            }
        }
    }
}