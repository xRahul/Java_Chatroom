package com.chatroom.client;

import com.chatroom.shared.MessageProtocol;

import java.io.*;
import java.net.Socket;
import java.util.logging.Logger;

public class ChatClient {
    private static final Logger LOGGER = Logger.getLogger(ChatClient.class.getName());

    private final ClientListener listener;
    private volatile Socket socket;
    private volatile ObjectOutputStream output;
    private volatile ObjectInputStream input;
    private volatile boolean connected = false;
    private volatile MessageListener messageListener;
    private volatile Thread listenerThread;
    private String userName = "Anonymous";

    public ChatClient(ClientListener listener) {
        this.listener = listener;
    }

    public void connect(String host, int port, String username) {
        if (connected) {
            return;
        }
        this.userName = username;

        try {
            socket = new Socket(host, port);
            output = new ObjectOutputStream(socket.getOutputStream());
            output.flush();
            input = new ObjectInputStream(socket.getInputStream());
            input.setObjectInputFilter(
                java.io.ObjectInputFilter.Config.createFilter("java.lang.String;!*"));

            // Send username
            output.writeObject(userName);
            output.flush();

            connected = true;
            messageListener = new MessageListener(input, listener, this);
            listenerThread = new Thread(messageListener, "MessageListener-" + userName);
            listenerThread.start();
        } catch (IOException e) {
            connected = false;
            listener.onConnectionError("Could not connect: " + e.getMessage());
        }
    }

    public void send(String message) {
        if (!connected || output == null) {
            return;
        }

        try {
            String writeStr;
            if (message.startsWith(MessageProtocol.PREFIX_PRIVATE)) {
                listener.onMessageReceived(userName + ": " + message);
                writeStr = message;
            } else {
                writeStr = MessageProtocol.PREFIX_BROADCAST + userName + ": " + message;
            }
            synchronized (output) {
                output.writeObject(writeStr);
                output.flush();
            }
        } catch (IOException e) {
            listener.onConnectionError("Failed to send message");
        }
    }

    public void disconnect() {
        connected = false;
        if (messageListener != null) {
            messageListener.stop();
        }
        closeQuietly();
    }

    void onListenerDisconnect() {
        connected = false;
        closeQuietly();
    }

    private void closeQuietly() {
        try { if (output != null) output.close(); } catch (IOException e) {}
        try { if (input != null) input.close(); } catch (IOException e) {}
        try { if (socket != null && !socket.isClosed()) socket.close(); } catch (IOException e) {}
    }
}