package com.artillexstudios.axtrade.utils;

import com.artillexstudios.axtrade.hooks.currency.CurrencyHook;
import com.artillexstudios.axtrade.lang.LanguageManager;
import org.bukkit.inventory.ItemStack;
import org.jetbrains.annotations.NotNull;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Utils {
    private static final Pattern LEGACY_HEX_PATTERN = Pattern.compile("[§&][xX]((?:[§&][0-9a-fA-F]){6})");
    private static final Pattern LEGACY_CODE_PREFIX = Pattern.compile("[§&]");

    @NotNull
    public static String getFormattedItemName(@NotNull ItemStack itemStack) {
        if (itemStack.getItemMeta() == null || itemStack.getItemMeta().getDisplayName().isBlank()) {
            return LanguageManager.getTranslated(itemStack.getType());
        }
        return convertLegacyHexColors(itemStack.getItemMeta().getDisplayName()).replace("§", "&");
    }

    @NotNull
    public static String convertLegacyHexColors(@NotNull String text) {
        Matcher matcher = LEGACY_HEX_PATTERN.matcher(text);
        StringBuilder result = new StringBuilder();
        while (matcher.find()) {
            String hex = LEGACY_CODE_PREFIX.matcher(matcher.group(1)).replaceAll("");
            matcher.appendReplacement(result, Matcher.quoteReplacement("&#" + hex));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    @NotNull
    public static String getFormattedCurrency(@NotNull CurrencyHook currencyHook) {
        return currencyHook.getSettings().getOrDefault("name", currencyHook.getName()).toString();
    }
}
