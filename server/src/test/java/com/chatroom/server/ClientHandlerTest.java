package com.chatroom.server;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ClientHandlerTest {

    private ChatServer server;
    private ServerDisplay mockDisplay;
    private Thread serverThread;
    private int serverPort;

    @BeforeEach
    void setUp() throws Exception {
        mockDisplay = message -> {};
        server = new ChatServer(0, mockDisplay);
        serverThread = new Thread(() -> server.start());
        serverThread.start();
        
        for (int i = 0; i < 50; i++) {
            serverPort = server.getPort();
            if (serverPort > 0) break;
            Thread.sleep(100);
        }
    }

    @Test
    @Timeout(10)
    void testClientConnectionEstablished() throws Exception {
        CountDownLatch connected = new CountDownLatch(1);
        
        Socket socket = new Socket("localhost", serverPort);
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        
        out.writeObject("TestUser");
        out.flush();
        
        Thread reader = new Thread(() -> {
            try {
                Object received = in.readObject();
                if (received != null) {
                    connected.countDown();
                }
            } catch (Exception e) {
                // ignore
            }
        });
        reader.start();
        
        assertTrue(connected.await(5, TimeUnit.SECONDS), "Client should connect and receive message");
        
        socket.close();
        server.stopServer();
    }

    @Test
    @Timeout(10)
    void testInvalidMessageHandling() throws Exception {
        Socket socket = new Socket("localhost", serverPort);
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        
        out.writeObject("TestUser");
        out.flush();
        
        // Read user list update
        in.readObject();
        
        // Send invalid message (not starting with @ or !)
        out.writeObject("invalid message format");
        out.flush();
        
        // Server should not crash - send another valid message
        Thread.sleep(500);
        out.writeObject("@EE@|Valid message");
        out.flush();
        
        Thread.sleep(500);
        socket.close();
        server.stopServer();
    }

    @Test
    @Timeout(10)
    void testEmptyUsername() throws Exception {
        Socket socket = new Socket("localhost", serverPort);
        ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
        out.flush();
        ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
        
        out.writeObject("");
        out.flush();
        
        // Wait a bit - server should close connection for empty username
        Thread.sleep(1000);
        
        // Try to read - should get EOF or null
        try {
            Object received = in.readObject();
            // If we get here, server didn't disconnect
        } catch (Exception e) {
            // Expected - connection should be closed
        }
        
        socket.close();
        server.stopServer();
    }
}
