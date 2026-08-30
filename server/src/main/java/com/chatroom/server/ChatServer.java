package com.chatroom.server;

import com.chatroom.shared.MessageProtocol;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.concurrent.*;

public class ChatServer {
    private static final int MAX_CLIENTS = 100;

    private final int port;
    private final ServerDisplay display;
    private final ConcurrentHashMap<String, ObjectOutputStream> clients = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<Socket, ObjectOutputStream> outputStreams = new ConcurrentHashMap<>();
    private final ExecutorService pool = Executors.newFixedThreadPool(MAX_CLIENTS);
    private volatile ServerSocket serverSocket;
    private volatile boolean running = false;

    public ChatServer(int port, ServerDisplay display) {
        this.port = port;
        this.display = display;
    }

    public int getPort() {
        return serverSocket != null ? serverSocket.getLocalPort() : -1;
    }

    public void start() {
        try {
            serverSocket = new ServerSocket(port);
            running = true;
            display.showMessage("Waiting for clients at " + serverSocket + "\n");

            while (running) {
                Socket socket = serverSocket.accept();
                if (socket == null) break;
                try {
                    pool.execute(new ClientHandler(this, socket));
                } catch (RejectedExecutionException e) {
                    try { socket.close(); } catch (IOException ignored) {}
                    display.showMessage("Rejected connection (server full): " + socket + "\n");
                }
            }
        } catch (IOException e) {
            if (running) {
                display.showMessage("Server stopped or error: " + e.getMessage() + "\n");
            }
        }
    }

    void stopServer() {
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            // ignore
        }
        pool.shutdownNow();
    }

    void handleClientMessage(String username, Socket socket, String message) {
        if (MessageProtocol.isBroadcast(message)) {
            sendToAll(message);
        } else {
            String target = MessageProtocol.extractPrivateTarget(message);
            if (target != null) {
                int colon = message.indexOf(':');
                if (colon < 0) return;
                String content = message.substring(colon);
                String formattedMsg = "@" + username + content;
                sendPrivately(target, formattedMsg);
            }
        }
    }

    /** @return true if registered, false if username already taken */
    boolean registerClient(String username, Socket socket, ObjectOutputStream output) {
        if (clients.putIfAbsent(username, output) != null) {
            return false;
        }
        outputStreams.put(socket, output);
        display.showMessage("\n" + username + " (" + socket.getInetAddress().getHostAddress() + ") is online");
        broadcastUserList();
        return true;
    }

    void removeClient(String username) {
        clients.remove(username);
        broadcastUserList();
    }

    void removeConnection(Socket socket, String username) {
        outputStreams.remove(socket);
        display.showMessage("\n" + username + " (" + socket.getInetAddress().getHostAddress() + ") is offline");
    }

    void sendToAll(Object data) {
        // snapshot to avoid concurrent modification issues + weakly-consistent iteration
        var snapshot = new ArrayList<>(outputStreams.entrySet());
        for (var entry : snapshot) {
            ObjectOutputStream out = entry.getValue();
            synchronized (out) {
                try {
                    out.writeObject(data);
                    out.flush();
                } catch (IOException e) {
                    // evict broken client — will be cleaned up by ClientHandler finally block,
                    // but remove mapping now so user list stays accurate
                    outputStreams.remove(entry.getKey());
                }
            }
        }
    }

    void sendPrivately(String username, String message) {
        ObjectOutputStream output = clients.get(username);
        if (output != null) {
            synchronized (output) {
                try {
                    output.writeObject(message);
                    output.flush();
                } catch (IOException e) {
                    // client disconnected
                }
            }
        }
    }

    private void broadcastUserList() {
        sendToAll(MessageProtocol.buildUserList(new ArrayList<>(clients.keySet())));
    }
}