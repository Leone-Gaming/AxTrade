package com.artillexstudios.axtrade.commands;

import com.artillexstudios.axapi.utils.StringUtils;
import com.artillexstudios.axtrade.AxTrade;
import com.artillexstudios.axtrade.hooks.HookManager;
import com.artillexstudios.axtrade.lang.LanguageManager;
import com.artillexstudios.axtrade.request.Requests;
import com.artillexstudios.axtrade.trade.Trade;
import com.artillexstudios.axtrade.trade.Trades;
import com.artillexstudios.axtrade.trade.logging.TradeItem;
import com.artillexstudios.axtrade.trade.logging.TradeLog;
import com.artillexstudios.axtrade.trade.logging.TradeResult;
import com.artillexstudios.axtrade.trade.logging.database.Database;
import com.artillexstudios.axtrade.utils.CommandMessages;
import com.artillexstudios.axtrade.utils.NumberUtils;
import com.artillexstudios.axtrade.utils.SoundUtils;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.leonemc.library.cache.UUIDCache;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import revxrsal.commands.annotation.AutoComplete;
import revxrsal.commands.annotation.DefaultFor;
import revxrsal.commands.annotation.Optional;
import revxrsal.commands.annotation.Subcommand;
import revxrsal.commands.bukkit.BukkitCommandHandler;
import revxrsal.commands.bukkit.annotation.CommandPermission;
import revxrsal.commands.orphan.OrphanCommand;
import revxrsal.commands.orphan.Orphans;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;


import static com.artillexstudios.axtrade.AxTrade.CONFIG;
import static com.artillexstudios.axtrade.AxTrade.GUIS;
import static com.artillexstudios.axtrade.AxTrade.HOOKS;
import static com.artillexstudios.axtrade.AxTrade.LANG;
import static com.artillexstudios.axtrade.AxTrade.MESSAGEUTILS;
import static com.artillexstudios.axtrade.AxTrade.TOGGLED;

@CommandPermission(value = "axtrade.trade")
public class Commands implements OrphanCommand {

    DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("M/d/yyyy h:mma", Locale.ENGLISH)
            .withZone(ZoneId.systemDefault());

    public void help(@NotNull CommandSender sender) {
        if (sender.hasPermission("axtrade.admin")) {
            for (String m : LANG.getStringList("admin-help")) {
                sender.sendMessage(StringUtils.formatToString(m));
            }
        } else {
            for (String m : LANG.getStringList("player-help")) {
                sender.sendMessage(StringUtils.formatToString(m));
            }
        }
    }

    @DefaultFor({"~"})
    public void trade(@NotNull Player sender, @Optional Player other) {
        if (other == null) {
            help(sender);
            return;
        }

        Requests.addRequest(sender, other);
    }

    @Subcommand("accept")
    public void accept(@NotNull Player sender, @NotNull Player other) {
        var request = Requests.getRequest(sender, other);
        if (request == null || request.getSender().equals(sender) || !request.isActive()) {
            MESSAGEUTILS.sendLang(sender, "request.no-request", Map.of("%player%", other.getName()));
            return;
        }

        Requests.addRequest(sender, other);
    }

    @Subcommand("deny")
    public void deny(@NotNull Player sender, @NotNull Player other) {
        var request = Requests.getRequest(sender, other);
        if (request == null || request.getSender().equals(sender) || !request.isActive()) {
            MESSAGEUTILS.sendLang(sender, "request.no-request", Map.of("%player%", other.getName()));
            return;
        }

        request.deactivate();
        MESSAGEUTILS.sendLang(request.getSender(), "request.deny-sender", Map.of("%player%", request.getReceiver().getName()));
        MESSAGEUTILS.sendLang(request.getReceiver(), "request.deny-receiver", Map.of("%player%", request.getSender().getName()));
        SoundUtils.playSound(request.getSender(), "deny");
        SoundUtils.playSound(request.getReceiver(), "deny");
    }

    @CommandPermission("axtrade.toggle")
    @Subcommand("toggle")
    public void toggle(@NotNull Player sender) {
        boolean toggled = TOGGLED.getBoolean("toggled." + sender.getUniqueId(), false);
        if (toggled) {
            TOGGLED.getBackingDocument().remove("toggled." + sender.getUniqueId());
            MESSAGEUTILS.sendLang(sender, "toggle.enabled");
        } else {
            TOGGLED.set("toggled." + sender.getUniqueId(), true);
            MESSAGEUTILS.sendLang(sender, "toggle.disabled");
        }
        TOGGLED.save();
    }

    @Subcommand("reload")
    @CommandPermission(value = "axtrade.admin")
    public void reload(@NotNull CommandSender sender) {
        Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#00FFDD[AxTrade] &#AAFFDDReloading configuration..."));
        if (!CONFIG.reload()) {
            MESSAGEUTILS.sendLang(sender, "reload.failed", Map.of("%file%", "config.yml"));
            return;
        }
        Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#00FFDD╠ &#AAFFDDReloaded &fconfig.yml&#AAFFDD!"));

        if (!LANG.reload()) {
            MESSAGEUTILS.sendLang(sender, "reload.failed", Map.of("%file%", "lang.yml"));
            return;
        }
        Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#00FFDD╠ &#AAFFDDReloaded &flang.yml&#AAFFDD!"));

        if (!GUIS.reload()) {
            MESSAGEUTILS.sendLang(sender, "reload.failed", Map.of("%file%", "guis.yml"));
            return;
        }
        Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#00FFDD╠ &#AAFFDDReloaded &fguis.yml&#AAFFDD!"));

        if (!HOOKS.reload()) {
            MESSAGEUTILS.sendLang(sender, "reload.failed", Map.of("%file%", "currencies.yml"));
            return;
        }
        Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#00FFDD╠ &#AAFFDDReloaded &fcurrencies.yml&#AAFFDD!"));

        LanguageManager.reload();

        HookManager.updateHooks();
        NumberUtils.reload();

        Bukkit.getConsoleSender().sendMessage(StringUtils.formatToString("&#00FFDD╚ &#AAFFDDSuccessful reload!"));
        MESSAGEUTILS.sendLang(sender, "reload.success");
    }

    @Subcommand("force")
    @CommandPermission(value = "axtrade.admin")
    public void force(@NotNull Player sender, Player other) {
        if (sender.equals(other)) {
            MESSAGEUTILS.sendLang(sender, "request.cant-trade-self");
            return;
        }
        Trades.addTrade(sender, other);
    }

    @Subcommand("preview")
    @CommandPermission(value = "axtrade.admin")
    public void preview(@NotNull Player sender) {
        new Trade(sender, sender);
        MESSAGEUTILS.sendLang(sender, "trade.preview-info");
    }

    @Subcommand("logs")
    @CommandPermission(value = "axtrade.admin")
    @AutoComplete("@players")
    public void logs(@NotNull Player sender, @NotNull String target, int page) {
        final UUID uuid = UUIDCache.INSTANCE.uniqueId(target);

        if (uuid == null) {
            sender.sendMessage(Component.text("Target not found!", NamedTextColor.RED));
            return;
        }

        Database.getTradeLogs(uuid)
                .exceptionally(ex -> {
                    sender.sendMessage(Component.text("An error occurred while fetching trade logs.", NamedTextColor.RED));
                    ex.printStackTrace();
                    return null;
                }).thenAcceptAsync(tradeLogs -> {
                    if (tradeLogs.isEmpty()) {
                        sender.sendMessage(Component.text("No trade logs found for " + target, NamedTextColor.RED));
                        return;
                    }

                    int pageSize = 5; // Number of logs per page
                    int totalPages = (int) Math.ceil((double) tradeLogs.size() / pageSize);

                    if (page < 1 || page > totalPages) {
                        sender.sendMessage(Component.text("Invalid page number. Must be between 1 and " + totalPages, NamedTextColor.RED));
                        return;
                    }

                    Component message = Component.text()
                            .append(Component.text("-----------------------------------------", NamedTextColor.GRAY))
                            .append(Component.newline())
                            .append(Component.text("Trade logs for ", NamedTextColor.RED))
                            .append(Component.text(target, NamedTextColor.WHITE))
                            .append(Component.newline())
                            .append(Component.text("-----------------------------------------", NamedTextColor.GRAY))
                            .append(Component.newline())
                            .build();

                    int start = (page - 1) * pageSize;
                    int end = Math.min(start + pageSize, tradeLogs.size());

                    for (int i = start; i < end; i++) {
                        TradeLog log = tradeLogs.get(i);
                        String name = UUIDCache.INSTANCE.name(log.getOther(uuid));
                        String formattedDate = DATE_FORMATTER.format(Instant.ofEpochMilli(log.time()));

                        if (name == null) {
                            name = uuid.toString();
                        }

                        message = message.append(Component.text(formattedDate, NamedTextColor.GRAY)) // Assuming log.getFormattedDate() returns the formatted date
                                .append(Component.text(" - ", NamedTextColor.DARK_GRAY))
                                .append(Component.text("Traded with ", NamedTextColor.GRAY))
                                .append(Component.text(name, NamedTextColor.YELLOW))
                                .append(Component.newline());

                        TradeResult result = log.result(uuid);

                        // Received items
                        for (TradeItem tradeItem : result.received()) {
                            message = message.append(Component.text(" + ", NamedTextColor.GREEN))
                                    .append(tradeItem.display())
                                    .append(Component.newline());
                        }

                        // Given items
                        for (TradeItem tradeItem : result.given()) {
                            message = message.append(Component.text(" - ", NamedTextColor.RED))
                                    .append(tradeItem.display())
                                    .append(Component.newline());
                        }

                        message = message.append(Component.newline());
                    }


                    message = message.append(Component.newline())
                            .append(Component.text("You are currently on page ", NamedTextColor.GRAY))
                            .append(Component.text(page + "/" + totalPages, NamedTextColor.WHITE))
                            .append(Component.newline())
                            .append(Component.text("Type ", NamedTextColor.GRAY))
                            .append(Component.text("/trade logs " + target + " <page>", NamedTextColor.WHITE))
                            .append(Component.text(" to navigate between pages", NamedTextColor.GRAY))
                            .append(Component.newline())
                            .append(Component.text("-----------------------------------------", NamedTextColor.GRAY));

                    sender.sendMessage(message);
                });
    }

    private static BukkitCommandHandler handler = null;

    public static void registerCommand() {
        if (handler == null) {
            handler = BukkitCommandHandler.create(AxTrade.getInstance());
            handler.getTranslator().add(new CommandMessages());
            handler.setLocale(new Locale("en", "US"));
        }
        handler.unregisterAllCommands();
        handler.register(Orphans.path(CONFIG.getStringList("command-aliases").toArray(String[]::new)).handler(new Commands()));
        handler.registerBrigadier();
    }
}
