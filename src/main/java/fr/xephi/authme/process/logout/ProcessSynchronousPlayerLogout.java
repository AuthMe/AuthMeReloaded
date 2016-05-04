package fr.xephi.authme.process.logout;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.events.LogoutEvent;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.process.Process;
import fr.xephi.authme.process.ProcessService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.task.MessageTask;
import fr.xephi.authme.task.TimeoutTask;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

/**
 */
public class ProcessSynchronousPlayerLogout implements Process {

    private final Player player;
    private final AuthMe plugin;
    private final String name;
    private final ProcessService service;

    /**
     * Constructor for ProcessSynchronousPlayerLogout.
     *
     * @param player Player
     * @param plugin AuthMe
     * @param service The process service
     */
    public ProcessSynchronousPlayerLogout(Player player, AuthMe plugin, ProcessService service) {
        this.player = player;
        this.plugin = plugin;
        this.name = player.getName().toLowerCase();
        this.service = service;
    }

    protected void sendBungeeMessage() {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL");
        out.writeUTF("AuthMe");
        out.writeUTF("logout;" + name);
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    protected void restoreSpeedEffect() {
        if (Settings.isRemoveSpeedEnabled) {
            player.setWalkSpeed(0.0F);
            player.setFlySpeed(0.0F);
        }
    }

    @Override
    public void run() {
        if (plugin.sessions.containsKey(name)) {
            plugin.sessions.get(name).cancel();
            plugin.sessions.remove(name);
        }
        if (Settings.protectInventoryBeforeLogInEnabled) {
            plugin.inventoryProtector.sendBlankInventoryPacket(player);
        }
        int timeOut = service.getProperty(RestrictionSettings.TIMEOUT) * 20;
        int interval = service.getProperty(RegistrationSettings.MESSAGE_INTERVAL);
        if (timeOut != 0) {
            BukkitTask id = service.runTaskLater(new TimeoutTask(plugin, name, player), timeOut);
            LimboCache.getInstance().getLimboPlayer(name).setTimeoutTask(id);
        }
        BukkitTask msgT = service.runTask(new MessageTask(service.getBukkitService(), plugin.getMessages(),
            name, MessageKey.LOGIN_MESSAGE, interval));
        LimboCache.getInstance().getLimboPlayer(name).setMessageTask(msgT);
        if (player.isInsideVehicle() && player.getVehicle() != null) {
            player.getVehicle().eject();
        }
        if (service.getProperty(RegistrationSettings.APPLY_BLIND_EFFECT)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, timeOut, 2));
        }
        player.setOp(false);
        restoreSpeedEffect();
        // Player is now logout... Time to fire event !
        Bukkit.getServer().getPluginManager().callEvent(new LogoutEvent(player));
        if (Settings.bungee) {
            sendBungeeMessage();
        }
        service.send(player, MessageKey.LOGOUT_SUCCESS);
        ConsoleLogger.info(player.getName() + " logged out");
    }

}
