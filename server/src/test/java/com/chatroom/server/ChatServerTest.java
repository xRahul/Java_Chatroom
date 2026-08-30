package com.chatroom.server;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.*;

class ChatServerTest {

    private ChatServer server;
    private ServerDisplay mockDisplay;
    private ExecutorService executor;
    private int serverPort;

    @BeforeEach
    void setUp() throws Exception {
        mockDisplay = message -> {};
        server = new ChatServer(0, mockDisplay);
        executor = Executors.newSingleThreadExecutor();
        
        executor.submit(() -> server.start());
        
        // Wait for server to start
        for (int i = 0; i < 20; i++) {
            serverPort = server.getPort();
            if (serverPort > 0) break;
            Thread.sleep(100);
        }
        assertTrue(serverPort > 0, "Server did not start");
    }

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stopServer();
        }
        if (executor != null) {
            executor.shutdownNow();
            try {
                if (!executor.awaitTermination(2, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
            }
        }
    }

    @Test
    @Timeout(5)
    void testServerStartsOnPort() {
        assertTrue(server.getPort() > 0, "Server should be running on a valid port");
    }

    @Test
    @Timeout(10)
    void testClientConnection() throws Exception {
        CountDownLatch latch = new CountDownLatch(1);
        
        try (Socket socket = new Socket("localhost", serverPort);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {
            
            out.writeObject("TestUser");
            out.flush();
            
            Thread reader = new Thread(() -> {
                try {
                    Object received = in.readObject();
                    if (received != null) {
                        latch.countDown();
                    }
                } catch (Exception e) {
                    // ignore
                }
            });
            reader.start();
            reader.join(3000);
            
            assertEquals(0, latch.getCount(), "Should receive response from server");
        }
    }
}
