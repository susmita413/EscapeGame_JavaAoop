package com.example.escapeGame.server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class ServerMain {
    public static void main(String[] args) throws IOException {
        int port = 9090;
        if (args.length > 0) {
            try {
                port = Integer.parseInt(args[0]);
            } catch (NumberFormatException ignored) {
            }
        }
        System.out.println("Multiplayer Server starting on port " + port);
        RoomDirectory roomDirectory = new RoomDirectory();
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client connected: " + socket.getRemoteSocketAddress());
                ClientHandler handler = new ClientHandler(socket, roomDirectory);
                Thread t = new Thread(handler, "client-" + socket.getPort());
                t.setDaemon(true);
                t.start();
            }
        }
    }
}


