package db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class dbInitializer {
    public static void main(String[] args) {
        initDatabase();
        printDatabaseStructure();
        printContactRelationships();
        dumpContacts();
    }
    
    public static void initDatabase() {
        try (Connection conn = dbConnector.getConnection();
             Statement stmt = conn.createStatement()) {
            
            System.out.println("Initializing database...");
            
            // First create users table
            String createUserTable = 
                "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT NOT NULL UNIQUE," +
                "password TEXT NOT NULL," +
                "email TEXT NOT NULL," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ");";
            
            stmt.execute(createUserTable);
            System.out.println("Users table initialized successfully!");
            
            // Then create contacts table
            String createContactsTable = 
                "CREATE TABLE IF NOT EXISTS contacts (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "user_id INTEGER NOT NULL," +
                "contact_id INTEGER NOT NULL," +
                "FOREIGN KEY (user_id) REFERENCES users(id)," +
                "FOREIGN KEY (contact_id) REFERENCES users(id)," +
                "UNIQUE(user_id, contact_id)" +
                ");";
            
            stmt.execute(createContactsTable);
            System.out.println("Contacts table initialized successfully!");
            
            System.out.println("Database initialization completed successfully");
            
        } catch (SQLException e) {
            System.out.println("Database initialization error: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void printDatabaseStructure() {
        try (Connection conn = dbConnector.getConnection()) {
            DatabaseMetaData meta = conn.getMetaData();
            
            // Print tables
            ResultSet tables = meta.getTables(null, null, "%", new String[] {"TABLE"});
            System.out.println("==== TABLES ====");
            while (tables.next()) {
                String tableName = tables.getString("TABLE_NAME");
                System.out.println(tableName);
                
                // Print columns for this table
                ResultSet columns = meta.getColumns(null, null, tableName, "%");
                while (columns.next()) {
                    System.out.println("  " + columns.getString("COLUMN_NAME") + 
                                     " (" + columns.getString("TYPE_NAME") + ")");
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
    
    public static void printContactRelationships() {
        try (Connection conn = dbConnector.getConnection();
             Statement stmt = conn.createStatement()) {
            
            ResultSet rs = stmt.executeQuery(
                "SELECT u1.username as user, u2.username as contact " +
                "FROM contacts c " +
                "JOIN users u1 ON c.user_id = u1.id " +
                "JOIN users u2 ON c.contact_id = u2.id"
            );
            
            System.out.println("Contact relationships in database:");
            boolean hasContacts = false;
            
            while (rs.next()) {
                hasContacts = true;
                System.out.println("  " + rs.getString("user") + " -> " + rs.getString("contact"));
            }
            
            if (!hasContacts) {
                System.out.println("  No contact relationships found in database.");
            }
        } catch (SQLException e) {
            System.out.println("Error printing contacts: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    public static void clearDatabase() {
        try (Connection conn = dbConnector.getConnection();
             Statement stmt = conn.createStatement()) {
            
            System.out.println("Clearing database...");
            
            // Delete all data
            stmt.executeUpdate("DELETE FROM contacts");
            stmt.executeUpdate("DELETE FROM users");
            
            // Reset auto-increment
            stmt.executeUpdate("DELETE FROM sqlite_sequence WHERE name='users' OR name='contacts'");
            
            System.out.println("Database cleared successfully!");
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public static void dumpContacts() {
        try (Connection conn = dbConnector.getConnection();
            Statement stmt = conn.createStatement()) {
            
            ResultSet rs = stmt.executeQuery(
                "SELECT u1.username as user, u2.username as contact " +
                "FROM contacts c " +
                "JOIN users u1 ON c.user_id = u1.id " +
                "JOIN users u2 ON c.contact_id = u2.id " +
                "ORDER BY u1.username, u2.username"
            );
            
            System.out.println("======== CONTACTS DATABASE DUMP ========");
            boolean found = false;
            
            while (rs.next()) {
                found = true;
                String user = rs.getString("user");
                String contact = rs.getString("contact");
                System.out.println(user + " → " + contact);
            }
            
            if (!found) {
                System.out.println("No contacts found in database");
            }
            System.out.println("=======================================");
            
        } catch (SQLException e) {
            System.out.println("Error dumping contacts: " + e.getMessage());
            e.printStackTrace();
        }
    }

        public static void examineDatabase() {
        try (Connection conn = dbConnector.getConnection();
            Statement stmt = conn.createStatement()) {
            
            // Print all users
            System.out.println("\n==== USERS TABLE ====");
            ResultSet users = stmt.executeQuery("SELECT id, username, email FROM users");
            while (users.next()) {
                System.out.println("ID: " + users.getInt("id") + ", Username: " + 
                    users.getString("username") + ", Email: " + users.getString("email"));
            }
            
            // Print all contacts
            System.out.println("\n==== RAW CONTACTS TABLE ====");
            ResultSet contacts = stmt.executeQuery("SELECT * FROM contacts");
            while (contacts.next()) {
                System.out.println("ID: " + contacts.getInt("id") + 
                                ", User ID: " + contacts.getInt("user_id") + 
                                ", Contact ID: " + contacts.getInt("contact_id"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    public static void testContactLoad(String username) {
    try (Connection conn = dbConnector.getConnection()) {
        // Get user ID
        String userIdQuery = "SELECT id FROM users WHERE username = ?";
        PreparedStatement userStmt = conn.prepareStatement(userIdQuery);
        userStmt.setString(1, username);
        ResultSet userRs = userStmt.executeQuery();
        
        if (!userRs.next()) {
            System.out.println("TEST: User not found: " + username);
            return;
        }
        
        int userId = userRs.getInt("id");
        System.out.println("TEST: Found ID for " + username + ": " + userId);
        
        // Get contacts
        String query = 
            "SELECT u.username FROM users u " +
            "JOIN contacts c ON u.id = c.contact_id " +
            "WHERE c.user_id = ?";
        
        PreparedStatement stmt = conn.prepareStatement(query);
        stmt.setInt(1, userId);
        
        ResultSet rs = stmt.executeQuery();
        System.out.println("TEST: Contacts for " + username + ":");
        
        boolean hasContacts = false;
        while (rs.next()) {
            hasContacts = true;
            String contactName = rs.getString("username");
            System.out.println("  - " + contactName);
        }
        
        if (!hasContacts) {
            System.out.println("  No contacts found");
        }
    } catch (SQLException e) {
        System.out.println("TEST: Error loading contacts: " + e.getMessage());
        e.printStackTrace();
    }
}
}