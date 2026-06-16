package com.artillexstudios.axtrade.trade.logging;

import com.artillexstudios.axapi.serializers.impl.ItemStackSerializer;
import com.artillexstudios.axtrade.trade.logging.type.CurrencyTradeItem;
import com.artillexstudios.axtrade.trade.logging.type.ItemStackTradeItem;
import net.kyori.adventure.text.Component;
import org.bukkit.inventory.ItemStack;

import java.sql.ResultSet;
import java.sql.SQLException;

public interface TradeItem {

    static TradeItem deserialize(ResultSet result) throws SQLException {
        String type = result.getString("type");
        String itemData = result.getString("item");
        double quantity = result.getDouble("quantity");

        if (type.equals(ItemType.CURRENCY.name())) {
            CurrencyTradeItem.CurrencyType currencyType = CurrencyTradeItem.CurrencyType.ECONOMY;
            if (itemData != null && !itemData.isBlank()) {
                currencyType = CurrencyTradeItem.CurrencyType.valueOf(itemData);
            }

            return new CurrencyTradeItem(currencyType, quantity);
        } else if (type.equals("ECONOMY") || type.equals("EXPERIENCE")) {
            CurrencyTradeItem.CurrencyType currencyType = CurrencyTradeItem.CurrencyType.valueOf(type);
            return new CurrencyTradeItem(currencyType, quantity);
        } else {
            if (itemData == null || itemData.isBlank()) {
                throw new SQLException("Trade item row has no serialized item data");
            }

            final ItemStack item = new ItemStackSerializer().deserialize(itemData);
            return new ItemStackTradeItem(item);
        }
    }

    Component display();
    ItemType itemType();
    double amount();

    String serialize();

}
