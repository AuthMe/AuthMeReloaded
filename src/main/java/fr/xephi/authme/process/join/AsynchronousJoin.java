package fr.xephi.authme.process.join;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.SessionManager;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.data.limbo.LimboService;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.ProtectInventoryEvent;
import fr.xephi.authme.events.RestoreSessionEvent;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.permission.PlayerStatePermission;
import fr.xephi.authme.process.AsynchronousProcess;
import fr.xephi.authme.process.login.AsynchronousLogin;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.PluginHookService;
import fr.xephi.authme.service.ValidationService;
import fr.xephi.authme.settings.commandconfig.CommandManager;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.util.PlayerUtils;
import org.bukkit.GameMode;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.inject.Inject;

import static fr.xephi.authme.service.BukkitService.TICKS_PER_SECOND;
import static fr.xephi.authme.settings.properties.RestrictionSettings.PROTECT_INVENTORY_BEFORE_LOGIN;

/**
 * Asynchronous process for when a player joins.
 */
public class AsynchronousJoin implements AsynchronousProcess {

    @Inject
    private Server server;

    @Inject
    private DataSource database;

    @Inject
    private CommonService service;

    @Inject
    private PlayerCache playerCache;

    @Inject
    private LimboService limboService;

    @Inject
    private SessionManager sessionManager;

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

    AsynchronousJoin() {
    }

    public void processJoin(final Player player) {
        final String name = player.getName().toLowerCase();
        final String ip = PlayerUtils.getPlayerIp(player);

        if (service.getProperty(RestrictionSettings.UNRESTRICTED_NAMES).contains(name)) {
            return;
        }

        if (service.getProperty(RestrictionSettings.FORCE_SURVIVAL_MODE)
            && !service.hasPermission(player, PlayerStatePermission.BYPASS_FORCE_SURVIVAL)) {
            bukkitService.runTask(() -> player.setGameMode(GameMode.SURVIVAL));
        }

        if (service.getProperty(HooksSettings.DISABLE_SOCIAL_SPY)) {
            pluginHookService.setEssentialsSocialSpyStatus(player, false);
        }

        if (!validationService.fulfillsNameRestrictions(player)) {
            bukkitService.scheduleSyncTaskFromOptionallyAsyncTask(new Runnable() {
                @Override
                public void run() {
                    player.kickPlayer(service.retrieveSingleMessage(MessageKey.NOT_OWNER_ERROR));
                    if (service.getProperty(RestrictionSettings.BAN_UNKNOWN_IP)) {
                        server.banIP(ip);
                    }
                }
            });
            return;
        }

        if (!validatePlayerCountForIp(player, ip)) {
            return;
        }

        final boolean isAuthAvailable = database.isAuthAvailable(name);

        if (isAuthAvailable) {
            // Protect inventory
            if (service.getProperty(PROTECT_INVENTORY_BEFORE_LOGIN)) {
                ProtectInventoryEvent ev = bukkitService.createAndCallEvent(
                    isAsync -> new ProtectInventoryEvent(player, isAsync));
                if (ev.isCancelled()) {
                    player.updateInventory();
                    ConsoleLogger.fine("ProtectInventoryEvent has been cancelled for " + player.getName() + "...");
                }
            }

            // Session logic
            if (canResumeSession(player)) {
                service.send(player, MessageKey.SESSION_RECONNECTION);
                // Run commands
                bukkitService.scheduleSyncTaskFromOptionallyAsyncTask(() -> commandManager.runCommandsOnSessionLogin(player));
                bukkitService.runTaskOptionallyAsync(() -> asynchronousLogin.forceLogin(player));
                return;
            }
        } else if (!service.getProperty(RegistrationSettings.FORCE)) {
            // Skip if registration is optional
            return;
        }

        final int registrationTimeout = service.getProperty(RestrictionSettings.TIMEOUT) * TICKS_PER_SECOND;

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

    private boolean canResumeSession(Player player) {
        final String name = player.getName();
        if (sessionManager.hasSession(name) || database.isLogged(name)) {
            PlayerAuth auth = database.getAuth(name);
            database.setUnlogged(name);
            playerCache.removePlayer(name);
            if (auth != null) {
                if (auth.getIp().equals(PlayerUtils.getPlayerIp(player))) {
                    RestoreSessionEvent event = bukkitService.createAndCallEvent(
                        isAsync -> new RestoreSessionEvent(player, isAsync));
                    return !event.isCancelled();
                } else {
                    service.send(player, MessageKey.SESSION_EXPIRED);
                }
            }
        }
        return false;
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
    private boolean validatePlayerCountForIp(final Player player, String ip) {
        if (service.getProperty(RestrictionSettings.MAX_JOIN_PER_IP) > 0
            && !service.hasPermission(player, PlayerStatePermission.ALLOW_MULTIPLE_ACCOUNTS)
            && !"127.0.0.1".equalsIgnoreCase(ip)
            && !"localhost".equalsIgnoreCase(ip)
            && countOnlinePlayersByIp(ip) > service.getProperty(RestrictionSettings.MAX_JOIN_PER_IP)) {

            bukkitService.scheduleSyncTaskFromOptionallyAsyncTask(
                () -> player.kickPlayer(service.retrieveSingleMessage(MessageKey.SAME_IP_ONLINE)));
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
