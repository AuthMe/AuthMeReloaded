package fr.xephi.authme.task;

import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.util.BukkitService;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;

/**
 */
public class MessageTask implements Runnable {

    private final BukkitService bukkitService;
    private final String name;
    private final String[] msg;
    private final int interval;

    /*
     * Constructor.
     */
    public MessageTask(BukkitService bukkitService, String name, String[] lines, int interval) {
        this.bukkitService = bukkitService;
        this.name = name;
        this.msg = lines;
        this.interval = interval;
    }

    /*
     * Constructor.
     */
    public MessageTask(BukkitService bukkitService, Messages messages, String name, MessageKey messageKey,
                       int interval) {
        this(bukkitService, name, messages.retrieve(messageKey), interval);
    }

    @Override
    public void run() {
        if (PlayerCache.getInstance().isAuthenticated(name)) {
            return;
        }

        for (Player player : bukkitService.getOnlinePlayers()) {
            if (player.getName().equalsIgnoreCase(name)) {
                for (String ms : msg) {
                    player.sendMessage(ms);
                }
                BukkitTask nextTask = bukkitService.runTaskLater(this, interval * 20);
                if (LimboCache.getInstance().hasLimboPlayer(name)) {
                    LimboCache.getInstance().getLimboPlayer(name).setMessageTask(nextTask);
                }
                return;
            }
        }
    }
}
