package fr.xephi.authme.process.register;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.task.MessageTask;
import fr.xephi.authme.task.TimeoutTask;
import fr.xephi.authme.util.Utils;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

/**
 */
public class ProcessSyncEmailRegister implements Runnable {

    protected final Player player;
    protected final String name;
    private final AuthMe plugin;
    private final Messages m;

    /**
     * Constructor for ProcessSyncEmailRegister.
     *
     * @param player Player
     * @param plugin AuthMe
     */
    public ProcessSyncEmailRegister(Player player, AuthMe plugin) {
        this.m = plugin.getMessages();
        this.player = player;
        this.name = player.getName().toLowerCase();
        this.plugin = plugin;
    }

    /**
     * Method run.
     *
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        LimboPlayer limbo = LimboCache.getInstance().getLimboPlayer(name);
        if (!Settings.getRegisteredGroup.isEmpty()) {
            Utils.setGroup(player, Utils.GroupType.REGISTERED);
        }
        m.send(player, MessageKey.ACCOUNT_NOT_ACTIVATED);
        int time = Settings.getRegistrationTimeout * 20;
        int msgInterval = Settings.getWarnMessageInterval;

        BukkitScheduler sched = plugin.getServer().getScheduler();
        if (time != 0 && limbo != null) {
            limbo.getTimeoutTaskId().cancel();
            BukkitTask id = sched.runTaskLaterAsynchronously(plugin, new TimeoutTask(plugin, name, player), time);
            limbo.setTimeoutTaskId(id);
        }
        if (limbo != null) {
            limbo.getMessageTaskId().cancel();
            BukkitTask nwMsg = sched.runTaskAsynchronously(plugin, new MessageTask(plugin, name, m.retrieve(MessageKey.LOGIN_MESSAGE), msgInterval));
            limbo.setMessageTaskId(nwMsg);
        }

        player.saveData();
        if (!Settings.noConsoleSpam)
            ConsoleLogger.info(player.getName() + " registered " + plugin.getIP(player));
    }

}
