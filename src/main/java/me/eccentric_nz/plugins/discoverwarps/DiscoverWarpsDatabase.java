package me.eccentric_nz.plugins.discoverwarps;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class DiscoverWarpsDatabase {

    private static DiscoverWarpsDatabase instance = new DiscoverWarpsDatabase();
    public Connection connection = null;
    public Statement statement = null;
    private DiscoverWarps plugin;

    public static synchronized DiscoverWarpsDatabase getInstance() {
        return instance;
    }

    public void setConnection(String path) throws Exception {
        Class.forName("org.sqlite.JDBC");
        connection = DriverManager.getConnection("jdbc:sqlite:" + path);
    }

    public Connection getConnection() {
        return connection;
    }

    public void createTables() {
        try {
            statement = connection.createStatement();
            final String queryWarps = "CREATE TABLE IF NOT EXISTS discoverwarps (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT COLLATE NOCASE, world TEXT, x INTEGER, y INTEGER, z INTEGER, enabled INTEGER)";
            statement.executeUpdate(queryWarps);
            final String queryVisited = "CREATE TABLE IF NOT EXISTS players (pid INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, player TEXT COLLATE NOCASE, visited TEXT)";
            statement.executeUpdate(queryVisited);
        } catch (SQLException e) {
            plugin.debug("Create table error: " + e);
        } finally {
            if (statement != null) {
                try {
                    statement.close();
                } catch (Exception e) {
                }
            }
        }
    }

    @Override
    protected Object clone() throws CloneNotSupportedException {
        throw new CloneNotSupportedException("Clone is not allowed.");
    }
}