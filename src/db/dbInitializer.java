package db;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class dbInitializer {
    public static void main(String[] args) {
        initDatabase();
        printDatabaseStructure();
        printContactRelationships();
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
}