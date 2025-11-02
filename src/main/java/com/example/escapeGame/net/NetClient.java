package com.example.escapeGame.net;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import javafx.application.Platform;

import java.io.*;
import java.net.Socket;
import java.util.function.Consumer;

public class NetClient implements Closeable {
    private final String host;
    private final int port;
    private final Gson gson = new Gson();
    private Socket socket;
    private PrintWriter out;
    private Thread readerThread;
    private Consumer<JsonObject> onMessage;
    private Consumer<String> onError;

    public NetClient(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public void setOnMessage(Consumer<JsonObject> onMessage) { this.onMessage = onMessage; }
    public void setOnError(Consumer<String> onError) { this.onError = onError; }

    public void connect() throws IOException {
        socket = new Socket(host, port);
        out = new PrintWriter(new OutputStreamWriter(socket.getOutputStream()), true);
        readerThread = new Thread(this::readLoop, "net-reader");
        readerThread.setDaemon(true);
        readerThread.start();
    }

    private void readLoop() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            String line;
            while ((line = br.readLine()) != null) {
                try {
                    JsonObject msg = gson.fromJson(line, JsonObject.class);
                    if (onMessage != null) {
                        Platform.runLater(() -> onMessage.accept(msg));
                    }
                } catch (Exception ex) {
                    System.err.println("Failed to parse server message: " + line);
                    ex.printStackTrace();
                    if (onError != null) onError.accept("Failed to parse server message: " + ex.getMessage());
                }
            }
        } catch (IOException e) {
            if (onError != null) onError.accept("Disconnected: " + e.getMessage());
        }
    }

    public void send(JsonObject obj) {
        if (out != null) out.println(gson.toJson(obj));
    }

    public void sendType(String type) {
        JsonObject o = new JsonObject();
        o.addProperty("type", type);
        send(o);
    }

    @Override
    public void close() throws IOException {
        try { if (socket != null) socket.close(); } finally { socket = null; }
    }
}


