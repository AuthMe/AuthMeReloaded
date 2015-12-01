package fr.xephi.authme.task;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

/**
 */
public class TimeoutTask implements Runnable {

    private final AuthMe plugin;
    private final String name;
    private final Messages m;
    private final Player player;

    /**
     * Constructor for TimeoutTask.
     *
     * @param plugin AuthMe
     * @param name   String
     * @param player Player
     */
    public TimeoutTask(AuthMe plugin, String name, Player player) {
        this.m = plugin.getMessages();
        this.plugin = plugin;
        this.name = name;
        this.player = player;
    }

    /**
     * Method getName.
     *
     * @return String
     */
    public String getName() {
        return name;
    }

    /**
     * Method run.
     *
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        if (PlayerCache.getInstance().isAuthenticated(name)) {
            return;
        }

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (player.isOnline()) {
                    player.kickPlayer(m.retrieveSingle(MessageKey.LOGIN_TIMEOUT_ERROR));
                }
            }
        });
    }
}
