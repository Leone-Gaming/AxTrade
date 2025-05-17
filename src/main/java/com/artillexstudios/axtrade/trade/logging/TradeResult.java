package com.artillexstudios.axtrade.trade.logging;

import com.artillexstudios.axtrade.hooks.currency.ExperienceHook;
import com.artillexstudios.axtrade.hooks.currency.VaultHook;
import com.artillexstudios.axtrade.trade.TradePlayer;
import com.artillexstudios.axtrade.trade.logging.type.CurrencyTradeItem;
import com.artillexstudios.axtrade.trade.logging.type.ItemStackTradeItem;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class TradeResult {

    private final List<TradeItem> given;
    private final List<TradeItem> received;

    public TradeResult(List<TradeItem> given, List<TradeItem> received) {
        this.given = given;
        this.received = received;
    }

    /**
     * Creates a new trade result
     *
     * @param parent the player giving items
     * @param target the player receiving items
     */
    public TradeResult(TradePlayer parent, TradePlayer target) {
        this.given = new ArrayList<>();
        this.received = new ArrayList<>();

        parent.getCurrencies().forEach((k, v) -> {
            final TradeItem item;
            if (k instanceof VaultHook) {
                item = new CurrencyTradeItem(CurrencyTradeItem.CurrencyType.ECONOMY, v);
            } else if (k instanceof ExperienceHook) {
                item = new CurrencyTradeItem(CurrencyTradeItem.CurrencyType.EXPERIENCE, v);
            } else {
                item = null;
            }

            if (item != null) {
                this.given.add(item);
            }
        });

        target.getCurrencies().forEach((k, v) -> {
            final TradeItem item;
            if (k instanceof VaultHook) {
                item = new CurrencyTradeItem(CurrencyTradeItem.CurrencyType.ECONOMY, v);
            } else if (k instanceof ExperienceHook) {
                item = new CurrencyTradeItem(CurrencyTradeItem.CurrencyType.EXPERIENCE, v);
            } else {
                item = null;
            }

            if (item != null) {
                this.received.add(item);
            }
        });

        parent.getTradeGui().getItems(false).forEach(itemStack -> {
            if (itemStack == null) return;

            this.given.add(new ItemStackTradeItem(itemStack.clone()));
        });

        target.getTradeGui().getItems(false).forEach(itemStack -> {
            if (itemStack == null) return;

            this.received.add(new ItemStackTradeItem(itemStack.clone()));
        });
    }

    public List<TradeItem> received() {
        return received;
    }

    public List<TradeItem> given() {
        return given;
    }

    public void insert(TradeLog parent, Connection conn) {
        final String sql = "INSERT INTO trade_items(trade_log_id, type, direction, item, quantity) VALUES(?, ?, ?, ?, ?)";

        try (PreparedStatement statement = conn.prepareStatement(sql)) {
            for (TradeItem item : given) {
                statement.setString(1, parent.tradeId().toString());
                statement.setString(2, item.itemType().name());
                statement.setString(3, TradeDirection.GIVEN.name());
                statement.setString(4, item.serialize());
                statement.setDouble(5, item.amount());

                statement.addBatch(); // Add to batch for performance
            }

            for (TradeItem item : received) {
                statement.setString(1, parent.tradeId().toString());
                statement.setString(2, item.itemType().name());
                statement.setString(3, TradeDirection.RECEIVED.name());
                statement.setString(4, item.serialize());
                statement.setDouble(5, item.amount());

                statement.addBatch();
            }

            statement.executeBatch(); // Execute batch insert
        } catch (SQLException e) {
            throw new RuntimeException("Error inserting trade results", e);
        }
    }


    public enum TradeDirection {

        GIVEN,
        RECEIVED

    }

}
