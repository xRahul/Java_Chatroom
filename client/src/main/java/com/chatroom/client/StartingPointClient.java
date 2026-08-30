package com.chatroom.client;

import javax.swing.*;

public class StartingPointClient {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            ClientGUI gui = new ClientGUI();
            gui.start();
        });
    }
}