package com.chatroom.server;

import java.io.*;
import java.net.*;
import java.util.logging.Logger;

public class ClientHandler implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(ClientHandler.class.getName());

    private final ChatServer server;
    private final Socket socket;
    private ObjectInputStream input;
    private ObjectOutputStream output;
    private String username;

    public ClientHandler(ChatServer server, Socket socket) {
        this.server = server;
        this.socket = socket;
    }

    @Override
    public void run() {
        boolean registered = false;
        try {
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            input = new ObjectInputStream(socket.getInputStream());
            input.setObjectInputFilter(
                java.io.ObjectInputFilter.Config.createFilter("java.lang.String;!*"));

            username = (String) input.readObject();
            if (username == null || username.trim().isEmpty()) {
                LOGGER.warning("Client provided empty username, closing");
                return;
            }
            username = username.trim();
            if (!username.matches("[A-Za-z0-9_\\-]{1,32}")) {
                LOGGER.warning("Invalid username format: " + username);
                return;
            }

            if (!server.registerClient(username, socket, output)) {
                LOGGER.warning("Duplicate username rejected: " + username);
                try {
                    synchronized (output) {
                        output.writeObject("!ERROR: username already taken");
                        output.flush();
                    }
                } catch (IOException ignored) {}
                username = null; // prevent finally from removing the legitimate user
                return;
            }
            registered = true;

            while (true) {
                Object received = input.readObject();
                if (received == null) break;
                String message = received.toString();
                server.handleClientMessage(username, socket, message);
            }
        } catch (EOFException | SocketException e) {
            // normal client disconnect
        } catch (IOException e) {
            LOGGER.info("Client " + username + " disconnected: " + e.getMessage());
        } catch (ClassNotFoundException e) {
            LOGGER.warning("Invalid object received from " + username);
        } finally {
            if (registered && username != null) {
                server.removeClient(username);
            }
            server.removeConnection(socket, username != null ? username : "unknown");
            closeQuietly();
        }
    }

    private void closeQuietly() {
        try { if (output != null) output.close(); } catch (IOException e) {}
        try { if (input != null) input.close(); } catch (IOException e) {}
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException e) {}
    }
}