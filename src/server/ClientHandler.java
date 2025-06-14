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
        String[] parts = message.split(":", 4); // Ensure we can handle email with colons

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
        try (Connection conn = dbConnector.getConnection()) {
            // Get user IDs
            int userId = getUserId(conn, username);
            int contactId = getUserId(conn, contactUsername);
            
            if (userId == -1) {
                System.out.println("Cannot add contact: User ID not found for " + username);
                return false;
            }
            
            if (contactId == -1) {
                System.out.println("Cannot add contact: Contact ID not found for " + contactUsername);
                return false;
            }
            
            // Insert contact relationship
            String query = "INSERT OR IGNORE INTO contacts (user_id, contact_id) VALUES (?, ?)";
            PreparedStatement stmt = conn.prepareStatement(query);
            stmt.setInt(1, userId);
            stmt.setInt(2, contactId);
            
            int affected = stmt.executeUpdate();
            System.out.println("Added contact: " + (affected > 0 ? "success" : "already exists"));
            return true; // Return true even if already exists to avoid confusion
        } catch (SQLException e) {
            System.out.println("Error adding contact: " + e.getMessage());
            e.printStackTrace();
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
            
            if (userId == -1) {
                System.out.println("Cannot fetch contacts: User ID not found");
                sendMessage("CONTACT_LIST:");
                return;
            }
            
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
            System.out.println("Sending contact list with " + contacts.size() + " contacts");
            sendMessage(contactList);
        } catch (SQLException e) {
            System.out.println("Error fetching contacts: " + e.getMessage());
            e.printStackTrace();
            sendMessage("CONTACT_LIST:");
        }
    }

    public void sendMessage(String message) {
        System.out.println("Sending to client: " + message); // Debug
        out.println(message);
    }
}