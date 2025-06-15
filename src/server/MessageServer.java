package server;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class MessageServer {
    private int port;
    private Map<String, ClientHandler> clients = new ConcurrentHashMap<>();
    private Map<String, Session> sessions = new ConcurrentHashMap<>();
    
    public MessageServer(int port) {
        this.port = port;
    }
    
    public void start() {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started on port " + port);
            
            while (true) {
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress());
                
                // Create a new thread to handle this client
                ClientHandler handler = new ClientHandler(clientSocket, this);
                new Thread(handler).start();
            }
        } catch (IOException e) {
            System.out.println("Server error: " + e.getMessage());
        }
    }
    
    public void registerClient(String username, ClientHandler handler) {
        clients.put(username, handler);
        System.out.println("User registered: " + username + ", total users: " + clients.size());
        
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        
        // Send the complete user list to ALL clients, including the newly connected one
        broadcastUserList();
        
        String userList = "USERLIST:" + String.join(",", clients.keySet());
        handler.sendMessage(userList);
    }
    
    public void removeClient(String username) {
        clients.remove(username);
        System.out.println("User removed: " + username);
        broadcastUserList();
    }
    
    private void broadcastUserList() {
        // Send updated user list to all clients
        String userList = "USERLIST:" + String.join(",", clients.keySet());
        System.out.println("Broadcasting user list: " + userList + " to " + clients.size() + " clients");
        
        // Debug: print all connected clients
        System.out.println("Connected clients:");
        for (String user : clients.keySet()) {
            System.out.println("  - " + user);
        }
        
        // Send to each client individually and verify
        for (Map.Entry<String, ClientHandler> entry : clients.entrySet()) {
            String clientName = entry.getKey();
            ClientHandler handler = entry.getValue();
            System.out.println("Sending user list to client: " + clientName);
            handler.sendMessage(userList);
        }
    }
    
    // Method that allow clients to request the user list
    public void sendUserListToClient(String username) {
        ClientHandler handler = clients.get(username);
        if (handler != null) {
            String userList = "USERLIST:" + String.join(",", clients.keySet());
            System.out.println("EXPLICIT REQUEST: Sending user list to " + username + ": " + userList);
            handler.sendMessage(userList);
        } else {
            System.out.println("ERROR: Cannot send user list to " + username + " - handler not found");
        }
    }
        
    public void createSession(String creator, String recipient) {
        // Check if both users exist
        ClientHandler creatorHandler = clients.get(creator);
        ClientHandler recipientHandler = clients.get(recipient);
        
        if (creatorHandler == null || recipientHandler == null) {
            System.out.println("Cannot create session: one or both users not found");
            return;
        }
        
        // Create a session
        Session session = new Session();
        String sessionId = session.getSessionId();
        
        session.addParticipant(creator);
        // Don't add recipient yet, wait for acceptance
        session.setInvitePending(recipient);
        
        // Store session
        sessions.put(sessionId, session);
        
        // Notify both users
        creatorHandler.sendMessage("SESSION_CREATED:" + sessionId + ":" + recipient + ":PENDING");
        recipientHandler.sendMessage("SESSION_INVITATION:" + sessionId + ":" + creator);
        
        System.out.println("Created session " + sessionId + " between " + creator + " and " + recipient + " (pending)");
    }
    
    public void acceptSessionInvitation(String sessionId, String username) {
        Session session = sessions.get(sessionId);
        if (session != null && session.isInvitePending(username)) {
            session.addParticipant(username);
            session.clearPendingInvite();
            
            // Notify all participants
            for (String participant : session.getParticipants()) {
                ClientHandler handler = clients.get(participant);
                if (handler != null) {
                    handler.sendMessage("SESSION_ACCEPTED:" + sessionId + ":" + username);
                }
            }
            
            System.out.println(username + " accepted invitation to session " + sessionId);
            
            // Add system message
            SystemMessage message = new SystemMessage(
                username + " joined the conversation",
                SystemMessage.SystemMessageType.USER_JOINED
            );
            session.addMessage(message);
        } else {
            System.out.println("Invalid session acceptance: " + sessionId + " by " + username);
        }
    }
    
    public void rejectSessionInvitation(String sessionId, String username) {
        Session session = sessions.get(sessionId);
        if (session != null && session.isInvitePending(username)) {
            session.clearPendingInvite();
            
            // Notify the creator
            for (String participant : session.getParticipants()) {
                ClientHandler handler = clients.get(participant);
                if (handler != null) {
                    handler.sendMessage("SESSION_REJECTED:" + sessionId + ":" + username);
                }
            }
            
            System.out.println(username + " rejected invitation to session " + sessionId);
        } else {
            System.out.println("Invalid session rejection: " + sessionId + " by " + username);
        }
    }
    
    public void sendDirectMessage(String sender, String sessionId, String content) {
        Session session = sessions.get(sessionId);
        
        if (session != null && session.hasParticipant(sender)) {
            // Create a message object (OOP!)
            TextMessage message = new TextMessage(sender, content);
            session.addMessage(message);
            
            // Send to all participants except sender
            for (String participant : session.getParticipants()) {
                if (!participant.equals(sender)) {
                    ClientHandler handler = clients.get(participant);
                    if (handler != null) {
                        handler.sendMessage("MSG:" + sessionId + ":" + sender + ":" + content);
                    }
                }
            }
            
            System.out.println("Message sent in session " + sessionId + " from " + sender);
        } else {
            System.out.println("Invalid message send attempt to session " + sessionId + " from " + sender);
        }
    }
    
    public static void main(String[] args) {
        int port = 9090;
        MessageServer server = new MessageServer(port);
        server.start();
    }
}