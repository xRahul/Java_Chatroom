package com.chatroom.client;

import com.chatroom.shared.MessageProtocol;

import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.net.SocketException;
import java.util.logging.Logger;

public class MessageListener implements Runnable {
    private static final Logger LOGGER = Logger.getLogger(MessageListener.class.getName());

    private final ObjectInputStream input;
    private final ClientListener listener;
    private final ChatClient client;
    private volatile boolean running = true;

    public MessageListener(ObjectInputStream input, ClientListener listener, ChatClient client) {
        this.input = input;
        this.listener = listener;
        this.client = client;
    }

    @Override
    public void run() {
        while (running) {
            try {
                Object received = input.readObject();
                if (received == null) {
                    handleDisconnect();
                    return;
                }
                String message = received.toString();
                dispatch(message);
            } catch (EOFException | SocketException e) {
                if (running) {
                    listener.onConnectionError("Connection lost");
                }
                client.onListenerDisconnect();
                return;
            } catch (IOException e) {
                if (running) {
                    LOGGER.warning("Network error: " + e.getMessage());
                }
                handleDisconnect();
                return;
            } catch (ClassNotFoundException e) {
                LOGGER.warning("Invalid class received from server");
            }
        }
    }

    private void dispatch(String message) {
        if (MessageProtocol.isUserList(message)) {
            listener.onUserListUpdated(MessageProtocol.parseUserList(message));
        } else if (MessageProtocol.isBroadcast(message)) {
            listener.onMessageReceived(MessageProtocol.extractBroadcastContent(message));
        } else if (MessageProtocol.isPrivate(message)) {
            String content = message.length() > 1 ? message.substring(1) : message;
            listener.onMessageReceived(content);
        } else {
            listener.onMessageReceived(message);
        }
    }

    private void handleDisconnect() {
        if (running) {
            listener.onConnectionError("Connection lost");
        }
        client.onListenerDisconnect();
    }

    public void stop() {
        running = false;
    }
}