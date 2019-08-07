package fr.xephi.authme.process.logout;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.limbo.LimboService;
import fr.xephi.authme.events.LogoutEvent;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.listener.protocollib.ProtocolLibService;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.process.SynchronousProcess;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.TeleportationService;
import fr.xephi.authme.settings.commandconfig.CommandManager;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.inject.Inject;

import static fr.xephi.authme.service.BukkitService.TICKS_PER_SECOND;


public class ProcessSyncPlayerLogout implements SynchronousProcess {

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(ProcessSyncPlayerLogout.class);

    @Inject
    private CommonService service;

    @Inject
    private BukkitService bukkitService;

    @Inject
    private ProtocolLibService protocolLibService;

    @Inject
    private LimboService limboService;

    @Inject
    private TeleportationService teleportationService;

    @Inject
    private CommandManager commandManager;

    ProcessSyncPlayerLogout() {
    }

    /**
     * Processes a player which has been logged out.
     *
     * @param player the player logging out
     */
    public void processSyncLogout(Player player) {
        if (service.getProperty(RestrictionSettings.PROTECT_INVENTORY_BEFORE_LOGIN)) {
            protocolLibService.sendBlankInventoryPacket(player);
        }

        applyLogoutEffect(player);
        commandManager.runCommandsOnLogout(player);

        // Player is now logout... Time to fire event !
        bukkitService.callEvent(new LogoutEvent(player));

        service.send(player, MessageKey.LOGOUT_SUCCESS);
        logger.info(player.getName() + " logged out");
    }

    private void applyLogoutEffect(Player player) {
        // dismount player
        player.leaveVehicle();
        teleportationService.teleportOnJoin(player);

        // Apply Blindness effect
        if (service.getProperty(RegistrationSettings.APPLY_BLIND_EFFECT)) {
            int timeout = service.getProperty(RestrictionSettings.TIMEOUT) * TICKS_PER_SECOND;
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, timeout, 2));
        }

        // Set player's data to unauthenticated
        limboService.createLimboPlayer(player, true);
    }

}
