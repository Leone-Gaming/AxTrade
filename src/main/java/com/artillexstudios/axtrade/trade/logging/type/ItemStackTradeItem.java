package com.artillexstudios.axtrade.trade.logging.type;

import com.artillexstudios.axapi.serializers.impl.ItemStackSerializer;
import com.artillexstudios.axtrade.trade.logging.ItemType;
import com.artillexstudios.axtrade.trade.logging.TradeItem;
import com.artillexstudios.axtrade.trade.logging.TradeLog;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.HoverEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import org.bukkit.inventory.ItemStack;

import java.sql.Connection;
import java.text.DecimalFormat;

public class ItemStackTradeItem implements TradeItem {

    private static final DecimalFormat DECIMAL_FORMAT = new DecimalFormat("#.##");

    private final ItemStack stack;

    public ItemStackTradeItem(ItemStack stack) {
        this.stack = stack;
    }

    @Override
    public Component display() {
        return Component.text(stack.getAmount() + "x ")
                .color(NamedTextColor.WHITE)
                .append(
                        stack.displayName()
                                .hoverEvent(stack)
                );
    }

    @Override
    public ItemType itemType() {
        return ItemType.ITEM;
    }

    @Override
    public double amount() {
        return stack.getAmount();
    }

    @Override
    public String serialize() {
        return new ItemStackSerializer().serialize(stack);
    }
}
