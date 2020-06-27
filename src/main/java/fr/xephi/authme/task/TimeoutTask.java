package fr.xephi.authme.task;

import fr.xephi.authme.data.auth.PlayerCache;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

/**
 * Kicks a player if he hasn't logged in (scheduled to run after a configured delay).
 */
public class TimeoutTask implements Runnable {

    @NotNull
    private final Player player;
    @NotNull
    private final String message;
    @NotNull
    private final PlayerCache playerCache;

    /**
     * Constructor for TimeoutTask.
     *
     * @param player the player to check
     * @param message the kick message
     * @param playerCache player cache instance
     */
    public TimeoutTask(@NotNull Player player, @NotNull String message, @NotNull PlayerCache playerCache) {
        this.message = message;
        this.player = player;
        this.playerCache = playerCache;
    }

    @Override
    public void run() {
        if (!playerCache.isAuthenticated(player.getName())) {
            player.kickPlayer(message);
        }
    }

}
