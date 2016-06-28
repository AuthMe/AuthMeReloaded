package fr.xephi.authme.process.join;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.SessionManager;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.ProtectInventoryEvent;
import fr.xephi.authme.hooks.PluginHooks;
import fr.xephi.authme.listener.protocollib.ProtocolLibService;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.permission.AuthGroupType;
import fr.xephi.authme.permission.PlayerStatePermission;
import fr.xephi.authme.process.AsynchronousProcess;
import fr.xephi.authme.process.ProcessService;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.task.LimboPlayerTaskManager;
import fr.xephi.authme.util.BukkitService;
import fr.xephi.authme.util.TeleportationService;
import fr.xephi.authme.util.Utils;
import org.apache.commons.lang.reflect.MethodUtils;
import org.bukkit.GameMode;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import javax.inject.Inject;

import static fr.xephi.authme.settings.properties.RestrictionSettings.PROTECT_INVENTORY_BEFORE_LOGIN;
import static fr.xephi.authme.util.BukkitService.TICKS_PER_SECOND;


public class AsynchronousJoin implements AsynchronousProcess {

    private static final boolean DISABLE_COLLISIONS = MethodUtils
        .getAccessibleMethod(LivingEntity.class, "setCollidable", new Class[]{}) != null;

    @Inject
    private AuthMe plugin;

    @Inject
    private DataSource database;

    @Inject
    private ProcessService service;

    @Inject
    private PlayerCache playerCache;

    @Inject
    private LimboCache limboCache;

    @Inject
    private SessionManager sessionManager;

    @Inject
    private PluginHooks pluginHooks;

    @Inject
    private TeleportationService teleportationService;

    @Inject
    private BukkitService bukkitService;

    @Inject
    private ProtocolLibService protocolLibService;

    @Inject
    private LimboPlayerTaskManager limboPlayerTaskManager;

    AsynchronousJoin() { }


    public void processJoin(final Player player) {
        final String name = player.getName().toLowerCase();
        final String ip = Utils.getPlayerIp(player);

        if (isPlayerUnrestricted(name)) {
            return;
        }

        // Prevent player collisions in 1.9
        if (DISABLE_COLLISIONS) {
            player.setCollidable(false);
        }

        if (service.getProperty(RestrictionSettings.FORCE_SURVIVAL_MODE)
            && !service.hasPermission(player, PlayerStatePermission.BYPASS_FORCE_SURVIVAL)) {
            bukkitService.runTask(new Runnable() {
                @Override
                public void run() {
                    player.setGameMode(GameMode.SURVIVAL);
                }
            });
        }

        if (service.getProperty(HooksSettings.DISABLE_SOCIAL_SPY)) {
            pluginHooks.setEssentialsSocialSpyStatus(player, false);
        }

        if (isNameRestricted(name, ip, player.getAddress().getHostName())) {
            bukkitService.scheduleSyncDelayedTask(new Runnable() {
                @Override
                public void run() {
                    player.kickPlayer(service.retrieveSingleMessage(MessageKey.NOT_OWNER_ERROR));
                    if (service.getProperty(RestrictionSettings.BAN_UNKNOWN_IP)) {
                        plugin.getServer().banIP(ip);
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
            service.setGroup(player, AuthGroupType.NOT_LOGGED_IN);
            teleportationService.teleportOnJoin(player);
            limboCache.updateLimboPlayer(player);

            // Protect inventory
            if (service.getProperty(PROTECT_INVENTORY_BEFORE_LOGIN)) {
                ProtectInventoryEvent ev = new ProtectInventoryEvent(player);
                bukkitService.callEvent(ev);
                if (ev.isCancelled()) {
                    player.updateInventory();
                    if (!service.getProperty(SecuritySettings.REMOVE_SPAM_FROM_CONSOLE)) {
                        ConsoleLogger.info("ProtectInventoryEvent has been cancelled for " + player.getName() + "...");
                    }
                }
            }

            // Session logic
            if (service.getProperty(PluginSettings.SESSIONS_ENABLED) && (playerCache.isAuthenticated(name) || database.isLogged(name))) {
                if (sessionManager.hasSession(name)) {
                    sessionManager.cancelSession(name);
                }
                PlayerAuth auth = database.getAuth(name);
                database.setUnlogged(name);
                playerCache.removePlayer(name);
                if (auth != null && auth.getIp().equals(ip)) {
                    service.send(player, MessageKey.SESSION_RECONNECTION);
                    plugin.getManagement().performLogin(player, "dontneed", true);
                    return;
                } else if (service.getProperty(PluginSettings.SESSIONS_EXPIRE_ON_IP_CHANGE)) {
                    service.send(player, MessageKey.SESSION_EXPIRED);
                }
            }
        } else {
            // Not Registered

            // Groups logic
            service.setGroup(player, AuthGroupType.UNREGISTERED);

            // Skip if registration is optional
            if (!service.getProperty(RegistrationSettings.FORCE)) {
                return;
            }

            teleportationService.teleportOnJoin(player);
        }
        // The user is not logged in

        if (!limboCache.hasLimboPlayer(name)) {
            limboCache.addLimboPlayer(player);
        }

        final int registrationTimeout = service.getProperty(RestrictionSettings.TIMEOUT) * TICKS_PER_SECOND;

        bukkitService.scheduleSyncDelayedTask(new Runnable() {
            @Override
            public void run() {
                player.setOp(false);
                if (!service.getProperty(RestrictionSettings.ALLOW_UNAUTHED_MOVEMENT)
                    && service.getProperty(RestrictionSettings.REMOVE_SPEED)) {
                    player.setFlySpeed(0.0f);
                    player.setWalkSpeed(0.0f);
                }
                player.setNoDamageTicks(registrationTimeout);
                if (pluginHooks.isEssentialsAvailable() && service.getProperty(HooksSettings.USE_ESSENTIALS_MOTD)) {
                    player.performCommand("motd");
                }
                if (service.getProperty(RegistrationSettings.APPLY_BLIND_EFFECT)) {
                    // Allow infinite blindness effect
                    int blindTimeOut = (registrationTimeout <= 0) ? 99999 : registrationTimeout;
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, blindTimeOut, 2));
                }
            }

        });

        // Timeout and message task
        limboPlayerTaskManager.registerTimeoutTask(player);
        limboPlayerTaskManager.registerMessageTask(name, isAuthAvailable);
    }

    private boolean isPlayerUnrestricted(String name) {
        return service.getProperty(RestrictionSettings.UNRESTRICTED_NAMES).contains(name);
    }

    /**
     * Returns whether the name is restricted based on the restriction settings.
     *
     * @param name The name to check
     * @param ip The IP address of the player
     * @param domain The hostname of the IP address
     * @return True if the name is restricted (IP/domain is not allowed for the given name),
     *         false if the restrictions are met or if the name has no restrictions to it
     */
    private boolean isNameRestricted(String name, String ip, String domain) {
        if (!service.getProperty(RestrictionSettings.ENABLE_RESTRICTED_USERS)) {
            return false;
        }

        boolean nameFound = false;
        for (String entry : service.getProperty(RestrictionSettings.ALLOWED_RESTRICTED_USERS)) {
            String[] args = entry.split(";");
            String testName = args[0];
            String testIp = args[1];
            if (testName.equalsIgnoreCase(name)) {
                nameFound = true;
                if ((ip != null && testIp.equals(ip)) || (domain != null && testIp.equalsIgnoreCase(domain))) {
                    return false;
                }
            }
        }
        return nameFound;
    }

    /**
     * Checks whether the maximum number of accounts has been exceeded for the given IP address (according to
     * settings and permissions). If this is the case, the player is kicked.
     *
     * @param player the player to verify
     * @param ip the ip address of the player
     * @return true if the verification is OK (no infraction), false if player has been kicked
     */
    private boolean validatePlayerCountForIp(final Player player, String ip) {
        if (service.getProperty(RestrictionSettings.MAX_JOIN_PER_IP) > 0
            && !service.hasPermission(player, PlayerStatePermission.ALLOW_MULTIPLE_ACCOUNTS)
            && !"127.0.0.1".equalsIgnoreCase(ip)
            && !"localhost".equalsIgnoreCase(ip)
            && countOnlinePlayersByIp(ip) > service.getProperty(RestrictionSettings.MAX_JOIN_PER_IP)) {

            bukkitService.scheduleSyncDelayedTask(new Runnable() {
                @Override
                public void run() {
                    player.kickPlayer(service.retrieveSingleMessage(MessageKey.SAME_IP_ONLINE));
                }
            });
            return false;
        }
        return true;
    }

    private int countOnlinePlayersByIp(String ip) {
        int count = 0;
        for (Player player : bukkitService.getOnlinePlayers()) {
            if (ip.equalsIgnoreCase(Utils.getPlayerIp(player))) {
                ++count;
            }
        }
        return count;
    }
}
