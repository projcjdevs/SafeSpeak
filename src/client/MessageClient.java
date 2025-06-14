package client;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

public class MessageClient {
    private Socket socket;
    private PrintWriter out;
    private BufferedReader in;
    private String username;
    private BlockingQueue<String> messageQueue;
    private boolean connected = false;
    private Thread receiveThread;
    private Consumer<String> messageHandler;

    public MessageClient() {
        messageQueue = new LinkedBlockingQueue<>();
    }

    public boolean connect(String host, int port) {
        try {
            socket = new Socket(host, port);
            out = new PrintWriter(socket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            connected = true;
            
            // Start thread to receive messages
            receiveThread = new Thread(this::receiveMessages);
            receiveThread.setDaemon(true);
            receiveThread.start();
            
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
            // Wait for authentication response with timeout
            String response = messageQueue.poll(10, TimeUnit.SECONDS);
            System.out.println("Auth check, got from queue: " + response);
            
            if (response == null) {
                System.out.println("Authentication timed out");
                return false;
            }
            
            return "AUTH_SUCCESS".equals(response);
        } catch (InterruptedException e) {
            System.out.println("Authentication interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    public boolean register(String username, String password, String email) {
        if (!connected) return false;
        System.out.println("Sending registration: " + username + ", " + password + ", " + email);
        out.println("REGISTER:" + username + ":" + password + ":" + email);
        
        try {
            // Wait for registration response with timeout
            String response = messageQueue.poll(10, TimeUnit.SECONDS);
            System.out.println("Register check, got from queue: " + response);
            
            if (response == null) {
                System.out.println("Registration timed out");
                return false;
            }
            
            return "REGISTER_SUCCESS".equals(response);
        } catch (InterruptedException e) {
            System.out.println("Registration interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
            return false;
        }
    }
    
    public void createSession(String recipient) {
        if (!connected) return;
        System.out.println("Creating session with: " + recipient);
        out.println("CREATE_SESSION:" + recipient);
    }
    
    public void sendMessage(String sessionId, String content) {
        if (!connected) return;
        // Escape colons with backslash to prevent protocol issues
        String escapedContent = content.replace(":", "\\:");
        System.out.println("Sending message to session " + sessionId + ": " + escapedContent);
        out.println("MSG:" + sessionId + ":" + escapedContent);
    }
    
    public void acceptSessionInvitation(String sessionId) {
        if (!connected) return;
        System.out.println("Accepting session invitation: " + sessionId);
        out.println("ACCEPT_INVITATION:" + sessionId);
    }

    public void rejectSessionInvitation(String sessionId) {
        if (!connected) return;
        System.out.println("Rejecting session invitation: " + sessionId);
        out.println("REJECT_INVITATION:" + sessionId);
    }

    public void searchContacts(String emailQuery) {
        if (!connected) return;
        System.out.println("Searching contacts with email: " + emailQuery);
        out.println("SEARCH_CONTACT:" + emailQuery);
    }

    public void addContact(String username) {
        if (!connected) return;
        System.out.println("Adding contact: " + username);
        out.println("ADD_CONTACT:" + username);
    }

    public void requestContactList() {
        if (!connected) return;
        System.out.println("Requesting contact list");
        out.println("GET_CONTACTS");
    }
    
    public void setMessageHandler(Consumer<String> handler) {
        this.messageHandler = handler;
    }
    
    private void receiveMessages() {
        try {
            String message;
            while (connected && (message = in.readLine()) != null) {
                System.out.println("Received from server: " + message);
                
                // Add to queue for auth/register handling
                messageQueue.put(message);
                
                // Also pass to UI handler if set (except auth messages which are handled separately)
                if (messageHandler != null) {
                    if (message.equals("AUTH_SUCCESS") || message.equals("AUTH_FAILED") ||
                        message.equals("REGISTER_SUCCESS") || message.equals("REGISTER_FAILED")) {
                        // These messages are already handled by the queue
                    } else {
                        // UI-relevant messages
                        messageHandler.accept(message);
                    }
                }
            }
        } catch (IOException e) {
            System.out.println("Error receiving messages: " + e.getMessage());
            connected = false;
        } catch (InterruptedException e) {
            System.out.println("Message queue interrupted: " + e.getMessage());
            Thread.currentThread().interrupt();
        } finally {
            closeConnection();
        }
    }
    
    public void closeConnection() {
        try {
            connected = false;
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (IOException e) {
            System.out.println("Error closing connection: " + e.getMessage());
        }
    }
    
    public boolean isConnected() {
        return connected;
    }
    
    public String getUsername() {
        return username;
    }
}