package fr.xephi.authme.process.logout;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.events.LogoutEvent;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.process.ProcessService;
import fr.xephi.authme.process.SynchronousProcess;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.task.MessageTask;
import fr.xephi.authme.task.TimeoutTask;
import fr.xephi.authme.util.BukkitService;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import javax.inject.Inject;

import static fr.xephi.authme.util.BukkitService.TICKS_PER_SECOND;


public class ProcessSynchronousPlayerLogout implements SynchronousProcess {

    @Inject
    private AuthMe plugin;

    @Inject
    private ProcessService service;

    @Inject
    private LimboCache limboCache;

    @Inject
    private BukkitService bukkitService;

    ProcessSynchronousPlayerLogout() { }

    private void sendBungeeMessage(Player player) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL");
        out.writeUTF("AuthMe");
        out.writeUTF("logout;" + player.getName());
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    private void restoreSpeedEffect(Player player) {
        if (service.getProperty(RestrictionSettings.REMOVE_SPEED)) {
            player.setWalkSpeed(0.0F);
            player.setFlySpeed(0.0F);
        }
    }

    public void processSyncLogout(Player player) {
        final String name = player.getName().toLowerCase();
        if (plugin.sessions.containsKey(name)) {
            plugin.sessions.get(name).cancel();
            plugin.sessions.remove(name);
        }
        if (Settings.protectInventoryBeforeLogInEnabled) {
            plugin.inventoryProtector.sendBlankInventoryPacket(player);
        }
        int timeOut = service.getProperty(RestrictionSettings.TIMEOUT) * TICKS_PER_SECOND;
        int interval = service.getProperty(RegistrationSettings.MESSAGE_INTERVAL);
        if (timeOut != 0) {
            BukkitTask id = bukkitService.runTaskLater(new TimeoutTask(plugin, name, player), timeOut);
            limboCache.getLimboPlayer(name).setTimeoutTask(id);
        }
        BukkitTask msgT = bukkitService.runTask(new MessageTask(bukkitService, plugin.getMessages(),
            name, MessageKey.LOGIN_MESSAGE, interval));
        limboCache.getLimboPlayer(name).setMessageTask(msgT);
        if (player.isInsideVehicle() && player.getVehicle() != null) {
            player.getVehicle().eject();
        }
        if (service.getProperty(RegistrationSettings.APPLY_BLIND_EFFECT)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, timeOut, 2));
        }
        player.setOp(false);
        restoreSpeedEffect(player);
        // Player is now logout... Time to fire event !
        bukkitService.callEvent(new LogoutEvent(player));
        if (service.getProperty(HooksSettings.BUNGEECORD)) {
            sendBungeeMessage(player);
        }
        service.send(player, MessageKey.LOGOUT_SUCCESS);
        ConsoleLogger.info(player.getName() + " logged out");
    }

}
