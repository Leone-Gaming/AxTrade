package com.artillexstudios.axtrade.trade.logging.database;

import com.artillexstudios.axtrade.AxTrade;
import com.artillexstudios.axtrade.trade.logging.TradeItem;
import com.artillexstudios.axtrade.trade.logging.TradeLog;
import com.artillexstudios.axtrade.trade.logging.TradeResult;
import com.artillexstudios.axtrade.trade.logging.database.impl.H2DatabaseImpl;
import org.bukkit.Bukkit;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static java.util.logging.Level.SEVERE;

public class Database {

    private Database() {
    }

    private static DatabaseImpl db;

    public static void setup() {
        db = new H2DatabaseImpl();
        db.init();
    }

    public static void close() {
        db.close();
    }

    public static void logTrade(TradeLog trade) {
        try (Connection conn = db.connection()) {
            conn.setAutoCommit(false);

            try (PreparedStatement ps = conn.prepareStatement("INSERT INTO trade_logs VALUES (?, ?, ?, ?)")) {
                ps.setString(1, trade.tradeId().toString());
                ps.setString(2, trade.playerOne().toString());
                ps.setString(3, trade.playerTwo().toString());
                ps.setTimestamp(4, new Timestamp(trade.time()));
                ps.executeUpdate();
            }

            trade.results().forEach(result -> result.insert(trade, conn));

            conn.commit();
        } catch (SQLException e) {
            AxTrade.getInstance().getLogger().log(SEVERE, "Failed to log trade.", e);
        }
    }

    public static CompletableFuture<List<TradeLog>> getTradeLogs(UUID target) {
        return CompletableFuture.supplyAsync(() -> {
            final String tradeLogsQuery = """
                        SELECT * FROM trade_logs 
                        WHERE player_one = ? OR player_two = ? 
                        ORDER BY trade_time DESC;
                    """;

            List<TradeLog> tradeLogs = new ArrayList<>();

            try (Connection conn = db.connection();
                 PreparedStatement logsStmt = conn.prepareStatement(tradeLogsQuery)) {

                logsStmt.setString(1, target.toString());
                logsStmt.setString(2, target.toString());

                try (ResultSet logsResult = logsStmt.executeQuery()) {
                    while (logsResult.next()) {
                        UUID tradeId = UUID.fromString(logsResult.getString("id"));
                        UUID playerOneId = UUID.fromString(logsResult.getString("player_one"));
                        UUID playerTwoId = UUID.fromString(logsResult.getString("player_two"));
                        long tradeTime = logsResult.getTimestamp("trade_time").getTime();

                        List<TradeItem> given = new ArrayList<>();
                        List<TradeItem> received = new ArrayList<>();

                        try (PreparedStatement itemsStmt = conn.prepareStatement(
                                "SELECT * FROM trade_items WHERE trade_log_id = ?")) {

                            itemsStmt.setString(1, tradeId.toString());
                            try (ResultSet itemsResult = itemsStmt.executeQuery()) {
                                while (itemsResult.next()) {
                                    TradeItem item = TradeItem.deserialize(itemsResult);
                                    String directionStr = itemsResult.getString("direction");

                                    try {
                                        TradeResult.TradeDirection direction =
                                                TradeResult.TradeDirection.valueOf(directionStr);
                                        if (direction == TradeResult.TradeDirection.GIVEN) {
                                            given.add(item);
                                        } else {
                                            received.add(item);
                                        }
                                    } catch (IllegalArgumentException ex) {
                                        AxTrade.getInstance().getLogger().warning(
                                                "Invalid direction: " + directionStr);
                                    }
                                }
                            }
                        }

                        TradeResult playerOneResult = new TradeResult(given, received);
                        TradeResult playerTwoResult = new TradeResult(received, given);
                        tradeLogs.add(new TradeLog(tradeId, playerOneId, playerTwoId,
                                tradeTime, playerOneResult, playerTwoResult));
                    }
                }
            } catch (SQLException e) {
                throw new RuntimeException("Error retrieving trade logs", e);
            }
            return tradeLogs;
        });
    }

    public static TradeLog getTradeLog(String tradeId) {
        final String tradeLogQuery = "SELECT * FROM trade_logs WHERE id = ?";
        final String tradeItemsQuery = "SELECT * FROM trade_items WHERE trade_log_id = ?";

        try (Connection conn = db.connection();
             PreparedStatement logStmt = conn.prepareStatement(tradeLogQuery);
             PreparedStatement itemsStmt = conn.prepareStatement(tradeItemsQuery)) {

            logStmt.setString(1, tradeId);
            ResultSet logResult = logStmt.executeQuery();

            if (!logResult.next()) {
                return null; // No trade log found for the specified item
            }

            UUID id = UUID.fromString(logResult.getString("id"));
            UUID playerOneId = UUID.fromString(logResult.getString("player_one"));
            UUID playerTwoId = UUID.fromString(logResult.getString("player_two"));
            long tradeTime = logResult.getTimestamp("trade_time").getTime();

            // Fetch trade items
            itemsStmt.setString(1, tradeId);
            ResultSet itemsResult = itemsStmt.executeQuery();

            List<TradeItem> given = new ArrayList<>();
            List<TradeItem> received = new ArrayList<>();

            while (itemsResult.next()) {
                TradeItem item = TradeItem.deserialize(itemsResult);
                TradeResult.TradeDirection direction = TradeResult.TradeDirection.valueOf(itemsResult.getString("direction"));

                if (direction == TradeResult.TradeDirection.GIVEN) {
                    given.add(item);
                } else {
                    received.add(item);
                }
            }

            TradeResult playerOneResult = new TradeResult(given, received);
            TradeResult playerTwoResult = new TradeResult(received, given);
            return new TradeLog(id, playerOneId, playerTwoId, tradeTime, playerOneResult, playerTwoResult);

        } catch (SQLException e) {
            throw new RuntimeException("Error retrieving trade log", e);
        }
    }

}
