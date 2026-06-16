package com.artillexstudios.axtrade.listeners;

import com.artillexstudios.axapi.utils.Cooldown;
import com.artillexstudios.axtrade.request.Requests;
import com.sk89q.worldedit.bukkit.BukkitAdapter;
import com.sk89q.worldedit.math.BlockVector3;
import com.sk89q.worldguard.WorldGuard;
import com.sk89q.worldguard.protection.ApplicableRegionSet;
import com.sk89q.worldguard.protection.managers.RegionManager;
import com.sk89q.worldguard.protection.regions.ProtectedRegion;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.jetbrains.annotations.NotNull;

import java.util.Set;

import static com.artillexstudios.axtrade.AxTrade.CONFIG;
import static com.artillexstudios.axtrade.AxTrade.MESSAGEUTILS;

public class EntityInteractListener implements Listener {
    private static final Cooldown<Player> cooldown = Cooldown.create();
    private static final Set<String> ALLOWED_REQUEST_REGIONS = Set.of("plots", "spawn");

    @EventHandler (ignoreCancelled = true)
    public void onInteract(@NotNull PlayerInteractEntityEvent event) {
        if (!CONFIG.getBoolean("shift-click-send-request", true)) return;

        Player player = event.getPlayer();
        if (!player.hasPermission("axtrade.trade")) return;
        if (cooldown.hasCooldown(player)) return;
        if (!player.isSneaking()) return;
        if (!(event.getRightClicked() instanceof Player sendTo)) return;
        if (!sendTo.isOnline()) return;
        if (!isInAllowedRequestRegion(player)) {
            MESSAGEUTILS.sendLang(player, "request.invalid-region");
            event.setCancelled(true);
            return;
        }

        cooldown.addCooldown(player, 100L);
        Requests.addRequest(player, sendTo);
        event.setCancelled(true);
    }

    private boolean isInAllowedRequestRegion(Player player) {
        if (!Bukkit.getPluginManager().isPluginEnabled("WorldGuard")) return false;

        RegionManager regionManager = WorldGuard.getInstance()
                .getPlatform()
                .getRegionContainer()
                .get(BukkitAdapter.adapt(player.getWorld()));
        if (regionManager == null) return false;

        ApplicableRegionSet regions = regionManager.getApplicableRegions(BlockVector3.at(
                player.getLocation().getBlockX(),
                player.getLocation().getBlockY(),
                player.getLocation().getBlockZ()
        ));
        for (ProtectedRegion region : regions) {
            if (ALLOWED_REQUEST_REGIONS.contains(region.getId().toLowerCase())) {
                return true;
            }
        }

        return false;
    }
}
