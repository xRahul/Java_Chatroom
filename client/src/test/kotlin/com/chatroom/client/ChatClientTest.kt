package com.chatroom.client

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.net.ServerSocket
import java.io.ObjectOutputStream
import java.io.ObjectInputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class ChatClientTest {

    @Test
    fun testClientConnectAndReceiveMessage() {
        val serverSocket = ServerSocket(0)
        val port = serverSocket.localPort
        val latch = CountDownLatch(1)
        val receivedMessages = mutableListOf<String>()

        val listener = object : ClientListener {
            override fun onMessageReceived(message: String) {
                receivedMessages.add(message)
                if (message == "Hello Client") {
                    latch.countDown()
                }
            }
            override fun onUserListUpdated(users: List<String>) {}
            override fun onConnectionError(message: String) {}
        }

        val client = ChatClient(listener)

        // Start dummy server
        thread {
            try {
                val socket = serverSocket.accept()
                val out = ObjectOutputStream(socket.getOutputStream())
                out.flush()
                val input = ObjectInputStream(socket.getInputStream())
                val username = input.readObject() // read username

                // Send message
                out.writeObject("@EE@|Hello Client")
                out.flush()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        client.connect("localhost", port, "TestUser")

        assertTrue(latch.await(5, TimeUnit.SECONDS))
        assertTrue(receivedMessages.contains("Hello Client"))

        client.disconnect()
        serverSocket.close()
    }
}
