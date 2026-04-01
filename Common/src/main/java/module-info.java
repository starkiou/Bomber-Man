module Common {
<<<<<<< HEAD
    exports model.maze; // Autorise les autres à voir tes classes de labyrinthe
    exports network.message;
	requires org.json;
=======
    requires org.json;

    exports model.maze;
    exports model.entity;
    exports model.aiPlayer;
    exports model.game;
    exports network.message;
    exports model.logger;
>>>>>>> development
}