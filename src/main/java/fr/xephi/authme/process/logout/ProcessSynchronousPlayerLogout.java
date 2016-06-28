package fr.xephi.authme.process.logout;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.SessionManager;
import fr.xephi.authme.events.LogoutEvent;
import fr.xephi.authme.listener.protocollib.ProtocolLibService;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.permission.AuthGroupType;
import fr.xephi.authme.process.ProcessService;
import fr.xephi.authme.process.SynchronousProcess;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.task.LimboPlayerTaskManager;
import fr.xephi.authme.util.BukkitService;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.inject.Inject;

import static fr.xephi.authme.util.BukkitService.TICKS_PER_SECOND;


public class ProcessSynchronousPlayerLogout implements SynchronousProcess {

    @Inject
    private AuthMe plugin;

    @Inject
    private ProcessService service;

    @Inject
    private BukkitService bukkitService;

    @Inject
    private ProtocolLibService protocolLibService;

    @Inject
    private LimboPlayerTaskManager limboPlayerTaskManager;

    @Inject
    private SessionManager sessionManager;

    ProcessSynchronousPlayerLogout() { }


    private void sendBungeeMessage(Player player) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        out.writeUTF("Forward");
        out.writeUTF("ALL");
        out.writeUTF("AuthMe");
        out.writeUTF("logout;" + player.getName());
        player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
    }

    public void processSyncLogout(Player player) {
        final String name = player.getName().toLowerCase();
        if (sessionManager.hasSession(name)) {
            sessionManager.cancelSession(name);
        }
        if (service.getProperty(RestrictionSettings.PROTECT_INVENTORY_BEFORE_LOGIN)) {
            protocolLibService.sendBlankInventoryPacket(player);
        }

        limboPlayerTaskManager.registerTimeoutTask(player);
        limboPlayerTaskManager.registerMessageTask(name, true);

        if (player.isInsideVehicle() && player.getVehicle() != null) {
            player.getVehicle().eject();
        }
        final int timeout = service.getProperty(RestrictionSettings.TIMEOUT) * TICKS_PER_SECOND;
        if (service.getProperty(RegistrationSettings.APPLY_BLIND_EFFECT)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, timeout, 2));
        }

        service.setGroup(player, AuthGroupType.NOT_LOGGED_IN);
        player.setOp(false);
        // Remove speed
        if (!service.getProperty(RestrictionSettings.ALLOW_UNAUTHED_MOVEMENT)
            && service.getProperty(RestrictionSettings.REMOVE_SPEED)) {
            player.setFlySpeed(0.0f);
            player.setWalkSpeed(0.0f);
        }

        // Player is now logout... Time to fire event !
        bukkitService.callEvent(new LogoutEvent(player));
        if (service.getProperty(HooksSettings.BUNGEECORD)) {
            sendBungeeMessage(player);
        }
        service.send(player, MessageKey.LOGOUT_SUCCESS);
        ConsoleLogger.info(player.getName() + " logged out");
    }

}
