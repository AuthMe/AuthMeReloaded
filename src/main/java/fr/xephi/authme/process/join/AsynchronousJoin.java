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
import fr.xephi.authme.listener.AuthMePlayerListener;
import fr.xephi.authme.settings.MessageKey;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.Spawn;
import fr.xephi.authme.task.MessageTask;
import fr.xephi.authme.task.TimeoutTask;
import fr.xephi.authme.util.Utils;
import fr.xephi.authme.util.Utils.GroupType;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

/**
 */
public class AsynchronousJoin {

    private final AuthMe plugin;
    private final Player player;
    private final DataSource database;
    private final String name;
    private final Messages m;
    private final BukkitScheduler sched;

    public AsynchronousJoin(Player player, AuthMe plugin, DataSource database) {
        this.player = player;
        this.plugin = plugin;
        this.sched = plugin.getServer().getScheduler();
        this.database = database;
        this.name = player.getName().toLowerCase();
        this.m = Messages.getInstance();
    }

    public void process() {
        if (AuthMePlayerListener.gameMode.containsKey(name))
            AuthMePlayerListener.gameMode.remove(name);
        AuthMePlayerListener.gameMode.putIfAbsent(name, player.getGameMode());

        if (Utils.isNPC(player) || Utils.isUnrestricted(player)) {
            return;
        }

        if (plugin.ess != null && Settings.disableSocialSpy) {
            plugin.ess.getUser(player).setSocialSpyEnabled(false);
        }

        if (!plugin.canConnect()) {
            final GameMode gM = AuthMePlayerListener.gameMode.get(name);
            sched.scheduleSyncDelayedTask(plugin, new Runnable() {

                @Override
                public void run() {
                    AuthMePlayerListener.causeByAuthMe.putIfAbsent(name, true);
                    player.setGameMode(gM);
                    player.kickPlayer("Server is loading, please wait before joining!");
                }

            });
            return;
        }

        final String ip = plugin.getIP(player);
        if (Settings.isAllowRestrictedIp && !Settings.getRestrictedIp(name, ip)) {
            final GameMode gM = AuthMePlayerListener.gameMode.get(name);
            sched.scheduleSyncDelayedTask(plugin, new Runnable() {

                @Override
                public void run() {
                    AuthMePlayerListener.causeByAuthMe.putIfAbsent(name, true);
                    player.setGameMode(gM);
                    player.kickPlayer("You are not the Owner of this account, please try another name!");
                    if (Settings.banUnsafeIp)
                        plugin.getServer().banIP(ip);
                }

            });
            return;
        }
        if (Settings.getMaxJoinPerIp > 0 && !plugin.getPermissionsManager().hasPermission(player, "authme.allow2accounts") && !ip.equalsIgnoreCase("127.0.0.1") && !ip.equalsIgnoreCase("localhost")) {
            if (plugin.hasJoinedIp(player.getName(), ip)) {
                sched.scheduleSyncDelayedTask(plugin, new Runnable() {

                    @Override
                    public void run() {
                        player.kickPlayer("A player with the same IP is already in game!");
                    }

                });
                return;
            }
        }
        final Location spawnLoc = plugin.getSpawnLocation(player);
        final boolean isAuthAvailable = database.isAuthAvailable(name);
        if (isAuthAvailable) {
            if (Settings.isForceSurvivalModeEnabled && !Settings.forceOnlyAfterLogin) {
                sched.scheduleSyncDelayedTask(plugin, new Runnable() {

                    @Override
                    public void run() {
                        AuthMePlayerListener.causeByAuthMe.putIfAbsent(name, true);
                        Utils.forceGM(player);
                    }

                });
            }
            if (!Settings.noTeleport)
                if (Settings.isTeleportToSpawnEnabled || (Settings.isForceSpawnLocOnJoinEnabled && Settings.getForcedWorlds.contains(player.getWorld().getName()))) {
                    sched.scheduleSyncDelayedTask(plugin, new Runnable() {

                        @Override
                        public void run() {
                            SpawnTeleportEvent tpEvent = new SpawnTeleportEvent(player, player.getLocation(), spawnLoc, PlayerCache.getInstance().isAuthenticated(name));
                            plugin.getServer().getPluginManager().callEvent(tpEvent);
                            if (!tpEvent.isCancelled()) {
                                if (player.isOnline() && tpEvent.getTo() != null) {
                                    if (tpEvent.getTo().getWorld() != null)
                                        player.teleport(tpEvent.getTo());
                                }
                            }
                        }

                    });
                }
            placePlayerSafely(player, spawnLoc);
            LimboCache.getInstance().updateLimboPlayer(player);
            // protect inventory
            if (Settings.protectInventoryBeforeLogInEnabled && plugin.inventoryProtector != null) {
                ProtectInventoryEvent ev = new ProtectInventoryEvent(player);
                plugin.getServer().getPluginManager().callEvent(ev);
                if (ev.isCancelled()) {
                    plugin.inventoryProtector.sendInventoryPacket(player);
                    if (!Settings.noConsoleSpam)
                        ConsoleLogger.info("ProtectInventoryEvent has been cancelled for " + player.getName() + " ...");
                }
            }
        } else {
            if (Settings.isForceSurvivalModeEnabled && !Settings.forceOnlyAfterLogin) {
                sched.scheduleSyncDelayedTask(plugin, new Runnable() {

                    @Override
                    public void run() {
                        AuthMePlayerListener.causeByAuthMe.putIfAbsent(name, true);
                        Utils.forceGM(player);
                    }

                });
            }
            if (!Settings.unRegisteredGroup.isEmpty()) {
                Utils.setGroup(player, Utils.GroupType.UNREGISTERED);
            }
            if (!Settings.isForcedRegistrationEnabled) {
                return;
            }
            if (!Settings.noTeleport)
                if (!needFirstSpawn() && Settings.isTeleportToSpawnEnabled || (Settings.isForceSpawnLocOnJoinEnabled && Settings.getForcedWorlds.contains(player.getWorld().getName()))) {
                    sched.scheduleSyncDelayedTask(plugin, new Runnable() {

                        @Override
                        public void run() {
                            SpawnTeleportEvent tpEvent = new SpawnTeleportEvent(player, player.getLocation(), spawnLoc, PlayerCache.getInstance().isAuthenticated(name));
                            plugin.getServer().getPluginManager().callEvent(tpEvent);
                            if (!tpEvent.isCancelled()) {
                                if (player.isOnline() && tpEvent.getTo() != null) {
                                    if (tpEvent.getTo().getWorld() != null)
                                        player.teleport(tpEvent.getTo());
                                }
                            }
                        }

                    });
                }

        }

        if (!LimboCache.getInstance().hasLimboPlayer(name)) {
            LimboCache.getInstance().addLimboPlayer(player);
        }

        final int timeOut = Settings.getRegistrationTimeout * 20;
        int msgInterval = Settings.getWarnMessageInterval;
        if (timeOut > 0) {
            BukkitTask id = sched.runTaskLaterAsynchronously(plugin, new TimeoutTask(plugin, name, player), timeOut);
            LimboCache.getInstance().getLimboPlayer(name).setTimeoutTaskId(id);
        }

        Utils.setGroup(player, isAuthAvailable ? GroupType.NOTLOGGEDIN : GroupType.UNREGISTERED);
        sched.scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                player.setOp(false);
                if (!Settings.isMovementAllowed) {
                    player.setAllowFlight(true);
                    player.setFlying(true);
                }
                player.setNoDamageTicks(timeOut);
                if (Settings.useEssentialsMotd) {
                    player.performCommand("motd");
                }
                if (Settings.applyBlindEffect) {
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, timeOut, 2));
                }
                if (!Settings.isMovementAllowed && Settings.isRemoveSpeedEnabled) {
                    player.setWalkSpeed(0.0f);
                    player.setFlySpeed(0.0f);
                }
            }

        });

        if (Settings.isSessionsEnabled && isAuthAvailable && (PlayerCache.getInstance().isAuthenticated(name) || database.isLogged(name))) {
            if (plugin.sessions.containsKey(name)) {
                plugin.sessions.get(name).cancel();
                plugin.sessions.remove(name);
            }
            PlayerAuth auth = database.getAuth(name);
            database.setUnlogged(name);
            PlayerCache.getInstance().removePlayer(name);
            if (auth != null && auth.getIp().equals(ip)) {
                m.send(player, MessageKey.SESSION_RECONNECTION);
                plugin.management.performLogin(player, "dontneed", true);
                return;
            } else if (Settings.sessionExpireOnIpChange) {
                m.send(player, MessageKey.SESSION_EXPIRED);
            }
        }

        String[] msg;
        if (isAuthAvailable) {
            msg = m.retrieve(MessageKey.LOGIN_MESSAGE);
        } else {
            msg = Settings.emailRegistration
                ? m.retrieve(MessageKey.REGISTER_EMAIL_MESSAGE)
                : m.retrieve(MessageKey.REGISTER_MESSAGE);
        }
        BukkitTask msgTask = sched.runTaskAsynchronously(plugin, new MessageTask(plugin, name, msg, msgInterval));
        LimboCache.getInstance().getLimboPlayer(name).setMessageTaskId(msgTask);
    }

    private boolean needFirstSpawn() {
        if (player.hasPlayedBefore())
            return false;
        Location firstSpawn = Spawn.getInstance().getFirstSpawn();
        if (firstSpawn == null || firstSpawn.getWorld() == null)
            return false;
        FirstSpawnTeleportEvent tpEvent = new FirstSpawnTeleportEvent(player, player.getLocation(), firstSpawn);
        plugin.getServer().getPluginManager().callEvent(tpEvent);
        if (!tpEvent.isCancelled()) {
            if (player.isOnline() && tpEvent.getTo() != null && tpEvent.getTo().getWorld() != null) {
                final Location fLoc = tpEvent.getTo();
                Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {

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
        if (spawnLoc == null)
            return;
        if (!Settings.noTeleport)
            return;
        if (Settings.isTeleportToSpawnEnabled || (Settings.isForceSpawnLocOnJoinEnabled && Settings.getForcedWorlds.contains(player.getWorld().getName())))
            return;
        if (!player.hasPlayedBefore())
            return;
        sched.scheduleSyncDelayedTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (spawnLoc.getWorld() == null) {
                    return;
                }
                Material cur = player.getLocation().getBlock().getType();
                Material top = player.getLocation().add(0D, 1D, 0D).getBlock().getType();
                if (cur == Material.PORTAL || cur == Material.ENDER_PORTAL
                    || top == Material.PORTAL || top == Material.ENDER_PORTAL) {
                    m.send(player, MessageKey.UNSAFE_QUIT_LOCATION);
                    player.teleport(spawnLoc);
                }
            }

        });
    }

}
