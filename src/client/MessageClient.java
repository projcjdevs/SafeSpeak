package client;

import java.io.*;
import java.net.*;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.function.Consumer;

public class MessageClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private boolean connected = false;
    private String username;

    private Consumer<String> messageHandler;

    // Add a queue for server messages
    private final BlockingQueue<String> messageQueue = new LinkedBlockingQueue<>();

    public void setMessageHandler(Consumer<String> handler) {
        this.messageHandler = handler;
    }

    public boolean connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            connected = true;
            new Thread(this::receiveMessages).start();
            System.out.println("Connected to server at " + host + ":" + port);
            return true;
        } catch (IOException e) {
            System.out.println("Connection error: " + e.getMessage());
            return false;
        }
    }

    public boolean authenticate(String username, String password) {
        if (!connected) return false;

        this.username = username;
        out.println("AUTH:" + username + ":" + password);

        try {
            while (true) {
                String response = messageQueue.take(); // Wait for a message
                System.out.println("Auth check, got from queue: " + response);
                if ("AUTH_SUCCESS".equals(response)) {
                    return true;
                } else if ("AUTH_FAILED".equals(response)) {
                    return false;
                }
                // Ignore other messages (like USERLIST) during authentication
            }
        } catch (InterruptedException e) {
            System.out.println("Authentication interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        }
    }

    public boolean register(String username, String password, String email) {
    if (!connected) return false;
    out.println("REGISTER:" + username + ":" + password + ":" + email);

    try {
        while (true) {
            String response = messageQueue.take();
            System.out.println("Register check, got from queue: " + response);
            if ("REGISTER_SUCCESS".equals(response)) {
                return true;
            } else if ("REGISTER_FAILED".equals(response)) {
                return false;
            }
        }
    } catch (InterruptedException e) {
        System.out.println("Registration interrupted: " + e.getMessage());
        Thread.currentThread().interrupt();
        return false;
    }
}

    public void sendMessage(String sessionId, String content) {
        if (!connected) return;
        out.println("MSG:" + sessionId + ":" + content);
    }

    public void createSession(String recipient) {
        if (!connected) return;
        out.println("CREATE_SESSION:" + recipient);
    }

    private void receiveMessages() {
        try {
            String message;
            while (connected && (message = in.readLine()) != null) {
                System.out.println("Received from server: " + message); // Debug

                // Always put message in the queue
                messageQueue.put(message);

                // Only pass non-auth messages to the handler
                if (messageHandler != null &&
                    !message.equals("AUTH_SUCCESS") &&
                    !message.equals("AUTH_FAILED")) {
                    messageHandler.accept(message);
                }
            }
        } catch (IOException | InterruptedException e) {
            System.out.println("Error receiving message: " + e.getMessage());
            connected = false;
            Thread.currentThread().interrupt();
        }
    }

    public void disconnect() {
        connected = false;
        try {
            if (socket != null) socket.close();
        } catch (IOException e) {
            System.out.println("Error closing connection: " + e.getMessage());
        }
    }
}