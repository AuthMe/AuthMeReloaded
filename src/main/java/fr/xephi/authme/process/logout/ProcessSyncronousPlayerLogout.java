package fr.xephi.authme.process.logout;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.events.LogoutEvent;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.task.MessageTask;
import fr.xephi.authme.task.TimeoutTask;

public class ProcessSyncronousPlayerLogout implements Runnable {

    protected Player player;
    protected AuthMe plugin;
    protected String name;
    private Messages m = Messages.getInstance();

    public ProcessSyncronousPlayerLogout(Player player, AuthMe plugin) {
        this.player = player;
        this.plugin = plugin;
        this.name = player.getName().toLowerCase();
    }

    @Override
    public void run() {
        if (plugin.sessions.containsKey(name))
            plugin.sessions.get(name).cancel();
        plugin.sessions.remove(name);
        int delay = Settings.getRegistrationTimeout * 20;
        int interval = Settings.getWarnMessageInterval;
        BukkitScheduler sched = player.getServer().getScheduler();
        if (delay != 0) {
            BukkitTask id = sched.runTaskLaterAsynchronously(plugin, new TimeoutTask(plugin, name, player), delay);
            LimboCache.getInstance().getLimboPlayer(name).setTimeoutTaskId(id);
        }
        BukkitTask msgT = sched.runTaskAsynchronously(plugin, new MessageTask(plugin, name, m.send("login_msg"), interval));
        LimboCache.getInstance().getLimboPlayer(name).setMessageTaskId(msgT);
        if (player.isInsideVehicle() && player.getVehicle() != null)
            player.getVehicle().eject();
        if (Settings.applyBlindEffect)
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Settings.getRegistrationTimeout * 20, 2));
        player.setOp(false);
        if (!Settings.isMovementAllowed) {
            player.setAllowFlight(true);
            player.setFlying(true);
            if (!Settings.isMovementAllowed && Settings.isRemoveSpeedEnabled) {
                player.setFlySpeed(0.0f);
                player.setWalkSpeed(0.0f);
            }
        }
        // Player is now logout... Time to fire event !
        Bukkit.getServer().getPluginManager().callEvent(new LogoutEvent(player));
        m.send(player, "logout");
        ConsoleLogger.info(player.getName() + " logged out");
    }

}
