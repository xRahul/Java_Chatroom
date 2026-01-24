package com.chatroom.client

import javax.swing.*
import javax.swing.border.*
import java.awt.*
import java.awt.event.*

class ClientGUI : ClientListener {
    private val client = ChatClient(this)

    // UI Components
    private val mainWindow = JFrame()
    private val userListModel = DefaultListModel<String>()
    private val userOnlineList = JList(userListModel)
    private val displayText = JTextArea()
    private val typeText = JTextArea()
    private val submitButton = JButton("SEND")
    private val logInWindow = JFrame("Log In")
    private val usernameField = JTextField(20)

    fun start() {
        buildLogInWindow()
    }

    private fun buildLogInWindow() {
        logInWindow.apply {
            defaultCloseOperation = JFrame.EXIT_ON_CLOSE
            layout = FlowLayout()
            add(JLabel("Enter Username: "))
            add(usernameField)

            val enterButton = JButton("Enter")
            add(enterButton)

            enterButton.addActionListener { login() }
            usernameField.addActionListener { login() }

            pack()
            setLocationRelativeTo(null)
            isVisible = true
        }
    }

    private fun login() {
        val username = usernameField.text.trim()
        if (username.isNotEmpty()) {
            logInWindow.dispose()
            buildMainWindow(username)
            client.connect("localhost", 5555, username)
        } else {
            JOptionPane.showMessageDialog(logInWindow, "Please Enter a name!")
        }
    }

    private fun buildMainWindow(username: String) {
        mainWindow.title = "Project ChatRoom - $username"
        mainWindow.defaultCloseOperation = JFrame.DO_NOTHING_ON_CLOSE

        mainWindow.addWindowListener(object : WindowAdapter() {
            override fun windowClosing(e: WindowEvent) {
                val result = JOptionPane.showConfirmDialog(mainWindow, "Are you sure you want to exit?", "Confirm", JOptionPane.YES_NO_OPTION)
                if (result == JOptionPane.YES_OPTION) {
                    client.disconnect()
                    System.exit(0)
                }
            }
        })

        // Setup UI
        val mainPanel = JPanel(BorderLayout(5, 5))

        // Top Bar (Themes)
        val topBar = JPanel(BorderLayout())
        val statusLabel = JLabel("Online")
        topBar.add(statusLabel, BorderLayout.WEST)

        val themes = UIManager.getInstalledLookAndFeels()
        val themeNames = themes.map { it.name }.toTypedArray()
        val themeChooser = JComboBox(themeNames)
        themeChooser.addActionListener {
            val index = themeChooser.selectedIndex
            try {
                UIManager.setLookAndFeel(themes[index].className)
                SwingUtilities.updateComponentTreeUI(mainWindow)
            } catch (e: Exception) {}
        }
        topBar.add(themeChooser, BorderLayout.EAST)
        mainPanel.add(topBar, BorderLayout.NORTH)

        // Center (Chat)
        displayText.isEditable = false
        displayText.lineWrap = true
        mainPanel.add(JScrollPane(displayText), BorderLayout.CENTER)

        // Bottom (Input)
        val bottomPanel = JPanel(BorderLayout(5, 5))
        typeText.rows = 3
        bottomPanel.add(JScrollPane(typeText), BorderLayout.CENTER)

        submitButton.addActionListener { submitMessage() }
        bottomPanel.add(submitButton, BorderLayout.EAST)
        mainPanel.add(bottomPanel, BorderLayout.SOUTH)

        // East (Users)
        userOnlineList.fixedCellWidth = 100
        userOnlineList.addMouseListener(object : MouseAdapter() {
            override fun mouseClicked(e: MouseEvent) {
                if (e.clickCount == 2) {
                    val selected = userOnlineList.selectedValue
                    if (selected != null) {
                        typeText.text = "@$selected: "
                        typeText.requestFocusInWindow()
                    }
                }
            }
        })
        mainPanel.add(JScrollPane(userOnlineList), BorderLayout.EAST)

        mainWindow.contentPane = mainPanel
        mainWindow.minimumSize = Dimension(500, 300)
        mainWindow.pack()
        mainWindow.setLocationRelativeTo(null)
        mainWindow.isVisible = true
    }

    private fun submitMessage() {
        val text = typeText.text
        if (text.isNotEmpty()) {
            client.send(text)
            typeText.text = ""
            typeText.requestFocusInWindow()
        }
    }

    override fun onMessageReceived(message: String) {
        SwingUtilities.invokeLater {
            displayText.append("\n$message")
            displayText.caretPosition = displayText.document.length
        }
    }

    override fun onUserListUpdated(users: List<String>) {
        SwingUtilities.invokeLater {
            userListModel.clear()
            users.sorted().forEach { userListModel.addElement(it) }
        }
    }

    override fun onConnectionError(message: String) {
        SwingUtilities.invokeLater {
            if (message.contains("Connection lost")) {
                 JOptionPane.showMessageDialog(mainWindow, "Server Not Responding")
                 System.exit(0)
            } else {
                 JOptionPane.showMessageDialog(mainWindow, message)
            }
        }
    }
}
