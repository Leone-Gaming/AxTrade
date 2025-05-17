package com.artillexstudios.axtrade.trade.logging;

import com.artillexstudios.axtrade.trade.Trade;
import org.bukkit.Bukkit;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class TradeLogger {

    private TradeLogger() {}

    public static CompletableFuture<Void> logTrade(Trade trade) {
        return CompletableFuture.runAsync(() -> {
            TradeResult playerOneResult = new TradeResult(trade.getPlayer1(), trade.getPlayer2());
            TradeResult playerTwoResult = new TradeResult(trade.getPlayer2(), trade.getPlayer1());

            TradeLog tradeLog = new TradeLog(
                    UUID.randomUUID(),
                    trade.getPlayer1().getPlayer().getUniqueId(),
                    trade.getPlayer2().getPlayer().getUniqueId(),
                    System.currentTimeMillis(),
                    playerOneResult,
                    playerTwoResult
            );

            tradeLog.insert();
        });
    }


}
