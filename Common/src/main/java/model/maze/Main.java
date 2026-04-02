package model.maze;

public class Main {
    public static void main(String[] args) {
        int width = 11;
        int height = 15;

        System.out.println("=== Maze Generation Test (Bomberman Style) ===");
        System.out.println("Dimensions: " + width + " x " + height);
        System.out.println("----------------------------------------------");

        // 1. Test de l'algorithme Exhaustif (Backtracking)
        System.out.println("\n[1] Algorithm: EXHAUSTIVE (DFS)");
        CellType[][] maze1 = MazeFactory.createMaze(MazeFactory.Algorithm.EXHAUSTIVE, width, height);
        displayMaze(maze1);

        // 2. Test de l'algorithme de Fusion (Kruskal)
        System.out.println("\n[2] Algorithm: FUSION (Kruskal)");
        CellType[][] maze2 = MazeFactory.createMaze(MazeFactory.Algorithm.RANDOM_FUSION, width, height);
        displayMaze(maze2);
    }

    /**
     * Affiche le labyrinthe dans la console
     * # = Mur fixe, X = Brique, . = Vide
     */
    private static void displayMaze(CellType[][] grid) {
        for (int y = 0; y < grid.length; y++) {
            for (int x = 0; x < grid[0].length; x++) {
                switch (grid[y][x]) {
                    case WALL -> System.out.print("##"); // Mur indestructible
                    case BRICK -> System.out.print("XX"); // Brique destructible
                    case EMPTY -> System.out.print("  "); // Passage
                }
            }
            System.out.println();
        }
    }
}