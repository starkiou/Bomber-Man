package org.example;

public class Main {



    public static void main(String[] args) {
        LogManager.getInstance().info("Démarrage du serveur sur le port 3000");
        ServerManager serverManager = new ServerManager(3000);

    }
    
}
