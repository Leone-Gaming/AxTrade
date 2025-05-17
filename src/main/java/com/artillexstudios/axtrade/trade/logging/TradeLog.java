package com.artillexstudios.axtrade.trade.logging;

import com.artillexstudios.axtrade.trade.logging.database.Database;
import org.bukkit.Bukkit;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class TradeLog {

    private final UUID tradeId;
    private final UUID playerOne;
    private final TradeResult playerOneResult;
    private final UUID playerTwo;
    private final TradeResult playerTwoResult;
    private final long time;

    public TradeLog(UUID tradeId, UUID playerOne, UUID playerTwo, long time, TradeResult playerOneResult, TradeResult playerTwoResult) {
        this.tradeId = tradeId;
        this.playerOne = playerOne;
        this.playerTwo = playerTwo;
        this.time = time;

        this.playerOneResult = playerOneResult;
        this.playerTwoResult = playerTwoResult;
    }

    public UUID tradeId() {
        return tradeId;
    }

    public UUID playerOne() {
        return playerOne;
    }

    public UUID playerTwo() {
        return playerTwo;
    }

    public long time() {
        return time;
    }

    public TradeResult result(UUID player) {
        if (player.equals(playerOne)) return playerOneResult;
        if (player.equals(playerTwo)) return playerTwoResult;

        throw new IllegalArgumentException(player + " was not part of the trade!");
    }

    public void insert() {
        Database.logTrade(this);
    }

    public Collection<TradeResult> results() {
        return List.of(playerOneResult, playerTwoResult);
    }

    public UUID getOther(UUID uuid) {
        if (uuid.equals(playerOne)) return playerTwo;
        if (uuid.equals(playerTwo)) return playerOne;

        throw new IllegalArgumentException(uuid + " was not part of the trade!");
    }
}
