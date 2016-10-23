package fr.xephi.authme.task;

import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.service.BukkitService;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

/**
 * Message shown to a player in a regular interval as long as he is not logged in.
 */
public class MessageTask extends BukkitRunnable {

    private final String name;
    private final String[] message;
    private final BukkitService bukkitService;
    private final PlayerCache playerCache;
    private boolean isMuted;

    /*
     * Constructor.
     */
    public MessageTask(String name, String[] lines, BukkitService bukkitService, PlayerCache playerCache) {
        this.name = name;
        this.message = lines;
        this.bukkitService = bukkitService;
        this.playerCache = playerCache;
        isMuted = false;
    }

    public void setMuted(boolean isMuted) {
        this.isMuted = isMuted;
    }

    @Override
    public void run() {
        if (playerCache.isAuthenticated(name)) {
            cancel();
        }

        if (isMuted) {
            return;
        }

        for (Player player : bukkitService.getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(name)) {
                for (String ms : message) {
                    player.sendMessage(ms);
                }
                break;
            }
        }
    }
}
