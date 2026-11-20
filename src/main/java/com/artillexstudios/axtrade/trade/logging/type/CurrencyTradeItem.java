package com.artillexstudios.axtrade.trade.logging.type;

import com.artillexstudios.axtrade.trade.logging.ItemType;
import com.artillexstudios.axtrade.trade.logging.TradeItem;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;

import java.text.DecimalFormat;

public class CurrencyTradeItem implements TradeItem {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");

    private final CurrencyType currency;
    private final double amount;

    public CurrencyTradeItem(CurrencyType currency, double amount) {
        this.currency = currency;
        this.amount = amount;
    }

    @Override
    public Component display() {
        return Component.text(DECIMAL_FORMAT.format(amount) + " " + currency.displayName())
                .color(currency == CurrencyType.EXPERIENCE ? NamedTextColor.AQUA : NamedTextColor.GREEN);
    }

    @Override
    public ItemType itemType() {
        return ItemType.CURRENCY;
    }

    @Override
    public double amount() {
        return amount;
    }

    @Override
    public String serialize() {
        return null;
    }

    public enum CurrencyType {

        ECONOMY("dollars"),
        EXPERIENCE("experience");

        public final String displayName;

        CurrencyType(String displayName) {
            this.displayName = displayName;
        }

        public String displayName() {
            return displayName;
        }

    }

}
