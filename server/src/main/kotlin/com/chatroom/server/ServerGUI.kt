package com.chatroom.server

import javax.swing.*
import java.awt.BorderLayout

class ServerGUI : JFrame("Server"), ServerDisplay {
    private val displayWindow: JTextArea = JTextArea()

    init {
        defaultCloseOperation = EXIT_ON_CLOSE
        setSize(500, 500)
        add(JScrollPane(displayWindow), BorderLayout.CENTER)
        isVisible = true
    }

    override fun showMessage(message: String) {
        SwingUtilities.invokeLater {
            displayWindow.append(message)
        }
    }
}
