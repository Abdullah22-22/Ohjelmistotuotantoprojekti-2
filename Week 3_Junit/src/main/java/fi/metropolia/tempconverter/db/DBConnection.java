package fi.metropolia.tempconverter.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class DBConnection {

    private static String url  = System.getenv().getOrDefault("DB_URL",
            "jdbc:mariadb://localhost:3307/tempdb?useUnicode=true&characterEncoding=utf8");
    private static String user = System.getenv().getOrDefault("DB_USER", "root");
    private static String pass = System.getenv().getOrDefault("DB_PASS", "root");

    private static Connection connection;

    private DBConnection() {
    }

    public static Connection getConnection() throws SQLException {
        if (connection == null || connection.isClosed()) {
            connection = DriverManager.getConnection(url, user, pass);
        }
        return connection;
    }

    public static void setCredentials(String newUrl, String newUser, String newPass) {
        url = newUrl;
        user = newUser;
        pass = newPass;
        connection = null;
    }

    public static boolean testConnection() {
        try {
            return getConnection() != null && !getConnection().isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public static void closeConnection() throws SQLException {
        if (connection != null && !connection.isClosed()) {
            connection.close();
            connection = null;
        }
    }
}