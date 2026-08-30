package com.chatroom.client;

import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;

public class ClientGUI implements ClientListener {
    private final ChatClient client = new ChatClient(this);

    private final JFrame mainWindow = new JFrame();
    private final DefaultListModel<String> userListModel = new DefaultListModel<>();
    private final JList<String> userOnlineList = new JList<>(userListModel);
    private final JTextArea displayText = new JTextArea();
    private final JTextArea typeText = new JTextArea();
    private final JButton submitButton = new JButton("SEND");
    private final JFrame logInWindow = new JFrame("Log In");
    private final JTextField usernameField = new JTextField(20);

    public void start() {
        buildLogInWindow();
    }

    private void buildLogInWindow() {
        logInWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        logInWindow.setLayout(new FlowLayout());
        logInWindow.add(new JLabel("Enter Username: "));
        logInWindow.add(usernameField);

        JButton enterButton = new JButton("Enter");
        logInWindow.add(enterButton);

        enterButton.addActionListener(e -> login());
        usernameField.addActionListener(e -> login());

        logInWindow.pack();
        logInWindow.setLocationRelativeTo(null);
        logInWindow.setVisible(true);
    }

    private void login() {
        String username = usernameField.getText().trim();
        if (username.isEmpty()) {
            JOptionPane.showMessageDialog(logInWindow, "Please Enter a name!");
            return;
        }
        logInWindow.dispose();
        buildMainWindow(username);
        client.connect("localhost", 5555, username);
    }

    private void buildMainWindow(String username) {
        mainWindow.setTitle("Project ChatRoom - " + username);
        mainWindow.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);

        mainWindow.addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                int result = JOptionPane.showConfirmDialog(mainWindow, "Are you sure you want to exit?", "Confirm", JOptionPane.YES_NO_OPTION);
                if (result == JOptionPane.YES_OPTION) {
                    client.disconnect();
                    System.exit(0);
                }
            }
        });

        JPanel mainPanel = new JPanel(new BorderLayout(5, 5));

        // Top Bar (Themes)
        JPanel topBar = new JPanel(new BorderLayout());
        JLabel statusLabel = new JLabel("Online");
        topBar.add(statusLabel, BorderLayout.WEST);

        JComboBox<String> themeChooser = new JComboBox<>();
        UIManager.LookAndFeelInfo[] themes = UIManager.getInstalledLookAndFeels();
        for (UIManager.LookAndFeelInfo theme : themes) {
            themeChooser.addItem(theme.getName());
        }
        themeChooser.addActionListener(e -> {
            int index = themeChooser.getSelectedIndex();
            if (index >= 0) {
                try {
                    UIManager.setLookAndFeel(themes[index].getClassName());
                    SwingUtilities.updateComponentTreeUI(mainWindow);
                } catch (Exception ignored) {}
            }
        });
        topBar.add(themeChooser, BorderLayout.EAST);
        mainPanel.add(topBar, BorderLayout.NORTH);

        // Center (Chat)
        displayText.setEditable(false);
        displayText.setLineWrap(true);
        mainPanel.add(new JScrollPane(displayText), BorderLayout.CENTER);

        // Bottom (Input)
        JPanel bottomPanel = new JPanel(new BorderLayout(5, 5));
        typeText.setRows(3);
        bottomPanel.add(new JScrollPane(typeText), BorderLayout.CENTER);
        submitButton.addActionListener(e -> submitMessage());
        bottomPanel.add(submitButton, BorderLayout.EAST);
        mainPanel.add(bottomPanel, BorderLayout.SOUTH);

        // East (Users)
        userOnlineList.setFixedCellWidth(100);
        userOnlineList.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                if (e.getClickCount() == 2) {
                    String selected = userOnlineList.getSelectedValue();
                    if (selected != null) {
                        typeText.setText("@" + selected + ": ");
                        typeText.requestFocusInWindow();
                    }
                }
            }
        });
        mainPanel.add(new JScrollPane(userOnlineList), BorderLayout.EAST);

        mainWindow.setContentPane(mainPanel);
        mainWindow.setMinimumSize(new Dimension(500, 300));
        mainWindow.pack();
        mainWindow.setLocationRelativeTo(null);
        mainWindow.setVisible(true);
    }

    private void submitMessage() {
        String text = typeText.getText();
        if (!text.isEmpty()) {
            client.send(text);
            typeText.setText("");
            typeText.requestFocusInWindow();
        }
    }

    @Override
    public void onMessageReceived(String message) {
        SwingUtilities.invokeLater(() -> {
            displayText.append("\n" + message);
            displayText.setCaretPosition(displayText.getDocument().getLength());
        });
    }

    @Override
    public void onUserListUpdated(List<String> users) {
        SwingUtilities.invokeLater(() -> {
            userListModel.clear();
            users.stream().sorted().forEach(userListModel::addElement);
        });
    }

    @Override
    public void onConnectionError(String message) {
        SwingUtilities.invokeLater(() -> {
            if (message.contains("Connection lost")) {
                JOptionPane.showMessageDialog(mainWindow, "Server Not Responding");
                System.exit(0);
            } else {
                JOptionPane.showMessageDialog(mainWindow, message);
            }
        });
    }
}