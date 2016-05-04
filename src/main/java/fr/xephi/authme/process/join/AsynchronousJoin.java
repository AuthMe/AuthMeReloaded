package fr.xephi.authme.process.join;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.FirstSpawnTeleportEvent;
import fr.xephi.authme.events.ProtectInventoryEvent;
import fr.xephi.authme.events.SpawnTeleportEvent;
import fr.xephi.authme.hooks.PluginHooks;
import fr.xephi.authme.listener.AuthMePlayerListener;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.permission.PlayerStatePermission;
import fr.xephi.authme.process.Process;
import fr.xephi.authme.process.ProcessService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.task.MessageTask;
import fr.xephi.authme.task.TimeoutTask;
import fr.xephi.authme.util.Utils;
import fr.xephi.authme.util.Utils.GroupType;
import org.apache.commons.lang.reflect.MethodUtils;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitTask;

import static fr.xephi.authme.settings.properties.RestrictionSettings.PROTECT_INVENTORY_BEFORE_LOGIN;

/**
 */
public class AsynchronousJoin implements Process {

    private final AuthMe plugin;
    private final Player player;
    private final DataSource database;
    private final String name;
    private final ProcessService service;
    private final PlayerCache playerCache;
    private final PluginHooks pluginHooks;

    private final boolean disableCollisions = MethodUtils
            .getAccessibleMethod(LivingEntity.class, "setCollidable", new Class[]{}) != null;

    public AsynchronousJoin(Player player, AuthMe plugin, DataSource database, PlayerCache playerCache,
                            PluginHooks pluginHooks, ProcessService service) {
        this.player = player;
        this.plugin = plugin;
        this.database = database;
        this.name = player.getName().toLowerCase();
        this.service = service;
        this.playerCache = playerCache;
        this.pluginHooks = pluginHooks;
    }

    @Override
    public void run() {
        if (Utils.isUnrestricted(player)) {
            return;
        }

        if (service.getProperty(HooksSettings.DISABLE_SOCIAL_SPY)) {
            service.getPluginHooks().setEssentialsSocialSpyStatus(player, false);
        }

        final String ip = Utils.getPlayerIp(player);
        if (isNameRestricted(name, ip, player.getAddress().getHostName())) {
            service.scheduleSyncDelayedTask(new Runnable() {
                @Override
                public void run() {
                    AuthMePlayerListener.causeByAuthMe.putIfAbsent(name, true);
                    player.kickPlayer(service.retrieveSingleMessage(MessageKey.NOT_OWNER_ERROR));
                    if (service.getProperty(RestrictionSettings.BAN_UNKNOWN_IP)) {
                        plugin.getServer().banIP(ip);
                    }
                }
            });
            return;
        }
        if (service.getProperty(RestrictionSettings.MAX_JOIN_PER_IP) > 0
            && !plugin.getPermissionsManager().hasPermission(player, PlayerStatePermission.ALLOW_MULTIPLE_ACCOUNTS)
            && !"127.0.0.1".equalsIgnoreCase(ip)
            && !"localhost".equalsIgnoreCase(ip)
            && hasJoinedIp(player.getName(), ip)) {
            service.scheduleSyncDelayedTask(new Runnable() {
                @Override
                public void run() {
                    player.kickPlayer("A player with the same IP is already in game!");
                }
            });
            return;
        }
        // Prevent player collisions in 1.9
        if (disableCollisions) {
            ((LivingEntity) player).setCollidable(false);
        }
        final Location spawnLoc = service.getSpawnLoader().getSpawnLocation(player);
        final boolean isAuthAvailable = database.isAuthAvailable(name);
        if (isAuthAvailable) {
            if (!service.getProperty(RestrictionSettings.NO_TELEPORT)) {
                if (Settings.isTeleportToSpawnEnabled || (Settings.isForceSpawnLocOnJoinEnabled && Settings.getForcedWorlds.contains(player.getWorld().getName()))) {
                    service.scheduleSyncDelayedTask(new Runnable() {
                        @Override
                        public void run() {
                            SpawnTeleportEvent tpEvent = new SpawnTeleportEvent(player, player.getLocation(), spawnLoc, playerCache.isAuthenticated(name));
                            service.callEvent(tpEvent);
                            if (!tpEvent.isCancelled() && player.isOnline() && tpEvent.getTo() != null
                                && tpEvent.getTo().getWorld() != null) {
                                player.teleport(tpEvent.getTo());
                            }
                        }
                    });
                }
            }
            placePlayerSafely(player, spawnLoc);
            LimboCache.getInstance().updateLimboPlayer(player);

            // protect inventory
            if (service.getProperty(PROTECT_INVENTORY_BEFORE_LOGIN) && plugin.inventoryProtector != null) {
                ProtectInventoryEvent ev = new ProtectInventoryEvent(player);
                plugin.getServer().getPluginManager().callEvent(ev);
                if (ev.isCancelled()) {
                    plugin.inventoryProtector.sendInventoryPacket(player);
                    if (!service.getProperty(SecuritySettings.REMOVE_SPAM_FROM_CONSOLE)) {
                        ConsoleLogger.info("ProtectInventoryEvent has been cancelled for " + player.getName() + "...");
                    }
                }
            }

            if (service.getProperty(PluginSettings.SESSIONS_ENABLED) && (playerCache.isAuthenticated(name) || database.isLogged(name))) {
                if (plugin.sessions.containsKey(name)) {
                    plugin.sessions.get(name).cancel();
                    plugin.sessions.remove(name);
                }
                PlayerAuth auth = database.getAuth(name);
                database.setUnlogged(name);
                PlayerCache.getInstance().removePlayer(name);
                if (auth != null && auth.getIp().equals(ip)) {
                    service.send(player, MessageKey.SESSION_RECONNECTION);
                    plugin.getManagement().performLogin(player, "dontneed", true);
                    return;
                } else if (service.getProperty(PluginSettings.SESSIONS_EXPIRE_ON_IP_CHANGE)) {
                    service.send(player, MessageKey.SESSION_EXPIRED);
                }
            }
        } else {
            if (!Settings.unRegisteredGroup.isEmpty()) {
                Utils.setGroup(player, Utils.GroupType.UNREGISTERED);
            }
            if (!service.getProperty(RegistrationSettings.FORCE)) {
                return;
            }

            if (!Settings.noTeleport && !needFirstSpawn() && Settings.isTeleportToSpawnEnabled
                || (Settings.isForceSpawnLocOnJoinEnabled && Settings.getForcedWorlds.contains(player.getWorld().getName()))) {
                service.scheduleSyncDelayedTask(new Runnable() {
                    @Override
                    public void run() {
                        SpawnTeleportEvent tpEvent = new SpawnTeleportEvent(player, player.getLocation(), spawnLoc, PlayerCache.getInstance().isAuthenticated(name));
                        service.callEvent(tpEvent);
                        if (!tpEvent.isCancelled() && player.isOnline() && tpEvent.getTo() != null
                            && tpEvent.getTo().getWorld() != null) {
                            player.teleport(tpEvent.getTo());
                        }
                    }
                });
            }
        }

        if (!LimboCache.getInstance().hasLimboPlayer(name)) {
            LimboCache.getInstance().addLimboPlayer(player);
        }
        Utils.setGroup(player, isAuthAvailable ? GroupType.NOTLOGGEDIN : GroupType.UNREGISTERED);

        final int registrationTimeout = service.getProperty(RestrictionSettings.TIMEOUT) * 20;

        service.scheduleSyncDelayedTask(new Runnable() {
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

        int msgInterval = service.getProperty(RegistrationSettings.MESSAGE_INTERVAL);
        if (registrationTimeout > 0) {
            BukkitTask id = service.runTaskLater(new TimeoutTask(plugin, name, player), registrationTimeout);
            LimboCache.getInstance().getLimboPlayer(name).setTimeoutTask(id);
        }

        MessageKey msg;
        if (isAuthAvailable) {
            msg = MessageKey.LOGIN_MESSAGE;
        } else {
            msg = service.getProperty(RegistrationSettings.USE_EMAIL_REGISTRATION)
                ? MessageKey.REGISTER_EMAIL_MESSAGE
                : MessageKey.REGISTER_MESSAGE;
        }
        if (msgInterval > 0 && LimboCache.getInstance().getLimboPlayer(name) != null) {
            BukkitTask msgTask = service.runTask(new MessageTask(service.getBukkitService(), plugin.getMessages(),
                name, msg, msgInterval));
            LimboCache.getInstance().getLimboPlayer(name).setMessageTask(msgTask);
        }
    }

    private boolean needFirstSpawn() {
        if (player.hasPlayedBefore()) {
            return false;
        }
        Location firstSpawn = service.getSpawnLoader().getFirstSpawn();
        if (firstSpawn == null) {
            return false;
        }

        FirstSpawnTeleportEvent tpEvent = new FirstSpawnTeleportEvent(player, player.getLocation(), firstSpawn);
        plugin.getServer().getPluginManager().callEvent(tpEvent);
        if (!tpEvent.isCancelled()) {
            if (player.isOnline() && tpEvent.getTo() != null && tpEvent.getTo().getWorld() != null) {
                final Location fLoc = tpEvent.getTo();
                service.scheduleSyncDelayedTask(new Runnable() {
                    @Override
                    public void run() {
                        player.teleport(fLoc);
                    }
                });
            }
        }
        return true;
    }

    private void placePlayerSafely(final Player player, final Location spawnLoc) {
        if (spawnLoc == null || service.getProperty(RestrictionSettings.NO_TELEPORT))
            return;
        if (Settings.isTeleportToSpawnEnabled || (Settings.isForceSpawnLocOnJoinEnabled && Settings.getForcedWorlds.contains(player.getWorld().getName())))
            return;
        if (!player.hasPlayedBefore())
            return;
        service.scheduleSyncDelayedTask(new Runnable() {
            @Override
            public void run() {
                if (spawnLoc.getWorld() == null) {
                    return;
                }
                Material cur = player.getLocation().getBlock().getType();
                Material top = player.getLocation().add(0, 1, 0).getBlock().getType();
                if (cur == Material.PORTAL || cur == Material.ENDER_PORTAL
                    || top == Material.PORTAL || top == Material.ENDER_PORTAL) {
                    service.send(player, MessageKey.UNSAFE_QUIT_LOCATION);
                    player.teleport(spawnLoc);
                }
            }

        });
    }

    /**
     * Return whether the name is restricted based on the restriction setting.
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

    private boolean hasJoinedIp(String name, String ip) {
        int count = 0;
        for (Player player : service.getOnlinePlayers()) {
            if (ip.equalsIgnoreCase(Utils.getPlayerIp(player))
                && !player.getName().equalsIgnoreCase(name)) {
                count++;
            }
        }
        return count >= service.getProperty(RestrictionSettings.MAX_JOIN_PER_IP);
    }
}
