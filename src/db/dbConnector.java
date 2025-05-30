package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class dbConnector {
    private static final String url = "jdbc:sqlite:safespeak.db";

    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(url);
    }

}


