package fr.xephi.authme.process.logout;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.events.LogoutEvent;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.task.MessageTask;
import fr.xephi.authme.task.TimeoutTask;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

/**
 */
public class ProcessSyncronousPlayerLogout implements Runnable {

    protected final Player player;
    protected final AuthMe plugin;
    protected final String name;
    private final Messages m;

    /**
     * Constructor for ProcessSyncronousPlayerLogout.
     *
     * @param player Player
     * @param plugin AuthMe
     */
    public ProcessSyncronousPlayerLogout(Player player, AuthMe plugin) {
        this.m = plugin.getMessages();
        this.player = player;
        this.plugin = plugin;
        this.name = player.getName().toLowerCase();
    }

    protected void sendBungeeMessage() {
    	ByteArrayOutputStream b = new ByteArrayOutputStream();
    	DataOutputStream out = new DataOutputStream(b);
    	try {
    		String str = "AuthMe;logout;" + name;
    		out.writeUTF("Forward");
    		out.writeUTF("ALL");
    		out.writeUTF("AuthMe");
    		out.writeShort(str.length());
    		out.writeUTF(str);
    		player.sendPluginMessage(plugin, "BungeeCord", b.toByteArray());
    	} catch (Exception e)
    	{}
    }

    /**
     * Method run.
     *
     * @see java.lang.Runnable#run()
     */
    @Override
    public void run() {
        if (plugin.sessions.containsKey(name)) {
            plugin.sessions.get(name).cancel();
            plugin.sessions.remove(name);
        }
        if (Settings.protectInventoryBeforeLogInEnabled) {
            plugin.inventoryProtector.sendBlankInventoryPacket(player);
        }
        int timeOut = Settings.getRegistrationTimeout * 20;
        int interval = Settings.getWarnMessageInterval;
        BukkitScheduler sched = player.getServer().getScheduler();
        if (timeOut != 0) {
            BukkitTask id = sched.runTaskLaterAsynchronously(plugin, new TimeoutTask(plugin, name, player), timeOut);
            LimboCache.getInstance().getLimboPlayer(name).setTimeoutTaskId(id);
        }
        BukkitTask msgT = sched.runTaskAsynchronously(plugin, new MessageTask(plugin, name, m.retrieve(MessageKey.LOGIN_MESSAGE), interval));
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
        if (Settings.bungee)
        	sendBungeeMessage();
        m.send(player, MessageKey.LOGOUT_SUCCESS);
        ConsoleLogger.info(player.getName() + " logged out");
    }

}
