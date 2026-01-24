package com.chatroom.server

import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.ConcurrentHashMap
import kotlin.concurrent.thread

class ChatServer(private val port: Int, private val display: ServerDisplay) {
    private val clients = ConcurrentHashMap<String, ObjectOutputStream>()
    private val outputStreams = ConcurrentHashMap<Socket, ObjectOutputStream>()
    private var serverSocket: ServerSocket? = null
    @Volatile private var running = false

    fun getPort(): Int = serverSocket?.localPort ?: -1

    fun start() {
        try {
            serverSocket = ServerSocket(port)
            running = true
            display.showMessage("Waiting for clients at $serverSocket\n")

            while (running) {
                val socket = serverSocket?.accept() ?: break
                handleClient(socket)
            }
        } catch (e: Exception) {
            if (running) {
                display.showMessage("Server stopped or error: ${e.message}\n")
            }
        }
    }

    private fun handleClient(socket: Socket) {
        thread {
            var username: String? = null
            var output: ObjectOutputStream? = null
            try {
                // Important: Create ObjectOutputStream first and flush to write header
                output = ObjectOutputStream(socket.getOutputStream())
                output.flush()
                val input = ObjectInputStream(socket.getInputStream())

                // Read username
                val received = input.readObject()
                username = received.toString()

                // Register client
                clients[username] = output
                outputStreams[socket] = output

                // Broadcast user list
                sendToAll("!${clients.keys}")
                display.showMessage("\n$username (${socket.inetAddress.hostAddress}) is online")

                while (true) {
                    val message = input.readObject().toString()

                    if (message.contains("@EE@")) {
                        sendToAll(message)
                    } else {
                        // Private message format: @target: message
                        // We want to send to target: @sender: message
                        val splitIndex = message.indexOf(':')
                        if (splitIndex != -1) {
                            val target = message.substring(1, splitIndex)
                            val content = message.substring(splitIndex) // includes ": "
                            val formattedMsg = "@$username$content"
                            sendPrivately(target, formattedMsg)
                        }
                    }
                }

            } catch (e: Exception) {
                // Connection lost or closed
            } finally {
                username?.let {
                    removeClient(it)
                    removeConnection(socket, it)
                }
                try { socket.close() } catch (ignore: Exception) {}
            }
        }
    }

    private fun sendToAll(data: Any) {
        outputStreams.values.forEach { output ->
            synchronized(output) {
                try {
                    output.writeObject(data)
                    output.flush()
                } catch (e: Exception) {
                    // Handle broken pipe?
                }
            }
        }
    }

    private fun sendPrivately(username: String, message: String) {
        val output = clients[username]
        if (output != null) {
            synchronized(output) {
                try {
                    output.writeObject(message)
                    output.flush()
                } catch (e: Exception) {
                }
            }
        }
    }

    private fun removeClient(username: String) {
        clients.remove(username)
        sendToAll("!${clients.keys}")
    }

    private fun removeConnection(socket: Socket, username: String) {
        outputStreams.remove(socket)
        display.showMessage("\n$username (${socket.inetAddress.hostAddress}) is offline")
    }
}
