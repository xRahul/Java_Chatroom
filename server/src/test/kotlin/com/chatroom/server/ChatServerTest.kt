package com.chatroom.server

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*
import java.net.Socket
import java.io.ObjectOutputStream
import java.io.ObjectInputStream
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.concurrent.thread

class ChatServerTest {

    @Test
    fun testServerBroadcast() {
        val display = object : ServerDisplay {
            override fun showMessage(message: String) {}
        }
        val server = ChatServer(0, display)

        val serverThread = thread {
            server.start()
        }

        // Wait for server to be ready
        var port = -1
        for (i in 0..20) {
            port = server.getPort()
            if (port != -1 && port != 0) break
            Thread.sleep(100)
        }
        assertNotEquals(-1, port)
        assertNotEquals(0, port)

        val latch = CountDownLatch(1)

        // Client 1
        thread {
            try {
                val socket = Socket("localhost", port)
                val out = ObjectOutputStream(socket.getOutputStream())
                out.flush()
                val input = ObjectInputStream(socket.getInputStream())

                // Send username
                out.writeObject("User1")
                out.flush()

                // Wait for broadcast
                while (true) {
                    val msg = input.readObject().toString()
                    if (msg.contains("Hello World")) {
                        latch.countDown()
                        break
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Client 2 (Sender)
        thread {
             Thread.sleep(1000) // wait for Client 1 to connect
             try {
                val socket = Socket("localhost", port)
                val out = ObjectOutputStream(socket.getOutputStream())
                out.flush()
                val input = ObjectInputStream(socket.getInputStream())

                out.writeObject("User2")
                out.flush()

                out.writeObject("@EE@|Hello World")
                out.flush()
             } catch (e: Exception) {
                e.printStackTrace()
             }
        }

        assertTrue(latch.await(10, TimeUnit.SECONDS))
    }
}
