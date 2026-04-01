package org.example;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.ByteBuffer;

import network.message.ConnectionMessage;
import network.message.GetRoomListUpdateMessage;
import network.message.Message;
import network.message.MessageSerializer;
import network.message.MessageType;
import network.message.RoomCreationMessage;
import network.message.RoomInfoDTO;
import network.message.RoomJoiningMessage;
import network.message.RoomListUpdateMessage;

/**
 * Test manuel : lance le serveur (Main.java) AVANT d'exécuter ce test.
 *
 * Ce que ce test fait pas à pas :
 *   1. Connexion TCP au serveur (port 3000)
 *   2. Envoi d'un ConnectionMessage  → le serveur enregistre le pseudo
 *   3. Envoi d'un RoomCreationMessage → le serveur crée un salon avec maxPlayer=4
 *   4. Envoi d'un GetRoomMessage      → le serveur renvoie la liste des salons
 *   5. Lecture de la réponse          → affichage de chaque salon reçu
 */
public class TestServer {

    private static final String HOST    = "localhost";
    private static final int    PORT    = 3000;
    private static final int    TIMEOUT = 3000;

    public static void main(String[] args) throws Exception {
    	System.out.println("=== LANCEMENT SERVEUR ===\n");
    	Main.main(args);

        System.out.println("=== TEST CLIENT BOMBERMAN ===\n");

        try (Socket socket = new Socket(HOST, PORT)) {
            OutputStream out = socket.getOutputStream();
            InputStream  in  = socket.getInputStream();
            MessageSerializer serializer = new MessageSerializer();

            // ----------------------------------------------------------------
            // ÉTAPE 1 : connexion avec pseudo
            // ----------------------------------------------------------------
            System.out.println("[1] Envoi ConnectionMessage (pseudo='TestBot')...");
            send(out, serializer, new ConnectionMessage("TestBot"));
            sleep(200);

            // ----------------------------------------------------------------
            // ÉTAPE 2 : création d'un salon
            // ----------------------------------------------------------------
            System.out.println("[2] Envoi RoomCreationMessage (maxPlayer=4, name='SalonToto')...");
            send(out, serializer, new RoomCreationMessage(4, "Salon Toto"));
            sleep(300);

            // ----------------------------------------------------------------
            // ÉTAPE 3 : demande de la liste des salons
            // ----------------------------------------------------------------
            System.out.println("[3] Envoi GetRoomMessage...");
            send(out, serializer, new GetRoomListUpdateMessage());

            // ----------------------------------------------------------------
            // ÉTAPE 4 : lecture de la réponse
            // ----------------------------------------------------------------
            System.out.println("[4] Attente de la réponse du serveur...\n");
            socket.setSoTimeout(TIMEOUT);

            try {
                Message response = readOneMessage(in, serializer);

                if (response == null) {
                    System.out.println("ERREUR : réponse nulle (désérialisation échouée).");
                    return;
                }

                if (response.getMessageType() != MessageType.ROOM_LIST_UPDATE) {
                    System.out.println("ERREUR : type inattendu reçu → " + response.getMessageType());
                    return;
                }

                // ----------------------------------------------------------------
                // ÉTAPE 5 : affichage des salons
                // ----------------------------------------------------------------
                RoomListUpdateMessage listMsg = (RoomListUpdateMessage) response;
                System.out.println("✅ Liste des salons reçue (" + listMsg.getRooms().size() + " salon(s)) :\n");

                for (RoomInfoDTO room : listMsg.getRooms()) {
                    String statut = room.isInGame()  ? "EN COURS" :
                                    room.isFull()    ? "PLEIN"    : "EN ATTENTE";
                    System.out.printf("  %s %s %d/%d joueurs  [%s]%n",
                        room.getRoomId(),
                        room.getRoomName(),
                        room.getCurrentPlayers(),
                        room.getMaxPlayers(),
                        statut);
                }


            } catch (java.net.SocketTimeoutException e) {
                System.out.println("ERREUR : timeout — le serveur n'a pas répondu dans les " + TIMEOUT + "ms.");
                System.out.println("Vérifie que le case GET_ROOM_LIST_UPDATE dans ServerMessageHandler envoie bien un RoomListUpdateMessage.");
            }
        }
        
        try (Socket socket = new Socket(HOST, PORT)) {
            OutputStream out = socket.getOutputStream();
            InputStream  in  = socket.getInputStream();
            MessageSerializer serializer = new MessageSerializer();

            // ----------------------------------------------------------------
            // ÉTAPE 6 : connexion a une room pseudo
            // ----------------------------------------------------------------
            System.out.println("[6] Envoi ConnectionMessage (pseudo='TestBot2')...");
            send(out, serializer, new ConnectionMessage("TestBot2"));
            sleep(200);
            
            System.out.println("[7] Envoi RoomJoiningMessage (roomId=1)...");
            send(out, serializer, new RoomJoiningMessage(1));
            sleep(300);
            
         // ----------------------------------------------------------------
            // ÉTAPE 7 : affichage des salons
            // ----------------------------------------------------------------
            send(out, serializer, new GetRoomListUpdateMessage());
            Message response = readOneMessage(in, serializer);
            RoomListUpdateMessage listMsg = (RoomListUpdateMessage) response;
            System.out.println("✅ Liste des salons reçue (" + listMsg.getRooms().size() + " salon(s)) :\n");

            for (RoomInfoDTO room : listMsg.getRooms()) {
                String statut = room.isInGame()  ? "EN COURS" :
                                room.isFull()    ? "PLEIN"    : "EN ATTENTE";
                System.out.printf("  %s %s %d/%d joueurs  [%s]%n",
                    room.getRoomId(),
                    room.getRoomName(),
                    room.getCurrentPlayers(),
                    room.getMaxPlayers(),
                    statut);
            }

            } catch (java.net.SocketTimeoutException e) {
                System.out.println("ERREUR : timeout — le serveur n'a pas répondu dans les " + TIMEOUT + "ms.");
                System.out.println("Vérifie que le case GET_ROOM_LIST_UPDATE dans ServerMessageHandler envoie bien un RoomListUpdateMessage.");
            }
        
    
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static void send(OutputStream out, MessageSerializer s, Message msg) throws IOException {
        byte[] data = s.serialize(msg);
        out.write(data);
        out.flush();
        System.out.println("    → envoyé : " + msg.getMessageType());
    }

    private static Message readOneMessage(InputStream in, MessageSerializer s) throws IOException {
        int typeByte = in.read();
        if (typeByte == -1) return null;

        byte[] lenBytes = in.readNBytes(4);
        if (lenBytes.length < 4) return null;

        int length = ByteBuffer.wrap(lenBytes).getInt();
        byte[] data = in.readNBytes(length);
        if (data.length < length) return null;

        ByteBuffer buf = ByteBuffer.allocate(1 + 4 + length);
        buf.put((byte) typeByte);
        buf.putInt(length);
        buf.put(data);

        return s.deserialize(buf.array());
    }

    private static void sleep(int ms) {
        try { Thread.sleep(ms); } catch (InterruptedException ignored) {}
    }
}