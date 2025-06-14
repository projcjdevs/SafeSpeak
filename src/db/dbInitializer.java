package db;

import java.sql.Connection;
import java.sql.Statement;
import java.sql.SQLException;

public class dbInitializer {
    public static void main(String[] args) {
        try (Connection conn = dbConnector.getConnection();
             Statement stmt = conn.createStatement()) {
            
            // Create users table
            String createUsersTable = 
                "CREATE TABLE IF NOT EXISTS users (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                "username TEXT NOT NULL UNIQUE," +
                "password TEXT NOT NULL," +
                "email TEXT NOT NULL," +
                "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP" +
                ");";
            
            // Execute the SQL statement
            stmt.execute(createUsersTable);
            System.out.println("Database initialized successfully!");

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
            
            // Insert a test user if none exists
            String checkUser = "SELECT COUNT(*) as count FROM users";
            var rs = stmt.executeQuery(checkUser);
            if (rs.next() && rs.getInt("count") == 0) {
                String insertTestUser = 
                    "INSERT INTO users (username, password, email) VALUES " +
                    "('test', 'password', 'test@example.com');";
                stmt.execute(insertTestUser);
                System.out.println("Added test user: username=test, password=password");
            }
            
        } catch (SQLException e) {
            System.out.println("Database initialization failed: " + e.getMessage());
            e.printStackTrace();
        }
    }
}