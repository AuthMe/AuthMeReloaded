package fr.xephi.authme.task;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.util.Utils;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 */
public class MessageTask implements Runnable {

    private AuthMe plugin;
    private String name;
    private String[] msg;
    private int interval;

    /**
     * Constructor for MessageTask.
     *
     * @param plugin   AuthMe
     * @param name     String
     * @param strings  String[]
     * @param interval int
     */
    public MessageTask(AuthMe plugin, String name, String[] strings,
                       int interval) {
        this.plugin = plugin;
        this.name = name;
        this.msg = strings;
        this.interval = interval;
    }

    /**
     * Method run.
     *
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        if (PlayerCache.getInstance().isAuthenticated(name))
            return;

        for (Player player : Utils.getOnlinePlayers()) {
            if (player.getName().toLowerCase().equals(name)) {
                for (String ms : msg) {
                    player.sendMessage(ms);
                }
                BukkitTask late = plugin.getServer().getScheduler().runTaskLaterAsynchronously(plugin, this, interval * 20);
                if (LimboCache.getInstance().hasLimboPlayer(name)) {
                    LimboCache.getInstance().getLimboPlayer(name).setMessageTaskId(late);
                }
                return;
            }
        }
    }
}
