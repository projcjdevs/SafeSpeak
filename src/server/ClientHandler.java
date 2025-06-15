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
import java.sql.Statement; 

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
        String[] parts = message.split(":", 4); // Split with limit to handle content with colons

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
                System.out.println("Registration successful for: " + username);
                sendMessage("REGISTER_SUCCESS");
            } else {
                System.out.println("Registration failed for: " + username);
                sendMessage("REGISTER_FAILED");
            }
            return;
        }

        // Handle authentication
        if (command.equals("AUTH") && parts.length >= 3) {
            String username = parts[1];
            String password = parts[2];
            System.out.println("Authenticating: " + username);

            if (authenticate(username, password)) {
                this.username = username;
                this.authenticated = true;
                sendMessage("AUTH_SUCCESS");
                System.out.println("Authentication successful for: " + username);
                server.registerClient(username, this);
            } else {
                sendMessage("AUTH_FAILED");
                System.out.println("Authentication failed for: " + username);
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
            System.out.println(username + " accepted invitation to session " + sessionId);
            server.acceptSessionInvitation(sessionId, username);
        }
        else if (command.equals("REJECT_INVITATION") && parts.length >= 2) {
            String sessionId = parts[1];
            System.out.println(username + " rejected invitation to session " + sessionId);
            server.rejectSessionInvitation(sessionId, username);
        }
        // Handle contact search
        else if (command.equals("SEARCH_CONTACT") && parts.length >= 2) {
            String emailQuery = parts[1];
            System.out.println(username + " searching for contacts with email like: " + emailQuery);
            List<String> matches = searchContacts(emailQuery);
            String result = "SEARCH_RESULTS:" + String.join(",", matches);
            sendMessage(result);
        }
        // Handle contact add
        else if (command.equals("ADD_CONTACT") && parts.length >= 2) {
            String contactUsername = parts[1];
            System.out.println(username + " adding contact: " + contactUsername);
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
            System.out.println(username + " requested contact list");
            sendContactList();
        }
        // Request user list
        else if (command.equals("GET_USERS")) {
            System.out.println(username + " requested user list");
            server.sendUserListToClient(username);
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
            e.printStackTrace();
            return false;
        }
    }

    private boolean registerUser(String username, String password, String email) {
        try (Connection conn = dbConnector.getConnection()) {
            // First check if username already exists
            String checkQuery = "SELECT COUNT(*) FROM users WHERE username = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
            checkStmt.setString(1, username);
            ResultSet rs = checkStmt.executeQuery();
            
            if (rs.next() && rs.getInt(1) > 0) {
                System.out.println("Registration failed: Username already exists: " + username);
                return false;
            }
            
            // Check if email already exists
            checkQuery = "SELECT COUNT(*) FROM users WHERE email = ?";
            checkStmt = conn.prepareStatement(checkQuery);
            checkStmt.setString(1, email);
            rs = checkStmt.executeQuery();
            
            if (rs.next() && rs.getInt(1) > 0) {
                System.out.println("Registration failed: Email already exists: " + email);
                return false;
            }
            
            // If not exists, proceed with registration
            String query = "INSERT INTO users (username, password, email) VALUES (?, ?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setString(1, username);
            stmt.setString(2, password);
            stmt.setString(3, email);
            int rows = stmt.executeUpdate();
            System.out.println("Registration rows affected: " + rows);
            return rows > 0;
        } catch (SQLException e) {
            System.out.println("Registration error: " + e.getMessage());
            e.printStackTrace();
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
            System.out.println("Found " + results.size() + " contacts matching email: " + emailQuery);
        } catch (SQLException e) {
            System.out.println("Error searching contacts: " + e.getMessage());
            e.printStackTrace();
        }
        return results;
    }

    private boolean addContact(String username, String contactUsername) {
        Connection conn = null;
        try {
            conn = dbConnector.getConnection();
            conn.setAutoCommit(false); // Start transaction
            
            System.out.println("[CONTACTS] Adding contact: " + username + " -> " + contactUsername);
            
            // Get user IDs
            int userId = getUserId(conn, username);
            int contactId = getUserId(conn, contactUsername);
            
            if (userId == -1) {
                System.out.println("[CONTACTS] Cannot add contact: User ID not found for " + username);
                return false;
            }
            
            if (contactId == -1) {
                System.out.println("[CONTACTS] Cannot add contact: Contact ID not found for " + contactUsername);
                return false;
            }
            
            // Insert contact relationship
            String query = "INSERT OR IGNORE INTO contacts (user_id, contact_id) VALUES (?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setInt(2, contactId);
            
            int affected = stmt.executeUpdate();
            System.out.println("[CONTACTS] Added contact relationship: " + username + " -> " + contactUsername 
                            + " (rows affected: " + affected + ")");
            
            // Verify the contact was added
            String checkQuery = "SELECT COUNT(*) FROM contacts WHERE user_id = ? AND contact_id = ?";
            PreparedStatement checkStmt = conn.prepareStatement(checkQuery);
            checkStmt.setInt(1, userId);
            checkStmt.setInt(2, contactId);
            ResultSet rs = checkStmt.executeQuery();
            
            boolean success = false;
            if (rs.next()) {
                int count = rs.getInt(1);
                System.out.println("[CONTACTS] Contact relationship exists: " + count + " rows found");
                success = count > 0;
            }
            
            if (success) {
                conn.commit(); // Commit transaction
                System.out.println("[CONTACTS] Contact addition committed to database");
            } else {
                conn.rollback(); // Rollback if verification failed
                System.out.println("[CONTACTS] Contact addition failed verification, rolled back");
            }
            
            return success;
        } catch (SQLException e) {
            System.out.println("[CONTACTS] Error adding contact: " + e.getMessage());
            e.printStackTrace();
            if (conn != null) {
                try {
                    conn.rollback();
                    System.out.println("[CONTACTS] Rolled back transaction due to error");
                } catch (SQLException ex) {
                    ex.printStackTrace();
                }
            }
            return false;
        } finally {
            if (conn != null) {
                try {
                    conn.setAutoCommit(true);
                    conn.close();
                } catch (SQLException e) {
                    e.printStackTrace();
                }
            }
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
            System.out.println("[CONTACTS] Fetching contact list for " + username);
            
            // Debug query to check for the existence of the contacts table
            try (Statement checkStmt = conn.createStatement()) {
                ResultSet tableCheck = checkStmt.executeQuery(
                    "SELECT name FROM sqlite_master WHERE type='table' AND name='contacts'");
                if (!tableCheck.next()) {
                    System.out.println("[CONTACTS] WARNING: contacts table doesn't exist!");
                } else {
                    System.out.println("[CONTACTS] contacts table exists.");
                }
            }
            
            // First get the user ID from username
            String userIdQuery = "SELECT id FROM users WHERE username = ?";
            PreparedStatement userStmt = conn.prepareStatement(userIdQuery);
            userStmt.setString(1, username);
            ResultSet userRs = userStmt.executeQuery();
            
            if (!userRs.next()) {
                System.out.println("[CONTACTS] Cannot fetch contacts: User ID not found for " + username);
                sendMessage("CONTACT_LIST:");
                return;
            }
            
            int userId = userRs.getInt("id");
            System.out.println("[CONTACTS] Found user ID for " + username + ": " + userId);
            
            // Check raw contacts for this user
            String rawQuery = "SELECT * FROM contacts WHERE user_id = ?";
            PreparedStatement rawStmt = conn.prepareStatement(rawQuery);
            rawStmt.setInt(1, userId);
            ResultSet rawRs = rawStmt.executeQuery();
            
            System.out.println("[CONTACTS] Raw contacts for user ID " + userId + ":");
            boolean hasAny = false;
            while (rawRs.next()) {
                hasAny = true;
                System.out.println("  - Contact ID: " + rawRs.getInt("contact_id"));
            }
            
            if (!hasAny) {
                System.out.println("[CONTACTS] No raw contact records found.");
            }
            
            // Now get their contacts - use a JOIN to get usernames directly
            String query = 
                "SELECT u.username FROM users u " +
                "JOIN contacts c ON u.id = c.contact_id " +
                "WHERE c.user_id = ?";
            
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            
            ResultSet rs = stmt.executeQuery();
            List<String> contacts = new ArrayList<>();
            
            while (rs.next()) {
                String contactName = rs.getString("username");
                contacts.add(contactName);
                System.out.println("[CONTACTS] Found contact for " + username + ": " + contactName);
            }
            
            String contactList = "CONTACT_LIST:" + String.join(",", contacts);
            System.out.println("[CONTACTS] Sending contact list to " + username + ": " + contactList);
            sendMessage(contactList);
            
            // DEBUG: Verify contacts in database 
            String debugQuery = "SELECT COUNT(*) FROM contacts WHERE user_id = ?";
            PreparedStatement debugStmt = conn.prepareStatement(debugQuery);
            debugStmt.setInt(1, userId);
            ResultSet debugRs = debugStmt.executeQuery();
            if (debugRs.next()) {
                System.out.println("[CONTACTS] Total contacts in database for " + username + ": " + debugRs.getInt(1));
            }
        } catch (SQLException e) {
            System.out.println("[CONTACTS] Error fetching contacts: " + e.getMessage());
            e.printStackTrace();
            sendMessage("CONTACT_LIST:");
        }
    }

    public void sendMessage(String message) {
        System.out.println("Sending to client: " + message); // Debug
        out.println(message);
    }
}