package com.artillexstudios.axtrade.trade.logging.database.impl;

import com.artillexstudios.axtrade.AxTrade;
import com.artillexstudios.axtrade.trade.logging.database.DatabaseImpl;
import org.h2.jdbc.JdbcConnection;

import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class H2DatabaseImpl implements DatabaseImpl {

    private H2Connection connection;

    @Override
    public void init() {
        try {
            Class.forName("org.h2.Driver");
        } catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }

        try {
            String url = "jdbc:h2:./" + AxTrade.getInstance().getDataFolder() + "/data;mode=MySQL";
            this.connection = new H2Connection(url, new Properties());
            this.connection.setAutoCommit(true);
        } catch (SQLException ex) {
            throw new RuntimeException("Failed to initialize H2 database connection", ex);
        }

        this.createTables();
    }

    @Override
    public void close() {
        if (this.connection != null) {
            try {
                this.connection.closeConnection();
            } catch (SQLException ex) {
                ex.printStackTrace();
            }
        }
    }

    @Override
    public Connection connection() {
        return this.connection;
    }

    private void createTables() {
        final String tradeLogsTable = """
                CREATE TABLE IF NOT EXISTS trade_logs (
                    id CHAR(36) PRIMARY KEY,
                    player_one CHAR(36) NOT NULL,
                    player_two CHAR(36) NOT NULL,
                    trade_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
                );
                """;

        final String tradeItemsTable = """
                CREATE TABLE IF NOT EXISTS trade_items (
                    id INT PRIMARY KEY AUTO_INCREMENT,
                    trade_log_id CHAR(36) NOT NULL,
                    type VARCHAR(255) NOT NULL,
                    direction VARCHAR(10) NOT NULL CHECK (direction IN ('GIVEN', 'RECEIVED')),
                    item TEXT,
                    quantity DOUBLE NOT NULL,
                    FOREIGN KEY (trade_log_id) REFERENCES trade_logs(id)
                );
                """;


        try (Connection c = connection(); Statement s = c.createStatement()) {
            c.setAutoCommit(false);

            s.executeUpdate(tradeLogsTable);
            s.executeUpdate(tradeItemsTable);

            c.commit();
        } catch (SQLException e) {
            throw new RuntimeException("Error creating database tables", e);
        }
    }

    private static class H2Connection extends JdbcConnection {
        public H2Connection(String url, Properties properties) throws SQLException {
            super(url, properties, null, null, false);
        }

        @Override
        public synchronized void close() {
            // Prevent accidental closing of the connection
        }

        public synchronized void closeConnection() throws SQLException {
            super.close();
        }
    }
}