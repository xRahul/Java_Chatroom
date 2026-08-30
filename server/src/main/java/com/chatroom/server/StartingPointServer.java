package com.chatroom.server;

import java.io.IOException;

public class StartingPointServer {
    public static void main(String[] args) throws IOException {
        ServerDisplay display = new ServerGUI();
        ChatServer server = new ChatServer(5555, display);
        server.start();
    }
}