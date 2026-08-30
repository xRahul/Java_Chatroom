package com.chatroom.client;

import java.util.List;

public interface ClientListener {
    void onMessageReceived(String message);
    void onUserListUpdated(List<String> users);
    void onConnectionError(String message);
}