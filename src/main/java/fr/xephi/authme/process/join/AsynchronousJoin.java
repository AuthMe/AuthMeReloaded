package fr.xephi.authme.process.join;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.ProxySessionManager;
import fr.xephi.authme.data.limbo.LimboService;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.ProtectInventoryEvent;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.permission.PlayerStatePermission;
import fr.xephi.authme.process.AsynchronousProcess;
import fr.xephi.authme.process.login.AsynchronousLogin;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.PluginHookService;
import fr.xephi.authme.service.SessionService;
import fr.xephi.authme.service.ValidationService;
import fr.xephi.authme.service.bungeecord.BungeeSender;
import fr.xephi.authme.service.bungeecord.MessageType;
import fr.xephi.authme.settings.WelcomeMessageConfiguration;
import fr.xephi.authme.settings.commandconfig.CommandManager;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.util.InternetProtocolUtils;
import fr.xephi.authme.util.PlayerUtils;
import org.bukkit.GameMode;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.inject.Inject;

import java.util.Locale;

import static fr.xephi.authme.service.BukkitService.TICKS_PER_SECOND;
import static fr.xephi.authme.settings.properties.RestrictionSettings.PROTECT_INVENTORY_BEFORE_LOGIN;

/**
 * Asynchronous process for when a player joins.
 */
public class AsynchronousJoin implements AsynchronousProcess {
    
    private final ConsoleLogger logger = ConsoleLoggerFactory.get(AsynchronousJoin.class);

    @Inject
    private Server server;

    @Inject
    private DataSource database;

    @Inject
    private CommonService service;

    @Inject
    private LimboService limboService;

    @Inject
    private PluginHookService pluginHookService;

    @Inject
    private BukkitService bukkitService;

    @Inject
    private AsynchronousLogin asynchronousLogin;

    @Inject
    private CommandManager commandManager;

    @Inject
    private ValidationService validationService;

    @Inject
    private WelcomeMessageConfiguration welcomeMessageConfiguration;

    @Inject
    private SessionService sessionService;

    @Inject
    private BungeeSender bungeeSender;

    @Inject
    private ProxySessionManager proxySessionManager;

    AsynchronousJoin() {
    }

    /**
     * Processes the given player that has just joined.
     *
     * @param player the player to process
     */
    public void processJoin(Player player) {
        String name = player.getName().toLowerCase(Locale.ROOT);
        String ip = PlayerUtils.getPlayerIp(player);

        if (!validationService.fulfillsNameRestrictions(player)) {
            handlePlayerWithUnmetNameRestriction(player, ip);
            return;
        }

        if (service.getProperty(RestrictionSettings.UNRESTRICTED_NAMES).contains(name)) {
            return;
        }

        if (service.getProperty(RestrictionSettings.FORCE_SURVIVAL_MODE)
            && player.getGameMode() != GameMode.SURVIVAL
            && !service.hasPermission(player, PlayerStatePermission.BYPASS_FORCE_SURVIVAL)) {
            bukkitService.scheduleSyncTaskFromOptionallyAsyncTask(() -> player.setGameMode(GameMode.SURVIVAL));
        }

        if (service.getProperty(HooksSettings.DISABLE_SOCIAL_SPY)) {
            pluginHookService.setEssentialsSocialSpyStatus(player, false);
        }

        if (!validatePlayerCountForIp(player, ip)) {
            return;
        }

        boolean isAuthAvailable = database.isAuthAvailable(name);

        if (isAuthAvailable) {
            // Protect inventory
            if (service.getProperty(PROTECT_INVENTORY_BEFORE_LOGIN)) {
                ProtectInventoryEvent ev = bukkitService.createAndCallEvent(
                    isAsync -> new ProtectInventoryEvent(player, isAsync));
                if (ev.isCancelled()) {
                    player.updateInventory();
                    logger.fine("ProtectInventoryEvent has been cancelled for " + player.getName() + "...");
                }
            }

            // Session logic
            if (sessionService.canResumeSession(player)) {
                service.send(player, MessageKey.SESSION_RECONNECTION);
                // Run commands
                bukkitService.scheduleSyncTaskFromOptionallyAsyncTask(
                    () -> commandManager.runCommandsOnSessionLogin(player));
                bukkitService.runTaskOptionallyAsync(() -> asynchronousLogin.forceLogin(player));
                return;
            } else if (proxySessionManager.shouldResumeSession(name)) {
                service.send(player, MessageKey.SESSION_RECONNECTION);
                // Run commands
                bukkitService.scheduleSyncTaskFromOptionallyAsyncTask(
                    () -> commandManager.runCommandsOnSessionLogin(player));
                bukkitService.runTaskOptionallyAsync(() -> asynchronousLogin.forceLogin(player));
                logger.info("The user " + player.getName() + " has been automatically logged in, "
                    + "as present in autologin queue.");
                return;
            }
        } else if (!service.getProperty(RegistrationSettings.FORCE)) {
            bukkitService.scheduleSyncTaskFromOptionallyAsyncTask(() -> {
                welcomeMessageConfiguration.sendWelcomeMessage(player);
            });

            // Skip if registration is optional

            if (bungeeSender.isEnabled()) {
                // As described at https://www.spigotmc.org/wiki/bukkit-bungee-plugin-messaging-channel/
                // "Keep in mind that you can't send plugin messages directly after a player joins."
                bukkitService.scheduleSyncDelayedTask(() ->
                    bungeeSender.sendAuthMeBungeecordMessage(player, MessageType.LOGIN), 5L);
            }
            return;
        }

        processJoinSync(player, isAuthAvailable);
    }

    private void handlePlayerWithUnmetNameRestriction(Player player, String ip) {
        bukkitService.scheduleSyncTaskFromOptionallyAsyncTask(() -> {
            player.kickPlayer(service.retrieveSingleMessage(player, MessageKey.NOT_OWNER_ERROR));
            if (service.getProperty(RestrictionSettings.BAN_UNKNOWN_IP)) {
                server.banIP(ip);
            }
        });
    }

    /**
     * Performs various operations in sync mode for an unauthenticated player (such as blindness effect and
     * limbo player creation).
     *
     * @param player the player to process
     * @param isAuthAvailable true if the player is registered, false otherwise
     */
    private void processJoinSync(Player player, boolean isAuthAvailable) {
        int registrationTimeout = service.getProperty(RestrictionSettings.TIMEOUT) * TICKS_PER_SECOND;

        bukkitService.scheduleSyncTaskFromOptionallyAsyncTask(() -> {
            limboService.createLimboPlayer(player, isAuthAvailable);

            player.setNoDamageTicks(registrationTimeout);
            if (pluginHookService.isEssentialsAvailable() && service.getProperty(HooksSettings.USE_ESSENTIALS_MOTD)) {
                player.performCommand("motd");
            }
            if (service.getProperty(RegistrationSettings.APPLY_BLIND_EFFECT)) {
                // Allow infinite blindness effect
                int blindTimeOut = (registrationTimeout <= 0) ? 99999 : registrationTimeout;
                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blindTimeOut, 2));
            }
            commandManager.runCommandsOnJoin(player);
        });
    }

    /**
     * Checks whether the maximum number of accounts has been exceeded for the given IP address (according to
     * settings and permissions). If this is the case, the player is kicked.
     *
     * @param player the player to verify
     * @param ip     the ip address of the player
     *
     * @return true if the verification is OK (no infraction), false if player has been kicked
     */
    private boolean validatePlayerCountForIp(Player player, String ip) {
        if (service.getProperty(RestrictionSettings.MAX_JOIN_PER_IP) > 0
            && !service.hasPermission(player, PlayerStatePermission.ALLOW_MULTIPLE_ACCOUNTS)
            && !InternetProtocolUtils.isLoopbackAddress(ip)
            && countOnlinePlayersByIp(ip) > service.getProperty(RestrictionSettings.MAX_JOIN_PER_IP)) {

            bukkitService.scheduleSyncTaskFromOptionallyAsyncTask(
                () -> player.kickPlayer(service.retrieveSingleMessage(player, MessageKey.SAME_IP_ONLINE)));
            return false;
        }
        return true;
    }

    private int countOnlinePlayersByIp(String ip) {
        int count = 0;
        for (Player player : bukkitService.getOnlinePlayers()) {
            if (ip.equalsIgnoreCase(PlayerUtils.getPlayerIp(player))) {
                ++count;
            }
        }
        return count;
    }
}
