package server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

import db.dbConnector;

public class ClientHandler implements Runnable {
    private Socket clientSocket;
    private MessageServer server;
    private PrintWriter out;
    private BufferedReader in;
    private String username = null;
    private boolean authenticated = false;

    public ClientHandler(Socket socket, MessageServer server) {
        this.clientSocket = socket;
        this.server = server;
    }

    @Override
    public void run() {
        try {
            out = new PrintWriter(clientSocket.getOutputStream(), true);
            in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                System.out.println("Received from client: " + inputLine);
                processMessage(inputLine);
            }
        } catch (IOException e) {
            System.out.println("Error handling client: " + e.getMessage());
        } finally {
            if (username != null) {
                server.removeClient(username);
            }
            try {
                clientSocket.close();
            } catch (IOException e) {
                System.out.println("Error closing socket: " + e.getMessage());
            }
        }
    }

    private void processMessage(String message) {
        System.out.println("Processing message: " + message); // Debug
        String[] parts = message.split(":", 4);

        if (parts.length < 2) {
            System.out.println("Invalid message format: " + message);
            return;
        }

        String command = parts[0];

        // Registration
        if (command.equals("REGISTER") && parts.length >= 4) {
            String username = parts[1];
            String password = parts[2];
            String email = parts[3];
            System.out.println("Registering: " + username + " / " + email);

            if (registerUser(username, password, email)) {
                sendMessage("REGISTER_SUCCESS");
            } else {
                sendMessage("REGISTER_FAILED");
            }
            return;
        }

        // Handle authentication
        if (command.equals("AUTH") && parts.length >= 3) {
            String username = parts[1];
            String password = parts[2];
            System.out.println("Authenticating: " + username + " / " + password);

            if (authenticate(username, password)) {
                this.username = username;
                this.authenticated = true;
                sendMessage("AUTH_SUCCESS");
                System.out.println("Sent AUTH_SUCCESS to client");
                server.registerClient(username, this);
            } else {
                sendMessage("AUTH_FAILED");
                System.out.println("Sent AUTH_FAILED to client");
            }
            return;
        }

        // Only process other commands if authenticated
        if (!authenticated) {
            sendMessage("NOT_AUTHENTICATED");
            System.out.println("Client not authenticated, sent NOT_AUTHENTICATED");
            return;
        }

        // Handle direct message
        if (command.equals("MSG") && parts.length >= 3) {
            String sessionId = parts[1];
            String content = parts[2];
            System.out.println("Direct message from " + username + " to session " + sessionId + ": " + content);
            server.sendDirectMessage(username, sessionId, content);
        }
        // Handle create session command
        else if (command.equals("CREATE_SESSION") && parts.length >= 2) {
            String recipient = parts[1];
            System.out.println("Creating session between " + username + " and " + recipient);
            server.createSession(username, recipient);
        }
        // Handle session invitation response
        else if (command.equals("ACCEPT_INVITATION") && parts.length >= 2) {
            String sessionId = parts[1];
            server.acceptSessionInvitation(sessionId, username);
        }
        else if (command.equals("REJECT_INVITATION") && parts.length >= 2) {
            String sessionId = parts[1];
            server.rejectSessionInvitation(sessionId, username);
        }
        // Handle contact search
        else if (command.equals("SEARCH_CONTACT") && parts.length >= 2) {
            String emailQuery = parts[1];
            List<String> matches = searchContacts(emailQuery);
            String result = "SEARCH_RESULTS:" + String.join(",", matches);
            sendMessage(result);
        }
        // Handle contact add
        else if (command.equals("ADD_CONTACT") && parts.length >= 2) {
            String contactUsername = parts[1];
            if (addContact(username, contactUsername)) {
                sendMessage("CONTACT_ADDED:" + contactUsername);
                // Send updated contact list
                sendContactList();
            } else {
                sendMessage("CONTACT_ADD_FAILED:" + contactUsername);
            }
        }
        // Request contact list
        else if (command.equals("GET_CONTACTS")) {
            sendContactList();
        }
    }

    private boolean authenticate(String username, String password) {
        try (Connection conn = dbConnector.getConnection()) {
            String query = "SELECT * FROM users WHERE username = ? AND password = ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            stmt.setString(2, password);

            ResultSet rs = stmt.executeQuery();
            boolean result = rs.next();
            System.out.println("Authentication result for " + username + ": " + result);
            return result;
        } catch (SQLException e) {
            System.out.println("Database error during authentication: " + e.getMessage());
            return false;
        }
    }

    private boolean registerUser(String username, String password, String email) {
        try (Connection conn = dbConnector.getConnection()) {
            String query = "INSERT INTO users (username, password, email) VALUES (?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, email);
            stmt.executeUpdate();
            return true;
        } catch (SQLException e) {
            System.out.println("Registration error: " + e.getMessage());
            return false;
        }
    }
    
    private List<String> searchContacts(String emailQuery) {
        List<String> results = new ArrayList<>();
        try (Connection conn = dbConnector.getConnection()) {
            String query = "SELECT username FROM users WHERE email LIKE ? AND username != ?";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, "%" + emailQuery + "%");
            stmt.setString(2, username);
            
            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                results.add(rs.getString("username"));
            }
        } catch (SQLException e) {
            System.out.println("Error searching contacts: " + e.getMessage());
        }
        return results;
    }

    private boolean addContact(String username, String contactUsername) {
        try (Connection conn = dbConnector.getConnection()) {
            // Get user IDs
            int userId = getUserId(conn, username);
            int contactId = getUserId(conn, contactUsername);
            
            if (userId == -1 || contactId == -1) return false;
            
            // Insert contact relationship
            String query = "INSERT OR IGNORE INTO contacts (user_id, contact_id) VALUES (?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setInt(2, contactId);
            
            int affected = stmt.executeUpdate();
            return affected > 0;
        } catch (SQLException e) {
            System.out.println("Error adding contact: " + e.getMessage());
            return false;
        }
    }

    private int getUserId(Connection conn, String username) throws SQLException {
        String query = "SELECT id FROM users WHERE username = ?";
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setString(1, username);
        
        ResultSet rs = stmt.executeQuery();
        if (rs.next()) {
            return rs.getInt("id");
        }
        return -1;
    }

    private void sendContactList() {
        try (Connection conn = dbConnector.getConnection()) {
            int userId = getUserId(conn, username);
            
            String query = 
                "SELECT u.username FROM users u " +
                "JOIN contacts c ON u.id = c.contact_id " +
                "WHERE c.user_id = ?";
            
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            
            ResultSet rs = stmt.executeQuery();
            List<String> contacts = new ArrayList<>();
            
            while (rs.next()) {
                contacts.add(rs.getString("username"));
            }
            
            String contactList = "CONTACT_LIST:" + String.join(",", contacts);
            sendMessage(contactList);
        } catch (SQLException e) {
            System.out.println("Error fetching contacts: " + e.getMessage());
        }
    }

    public void sendMessage(String message) {
        System.out.println("Sending to client: " + message); // Debug
        out.println(message);
    }
}