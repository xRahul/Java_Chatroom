package com.chatroom.client

import java.io.ObjectInputStream
import java.io.ObjectOutputStream
import java.net.Socket
import kotlin.concurrent.thread

class ChatClient(private val listener: ClientListener) {
    private var socket: Socket? = null
    private var output: ObjectOutputStream? = null
    private var input: ObjectInputStream? = null
    @Volatile private var running = false
    var userName: String = "Anonymous"

    fun connect(host: String, port: Int, username: String) {
        this.userName = username
        thread {
            try {
                socket = Socket(host, port)
                output = ObjectOutputStream(socket?.getOutputStream())
                output?.flush()
                input = ObjectInputStream(socket?.getInputStream())

                // Send username
                output?.writeObject(username)
                output?.flush()

                running = true
                startListening()
            } catch (e: Exception) {
                listener.onConnectionError("Could not connect: ${e.message}")
            }
        }
    }

    private fun startListening() {
        try {
            while (running) {
                val messageObj = input?.readObject() ?: break
                val message = messageObj.toString()

                if (message.startsWith("!")) {
                    // User list update: ![User1, User2]
                    val usersStr = message.substring(1) // [User1, User2]
                    val users = usersStr.removePrefix("[").removeSuffix("]").split(", ")
                        .filter { it.isNotBlank() }
                    listener.onUserListUpdated(users)
                } else {
                     if (message.startsWith("@EE@|")) {
                         listener.onMessageReceived(message.substring(5))
                     } else if (message.startsWith("@")) {
                         listener.onMessageReceived(message.substring(1))
                     } else {
                         // Fallback
                         listener.onMessageReceived(message)
                     }
                }
            }
        } catch (e: Exception) {
            if (running) {
                listener.onConnectionError("Connection lost")
            }
        } finally {
            disconnect()
        }
    }

    fun send(message: String) {
        if (!running) return

        try {
            val writeStr = if (message.startsWith("@")) {
                 listener.onMessageReceived("$userName: $message")
                 message
            } else {
                 "@EE@|$userName: $message"
            }

            output?.writeObject(writeStr)
            output?.flush()
        } catch (e: Exception) {
            listener.onConnectionError("Failed to send message")
        }
    }

    fun disconnect() {
        running = false
        try {
            output?.close()
            input?.close()
            socket?.close()
        } catch (e: Exception) {}
    }
}
