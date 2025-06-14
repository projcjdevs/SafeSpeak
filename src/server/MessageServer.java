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
        System.out.println("User registered: " + username);
        broadcastUserList();
    }
    
    public void removeClient(String username) {
        clients.remove(username);
        System.out.println("User removed: " + username);
        broadcastUserList();
    }
    
    private void broadcastUserList() {
        // Send updated user list to all clients
        String userList = "USERLIST:" + String.join(",", clients.keySet());
        for (ClientHandler handler : clients.values()) {
            handler.sendMessage(userList);
        }
    }
    
    public void createSession(String creator, String recipient) {
        // Create a session
        Session session = new Session();
        String sessionId = session.getSessionId();
        
        session.addParticipant(creator);
        // Don't add recipient yet, wait for acceptance
        session.setInvitePending(recipient);
        
        // Store session
        sessions.put(sessionId, session);
        
        // Notify both users
        ClientHandler creatorHandler = clients.get(creator);
        ClientHandler recipientHandler = clients.get(recipient);
        
        if (creatorHandler != null) {
            creatorHandler.sendMessage("SESSION_CREATED:" + sessionId + ":" + recipient + ":PENDING");
        }
        
        if (recipientHandler != null) {
            recipientHandler.sendMessage("SESSION_INVITATION:" + sessionId + ":" + creator);
        }
        
        // Add system message to session
        SystemMessage message = new SystemMessage(
            "Session created between " + creator + " and " + recipient + " (waiting for acceptance)",
            SystemMessage.SystemMessageType.SESSION_CREATED
        );
        session.addMessage(message);
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
            
            // Add system message
            SystemMessage message = new SystemMessage(
                username + " joined the conversation",
                SystemMessage.SystemMessageType.USER_JOINED
            );
            session.addMessage(message);
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
        }
    }
    
    public static void main(String[] args) {
        int port = 9090;
        MessageServer server = new MessageServer(port);
        server.start();
    }
}