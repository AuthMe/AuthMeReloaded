package fr.xephi.authme.task;

import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.data.limbo.LimboCache;
import fr.xephi.authme.service.BukkitService;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

import static fr.xephi.authme.service.BukkitService.TICKS_PER_SECOND;

/**
 * Message shown to a player in a regular interval as long as he is not logged in.
 */
public class MessageTask implements Runnable {

    private final String name;
    private final String[] message;
    private final int interval;
    private final BukkitService bukkitService;
    private final LimboCache limboCache;
    private final PlayerCache playerCache;

    /*
     * Constructor.
     */
    public MessageTask(String name, String[] lines, int interval, BukkitService bukkitService,
                       LimboCache limboCache, PlayerCache playerCache) {
        this.name = name;
        this.message = lines;
        this.interval = interval;
        this.bukkitService = bukkitService;
        this.limboCache = limboCache;
        this.playerCache = playerCache;
    }

    @Override
    public void run() {
        if (playerCache.isAuthenticated(name)) {
            return;
        }

        for (Player player : bukkitService.getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(name)) {
                for (String ms : message) {
                    player.sendMessage(ms);
                }
                BukkitTask nextTask = bukkitService.runTaskLater(this, interval * TICKS_PER_SECOND);
                if (limboCache.hasPlayerData(name)) {
                    limboCache.getPlayerData(name).setMessageTask(nextTask);
                }
                return;
            }
        }
    }
}
