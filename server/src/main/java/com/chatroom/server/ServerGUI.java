package com.chatroom.server;

import javax.swing.*;
import java.awt.BorderLayout;

public class ServerGUI extends JFrame implements ServerDisplay {
    private final JTextArea displayWindow = new JTextArea();

    public ServerGUI() {
        super("Server");
        initComponents();
    }

    private void initComponents() {
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(500, 500);
        add(new JScrollPane(displayWindow), BorderLayout.CENTER);
        setVisible(true);
    }

    @Override
    public void showMessage(String message) {
        SwingUtilities.invokeLater(() -> displayWindow.append(message));
    }
}