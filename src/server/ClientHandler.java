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

    public void sendMessage(String message) {
        System.out.println("Sending to client: " + message); // Debug
        out.println(message);
    }
}