package fr.xephi.authme.process.unregister;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.permission.AuthGroupHandler;
import fr.xephi.authme.permission.AuthGroupType;
import fr.xephi.authme.process.AsynchronousProcess;
import fr.xephi.authme.process.ProcessService;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.task.PlayerDataTaskManager;
import fr.xephi.authme.util.BukkitService;
import fr.xephi.authme.util.TeleportationService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.inject.Inject;

import static fr.xephi.authme.util.BukkitService.TICKS_PER_SECOND;

public class AsynchronousUnregister implements AsynchronousProcess {

    @Inject
    private DataSource dataSource;

    @Inject
    private ProcessService service;

    @Inject
    private PasswordSecurity passwordSecurity;

    @Inject
    private PlayerCache playerCache;

    @Inject
    private BukkitService bukkitService;

    @Inject
    private LimboCache limboCache;

    @Inject
    private PlayerDataTaskManager playerDataTaskManager;

    @Inject
    private TeleportationService teleportationService;

    @Inject
    private AuthGroupHandler authGroupHandler;

    AsynchronousUnregister() { }

    /**
     * Processes a player's request to unregister himself. Unregisters the player after
     * successful password check.
     *
     * @param player the player
     * @param password the input password to check before unregister
     */
    public void unregister(Player player, String password) {
        final String name = player.getName();
        final PlayerAuth cachedAuth = playerCache.getAuth(name);
        if (passwordSecurity.comparePassword(password, cachedAuth.getPassword(), name)) {
            if (dataSource.removeAuth(name)) {
                performUnregister(name, player);
                ConsoleLogger.info(name + " unregistered himself");
            } else {
                service.send(player, MessageKey.ERROR);
            }
        } else {
            service.send(player, MessageKey.WRONG_PASSWORD);
        }
    }

    /**
     * Unregisters a player.
     *
     * @param initiator the initiator of this process (nullable)
     * @param name the name of the player
     * @param player the according Player object (nullable)
     */
    // We need to have the name and the player separate because Player might be null in this case:
    // we might have some player in the database that has never been online on the server
    public void adminUnregister(CommandSender initiator, String name, Player player) {
        if (dataSource.removeAuth(name)) {
            performUnregister(name, player);
            if (initiator == null) {
                ConsoleLogger.info(name + " was unregistered");
            } else {
                ConsoleLogger.info(name + " was unregistered by " + initiator.getName());
                service.send(initiator, MessageKey.UNREGISTERED_SUCCESS);
            }
        } else if (initiator != null) {
            service.send(initiator, MessageKey.ERROR);
        }
    }

    private void performUnregister(String name, Player player) {
        playerCache.removePlayer(name);
        if (player == null || !player.isOnline()) {
            return;
        }

        if (service.getProperty(RegistrationSettings.FORCE)) {
            teleportationService.teleportOnJoin(player);
            player.saveData();

            limboCache.deletePlayerData(player);
            limboCache.addPlayerData(player);

            playerDataTaskManager.registerTimeoutTask(player);
            playerDataTaskManager.registerMessageTask(name, false);
            applyBlindEffect(player);
        }
        authGroupHandler.setGroup(player, AuthGroupType.UNREGISTERED);
        service.send(player, MessageKey.UNREGISTERED_SUCCESS);
    }

    private void applyBlindEffect(final Player player) {
        if (service.getProperty(RegistrationSettings.APPLY_BLIND_EFFECT)) {
            final int timeout = service.getProperty(RestrictionSettings.TIMEOUT) * TICKS_PER_SECOND;
            bukkitService.runTask(new Runnable() {
                @Override
                public void run() {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, timeout, 2));
                }
            });
        }
    }
}
