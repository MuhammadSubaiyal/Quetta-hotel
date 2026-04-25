
package db;

import java.io.File;
import java.net.URL;
import java.net.URLClassLoader;
import java.sql.*;

public class DBConnection {
    public static Connection getConnection() throws SQLException {
        // Load the driver explicitly (works for 9.x and newer)
        ensureDriverLoaded();

        // Update JDBC URL with proper timezone
        Connection con = DriverManager.getConnection(
            "jdbc:mysql://127.0.0.1:3306/quetta_hotel?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC",
            "root",
            "35201"    // replace with your MySQL password
        );
        System.out.println("Connected to DB!");
        return con;
    }

    private static void ensureDriverLoaded() throws SQLException {
        try {
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (ClassNotFoundException e) {
            String jarPath = "lib" + File.separator + "mysql-connector-j-9.6.0.jar";
            File jarFile = new File(jarPath);
            if (!jarFile.exists()) {
                throw new SQLException("MySQL JDBC driver not found. Add " + jarPath + " to the runtime classpath.", e);
            }

            try {
                URL jarUrl = jarFile.toURI().toURL();
                URLClassLoader loader = new URLClassLoader(new URL[]{jarUrl}, Thread.currentThread().getContextClassLoader());
                Thread.currentThread().setContextClassLoader(loader);
                Class.forName("com.mysql.cj.jdbc.Driver", true, loader);
            } catch (Exception ex) {
                throw new SQLException("Failed to load MySQL JDBC driver from " + jarPath, ex);
            }
        }
    }
}