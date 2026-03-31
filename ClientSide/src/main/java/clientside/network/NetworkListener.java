package clientside.network;

import java.io.ObjectInputStream;
import java.net.Socket;

public class NetworkListener implements Runnable {
    private final Socket socket;
    private boolean running = true;

    public NetworkListener(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            while (running) {
                // On attend un message du serveur (bloquant)
                Object message = in.readObject();

                handleMessage(message); //On traite le mess dedans
            }
        } catch (Exception e) {
            System.err.println("Connexion perdue avec le serveur.");
            e.printStackTrace();
        }
    }

    private void handleMessage(Object msg) {
        //TODO logique de redirection vers les controleurs pour l'affichage des infos recues

        System.out.println("Message reçu du serveur : " + msg);
    }

    public void stop() {
        this.running = false;
    }
}