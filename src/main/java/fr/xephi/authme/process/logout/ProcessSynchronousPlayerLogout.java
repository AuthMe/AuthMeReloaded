package fr.xephi.authme.process.logout;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.SessionManager;
import fr.xephi.authme.events.LogoutEvent;
import fr.xephi.authme.listener.protocollib.ProtocolLibService;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.permission.AuthGroupType;
import fr.xephi.authme.process.ProcessService;
import fr.xephi.authme.process.SynchronousProcess;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.task.PlayerDataTaskManager;
import fr.xephi.authme.util.BukkitService;
import fr.xephi.authme.util.TeleportationService;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.inject.Inject;

import static fr.xephi.authme.util.BukkitService.TICKS_PER_SECOND;


public class ProcessSynchronousPlayerLogout implements SynchronousProcess {

    @Inject
    private ProcessService service;

    @Inject
    private BukkitService bukkitService;

    @Inject
    private ProtocolLibService protocolLibService;

    @Inject
    private PlayerDataTaskManager playerDataTaskManager;

    @Inject
    private SessionManager sessionManager;

    @Inject
    private TeleportationService teleportationService;

    ProcessSynchronousPlayerLogout() {
    }

    public void processSyncLogout(Player player) {
        final String name = player.getName().toLowerCase();

        sessionManager.removeSession(name);
        if (service.getProperty(RestrictionSettings.PROTECT_INVENTORY_BEFORE_LOGIN)) {
            protocolLibService.sendBlankInventoryPacket(player);
        }

        playerDataTaskManager.registerTimeoutTask(player);
        playerDataTaskManager.registerMessageTask(name, true);

        applyLogoutEffect(player);

        // Player is now logout... Time to fire event !
        bukkitService.callEvent(new LogoutEvent(player));

        service.send(player, MessageKey.LOGOUT_SUCCESS);
        ConsoleLogger.info(player.getName() + " logged out");
    }

    private void applyLogoutEffect(Player player) {
        // dismount player
        player.leaveVehicle();
        teleportationService.teleportOnJoin(player);

        // Apply Blindness effect
        final int timeout = service.getProperty(RestrictionSettings.TIMEOUT) * TICKS_PER_SECOND;
        if (service.getProperty(RegistrationSettings.APPLY_BLIND_EFFECT)) {
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, timeout, 2));
        }

        // Set player's data to unauthenticated
        service.setGroup(player, AuthGroupType.NOT_LOGGED_IN);
        player.setOp(false);
        player.setAllowFlight(false);
        // Remove speed
        if (!service.getProperty(RestrictionSettings.ALLOW_UNAUTHED_MOVEMENT)
            && service.getProperty(RestrictionSettings.REMOVE_SPEED)) {
            player.setFlySpeed(0.0f);
            player.setWalkSpeed(0.0f);
        }
    }

}
