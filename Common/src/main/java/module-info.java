module Common {
    exports model.maze; // Autorise les autres à voir tes classes de labyrinthe
    exports network.message;
    exports model.logger;
    exports model.game;
    exports model.entity;
    exports model.aiPlayer;
    requires org.json;
}