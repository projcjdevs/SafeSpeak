package server;

import java.io.IOException; 
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class MessageServer {
    private static final int PORT = 9090;
    private ServerSocket serverSocket;
    private boolean running = false;

    private Map<String, ClientHandler> clients = new ConcurrentHashMap<>();

    // Changed from Set<String> to Session
    private Map<String, Session> sessions = new ConcurrentHashMap<>();

    public void start() {
        try {
            serverSocket = new ServerSocket(PORT);
            running = true;
            System.out.println("Server started on port " + PORT);

            // Main Server Loop
            while(running) {
                System.out.println("waiting for clients...");
                Socket clientSocket = serverSocket.accept();
                System.out.println("Client connected: " + clientSocket.getInetAddress().getHostAddress());

                // New Thread Connection for each client
                ClientHandler handler = new ClientHandler(clientSocket, this);
                new Thread(handler).start();
            }
        
        } catch (IOException e) {
            System.out.println("Server exception: " + e.getMessage());
            e.printStackTrace();
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
        // Fixed typo: clientS → clients
        for (ClientHandler handler : clients.values()) {
            handler.sendMessage(userList);
        }
    }

    public void createSession(String creator, String recipient) {
        Session session = new Session();
        String sessionId = session.getSessionId();
        
        session.addParticipant(creator);
        session.addParticipant(recipient);
        
        // Store session
        sessions.put(sessionId, session);
        
        // Notify participants
        ClientHandler creatorHandler = clients.get(creator);
        ClientHandler recipientHandler = clients.get(recipient);
        
        if (creatorHandler != null) {
            creatorHandler.sendMessage("SESSION_CREATED:" + sessionId + ":" + recipient);
        }
        
        if (recipientHandler != null) {
            recipientHandler.sendMessage("SESSION_INVITED:" + sessionId + ":" + creator);
        }
        
        // Add system message to session
        SystemMessage message = new SystemMessage(
            "Session created between " + creator + " and " + recipient,
            SystemMessage.SystemMessageType.SESSION_CREATED
        );
        session.addMessage(message);
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
        MessageServer server = new MessageServer();
        server.start();
    }
}