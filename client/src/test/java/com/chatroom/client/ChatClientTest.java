package com.chatroom.client;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ChatClientTest {

    private ServerSocket serverSocket;
    private int port;
    private volatile boolean serverRunning = true;

    @BeforeEach
    void setUp() throws Exception {
        serverSocket = new ServerSocket(0);
        port = serverSocket.getLocalPort();
    }

    @AfterEach
    void tearDown() throws IOException {
        serverRunning = false;
        serverSocket.close();
    }

    @Test
    @Timeout(10)
    void testConnectToServer() throws Exception {
        CountDownLatch connected = new CountDownLatch(1);
        AtomicReference<List<String>> userList = new AtomicReference<>();
        
        // Start mock server
        new Thread(() -> {
            try {
                Socket socket = serverSocket.accept();
                ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
                out.flush();
                ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
                in.readObject(); // Read username
                out.writeObject("![TestUser]");
                out.flush();
                socket.close();
            } catch (Exception e) {
                // ignore
            }
        }, "MockServer").start();
        
        ClientListener listener = new ClientListener() {
            @Override
            public void onMessageReceived(String message) {}
            
            @Override
            public void onUserListUpdated(List<String> users) {
                userList.set(users);
                connected.countDown();
            }
            
            @Override
            public void onConnectionError(String message) {}
        };
        
        ChatClient client = new ChatClient(listener);
        client.connect("localhost", port, "TestUser");
        
        assertTrue(connected.await(5, TimeUnit.SECONDS), "Should receive user list");
        assertNotNull(userList.get());
        assertTrue(userList.get().contains("TestUser"));
        
        client.disconnect();
    }

    @Test
    @Timeout(10)
    void testConnectionError() throws Exception {
        CountDownLatch errorLatch = new CountDownLatch(1);
        AtomicReference<String> errorMessage = new AtomicReference<>();
        
        ClientListener listener = new ClientListener() {
            @Override
            public void onMessageReceived(String message) {}
            
            @Override
            public void onUserListUpdated(List<String> users) {}
            
            @Override
            public void onConnectionError(String message) {
                errorMessage.set(message);
                errorLatch.countDown();
            }
        };
        
        ChatClient client = new ChatClient(listener);
        client.connect("localhost", 1, "TestUser"); // Port 1 - will fail
        
        assertTrue(errorLatch.await(5, TimeUnit.SECONDS), "Should receive connection error");
        assertNotNull(errorMessage.get());
        assertTrue(errorMessage.get().contains("Could not connect"));
    }
}
