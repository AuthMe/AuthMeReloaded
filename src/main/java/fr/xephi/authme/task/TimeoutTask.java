package fr.xephi.authme.task;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.backup.FileCache;
import fr.xephi.authme.settings.Messages;

public class TimeoutTask implements Runnable {

    private AuthMe plugin;
    private String name;
    private Messages m = Messages.getInstance();
    private FileCache playerCache;
    private Player player;

    public TimeoutTask(AuthMe plugin, String name, Player player) {
        this.plugin = plugin;
        this.name = name;
        this.player = player;
        this.playerCache = new FileCache(plugin);
    }

    public String getName() {
        return name;
    }

    @Override
    public void run() {
        if (PlayerCache.getInstance().isAuthenticated(name))
            return;

        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {

            @Override
            public void run() {
                if (player.isOnline())
                    player.kickPlayer(m.send("timeout")[0]);
            }
        });
    }
}
