package fr.xephi.authme.process.register;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
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
        if (time != 0) {
            LimboCache.getInstance().getLimboPlayer(name).getTimeoutTaskId().cancel();
            BukkitTask id = Bukkit.getScheduler().runTaskLaterAsynchronously(plugin, new TimeoutTask(plugin, name, player), time);
            LimboCache.getInstance().getLimboPlayer(name).setTimeoutTaskId(id);
        }

        LimboCache.getInstance().getLimboPlayer(name).getMessageTaskId().cancel();
        BukkitTask nwMsg = Bukkit.getScheduler().runTaskAsynchronously(plugin, new MessageTask(plugin, name, m.send("login_msg"), msgInterval));
        LimboCache.getInstance().getLimboPlayer(name).setMessageTaskId(nwMsg);
        player.saveData();
        if (!Settings.noConsoleSpam)
            ConsoleLogger.info(player.getName() + " registered " + plugin.getIP(player));
    }

}
