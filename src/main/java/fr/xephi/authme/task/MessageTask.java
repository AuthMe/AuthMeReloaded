package fr.xephi.authme.task;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.limbo.LimboCache;

public class MessageTask implements Runnable {

    private AuthMe plugin;
    private String name;
    private String[] msg;
    private int interval;

    public MessageTask(AuthMe plugin, String name, String[] strings,
            int interval) {
        this.plugin = plugin;
        this.name = name;
        this.msg = strings;
        this.interval = interval;
    }

    @Override
    public void run() {
        if (PlayerCache.getInstance().isAuthenticated(name))
            return;

        for (Player player : plugin.getServer().getOnlinePlayers()) {
            if (player.getName().equals(name)) {
                for (String ms : msg) {
                    player.sendMessage(ms);
                }
                BukkitScheduler sched = plugin.getServer().getScheduler();
                int late = sched.scheduleSyncDelayedTask(plugin, this, interval * 20);
                if (LimboCache.getInstance().hasLimboPlayer(name)) {
                    LimboCache.getInstance().getLimboPlayer(name).setMessageTaskId(late);
                }
            }
        }
    }
}
