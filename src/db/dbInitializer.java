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
        printTableSchema("users");
        printTableSchema("contacts");
        listUsers();
    }
    
    public static void initDatabase() {
        try (Connection conn = dbConnector.getConnection();
             Statement stmt = conn.createStatement()) {
            
            System.out.println("Initializing database...");
            
            // Users table
            String createUserTable = 
                "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT NOT NULL UNIQUE," +
                "password TEXT NOT NULL," +
                "email TEXT NOT NULL," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ");";
            
            stmt.execute(createUserTable);
            System.out.println("Users table initialized");
            
            // Contacts table
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
            System.out.println("Contacts table initialized");
            
            // Add test user if not exists
            String checkUser = "SELECT COUNT(*) FROM users WHERE username = 'test'";
            ResultSet rs = stmt.executeQuery(checkUser);
            if (rs.next() && rs.getInt(1) == 0) {
                String addUser = "INSERT INTO users (username, password, email) VALUES ('test', 'test', 'test@example.com')";
                stmt.execute(addUser);
                System.out.println("Test user added");
            } else {
                System.out.println("Test user already exists");
            }
            
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
    
    public static void printTableSchema(String tableName) {
        try (Connection conn = dbConnector.getConnection();
             Statement stmt = conn.createStatement()) {
            
            ResultSet rs = stmt.executeQuery("PRAGMA table_info(" + tableName + ")");
            System.out.println("Schema for table " + tableName + ":");
            while (rs.next()) {
                System.out.println("  " + rs.getString("name") + " (" + 
                                 rs.getString("type") + ", " + 
                                 (rs.getInt("notnull") == 1 ? "NOT NULL" : "NULL") + ")");
            }
        } catch (SQLException e) {
            System.out.println("Error getting schema: " + e.getMessage());
        }
    }
    
    public static void listUsers() {
        try (Connection conn = dbConnector.getConnection();
             Statement stmt = conn.createStatement()) {
            
            ResultSet rs = stmt.executeQuery("SELECT id, username, email FROM users");
            System.out.println("Users in database:");
            while (rs.next()) {
                System.out.println("  ID=" + rs.getInt("id") + 
                                 ", Username=" + rs.getString("username") +
                                 ", Email=" + rs.getString("email"));
            }
        } catch (SQLException e) {
            System.out.println("Error listing users: " + e.getMessage());
        }
    }
}