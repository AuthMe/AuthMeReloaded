package fr.xephi.authme.process.unregister;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.data.limbo.LimboService;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.UnregisterByAdminEvent;
import fr.xephi.authme.events.UnregisterByPlayerEvent;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.process.AsynchronousProcess;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.TeleportationService;
import fr.xephi.authme.service.bungeecord.BungeeSender;
import fr.xephi.authme.service.bungeecord.MessageType;
import fr.xephi.authme.settings.commandconfig.CommandManager;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.inject.Inject;

import static fr.xephi.authme.service.BukkitService.TICKS_PER_SECOND;

public class AsynchronousUnregister implements AsynchronousProcess {

    @Inject
    private DataSource dataSource;

    @Inject
    private CommonService service;

    @Inject
    private PasswordSecurity passwordSecurity;

    @Inject
    private PlayerCache playerCache;

    @Inject
    private BukkitService bukkitService;

    @Inject
    private LimboService limboService;

    @Inject
    private TeleportationService teleportationService;

    @Inject
    private CommandManager commandManager;

    @Inject
    private BungeeSender bungeeSender;

    AsynchronousUnregister() {
    }

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
                performPostUnregisterActions(name, player);
                ConsoleLogger.info(name + " unregistered himself");
                bukkitService.createAndCallEvent(isAsync -> new UnregisterByPlayerEvent(player, isAsync));
            } else {
                service.send(player, MessageKey.ERROR);
            }
        } else {
            service.send(player, MessageKey.WRONG_PASSWORD);
        }
    }

    /**
     * Unregisters a player as administrator or console.
     *
     * @param initiator the initiator of this process (nullable)
     * @param name the name of the player
     * @param player the according Player object (nullable)
     */
    // We need to have the name and the player separate because Player might be null in this case:
    // we might have some player in the database that has never been online on the server
    public void adminUnregister(CommandSender initiator, String name, Player player) {
        if (dataSource.removeAuth(name)) {
            performPostUnregisterActions(name, player);
            bukkitService.createAndCallEvent(isAsync -> new UnregisterByAdminEvent(player, name, isAsync, initiator));

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

    /**
     * Process the post unregister actions. Makes the user status consistent.
     *
     * @param name the name of the player
     * @param player the according Player object (nullable)
     */
    private void performPostUnregisterActions(String name, Player player) {
        playerCache.removePlayer(name);
        bungeeSender.sendAuthMeBungeecordMessage(MessageType.UNREGISTER, name);

        if (player == null || !player.isOnline()) {
            return;
        }
        bukkitService.scheduleSyncTaskFromOptionallyAsyncTask(() ->
            commandManager.runCommandsOnUnregister(player));

        if (service.getProperty(RegistrationSettings.FORCE)) {
            teleportationService.teleportOnJoin(player);
            player.saveData();

            bukkitService.scheduleSyncTaskFromOptionallyAsyncTask(() -> {
                limboService.createLimboPlayer(player, false);
                applyBlindEffect(player);
            });
        }
        service.send(player, MessageKey.UNREGISTERED_SUCCESS);
    }

    private void applyBlindEffect(final Player player) {
        if (service.getProperty(RegistrationSettings.APPLY_BLIND_EFFECT)) {
            int timeout = service.getProperty(RestrictionSettings.TIMEOUT) * TICKS_PER_SECOND;
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, timeout, 2));
        }
    }

}
