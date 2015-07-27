package fr.xephi.authme.process.register;

import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.Utils;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.task.MessageTask;
import fr.xephi.authme.task.TimeoutTask;

public class ProcessSyncronousEmailRegister implements Runnable {

    protected Player player;
    protected String name;
    private AuthMe plugin;
    private Messages m = Messages.getInstance();

    public ProcessSyncronousEmailRegister(Player player, AuthMe plugin) {
        this.player = player;
        this.name = player.getName().toLowerCase();
        this.plugin = plugin;
    }

    @Override
    public void run() {
        if (!Settings.getRegisteredGroup.isEmpty()) {
            Utils.getInstance().setGroup(player, Utils.groupType.REGISTERED);
        }
        m.send(player, "vb_nonActiv");
        int time = Settings.getRegistrationTimeout * 20;
        int msgInterval = Settings.getWarnMessageInterval;
        
        BukkitScheduler sched = plugin.getServer().getScheduler();
        if (time != 0) {
        	if (LimboCache.getInstance().getLimboPlayer(name).getTimeoutTaskId() != null)
        		LimboCache.getInstance().getLimboPlayer(name).getTimeoutTaskId().cancel();
            BukkitTask id = sched.runTaskLaterAsynchronously(plugin, new TimeoutTask(plugin, name, player), time);
            LimboCache.getInstance().getLimboPlayer(name).setTimeoutTaskId(id);
        }
        if (LimboCache.getInstance().getLimboPlayer(name).getMessageTaskId() != null)
            LimboCache.getInstance().getLimboPlayer(name).getMessageTaskId().cancel();
        BukkitTask nwMsg = sched.runTaskAsynchronously(plugin, new MessageTask(plugin, name, m.send("login_msg"), msgInterval));
        LimboCache.getInstance().getLimboPlayer(name).setMessageTaskId(nwMsg);

        player.saveData();
        if (!Settings.noConsoleSpam)
            ConsoleLogger.info(player.getName() + " registered " + plugin.getIP(player));
    }

}
