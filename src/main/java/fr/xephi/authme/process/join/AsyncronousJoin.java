package fr.xephi.authme.process.join;

import java.util.Date;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.Utils;
import fr.xephi.authme.Utils.groupType;
import fr.xephi.authme.api.API;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.backup.DataFileCache;
import fr.xephi.authme.cache.backup.FileCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.ProtectInventoryEvent;
import fr.xephi.authme.events.SessionEvent;
import fr.xephi.authme.events.SpawnTeleportEvent;
import fr.xephi.authme.listener.AuthMePlayerListener;
import fr.xephi.authme.plugin.manager.CombatTagComunicator;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.task.MessageTask;
import fr.xephi.authme.task.TimeoutTask;

public class AsyncronousJoin {

    protected Player player;
    protected DataSource database;
    protected AuthMe plugin;
    protected String name;
    private Utils utils = Utils.getInstance();
    private Messages m = Messages.getInstance();
    private FileCache playerBackup;

    public AsyncronousJoin(Player player, AuthMe plugin, DataSource database) {
        this.player = player;
        this.plugin = plugin;
        this.database = database;
        this.playerBackup = new FileCache(plugin);
        this.name = player.getName().toLowerCase();
    }

    public void process() {
        AuthMePlayerListener.gameMode.put(name, player.getGameMode());
        BukkitScheduler sched = plugin.getServer().getScheduler();

        if (plugin.getCitizensCommunicator().isNPC(player, plugin) || Utils.getInstance().isUnrestricted(player) || CombatTagComunicator.isNPC(player)) {
            return;
        }

        if (plugin.ess != null && Settings.disableSocialSpy) {
            try {
                plugin.ess.getUser(player.getName().toLowerCase()).setSocialSpyEnabled(false);
            } catch (Exception e) {
            } catch (NoSuchMethodError e) {
            }
        }

        final String ip = plugin.getIP(player);
        if (Settings.isAllowRestrictedIp && !Settings.getRestrictedIp(name, ip)) {
            final GameMode gM = AuthMePlayerListener.gameMode.get(name);
            sched.scheduleSyncDelayedTask(plugin, new Runnable() {

                @Override
                public void run() {
                    AuthMePlayerListener.causeByAuthMe.put(name, true);
                    player.setGameMode(gM);
                    AuthMePlayerListener.causeByAuthMe.put(name, false);
                    player.kickPlayer("You are not the Owner of this account, please try another name!");
                    if (Settings.banUnsafeIp)
                        plugin.getServer().banIP(ip);
                }

            });
            return;
        }
        if (Settings.getMaxJoinPerIp > 0 && !plugin.authmePermissible(player, "authme.allow2accounts") && !ip.equalsIgnoreCase("127.0.0.1") && !ip.equalsIgnoreCase("localhost")) {
            if (plugin.hasJoinedIp(player.getName(), ip)) {
                player.kickPlayer("A player with the same IP is already in game!");
                return;
            }
        }
        final Location spawnLoc = plugin.getSpawnLocation(player);
        if (database.isAuthAvailable(name)) {
            if (Settings.isSessionsEnabled) {
                PlayerAuth auth = database.getAuth(name);
                long timeout = Settings.getSessionTimeout * 60000;
                long lastLogin = auth.getLastLogin();
                long cur = new Date().getTime();
                if ((cur - lastLogin < timeout || timeout == 0) && !auth.getIp().equals("198.18.0.1")) {
                    if (auth.getNickname().equalsIgnoreCase(name) && auth.getIp().equals(ip)) {
                        if (PlayerCache.getInstance().getAuth(name) != null) {
                            PlayerCache.getInstance().updatePlayer(auth);
                        } else {
                            PlayerCache.getInstance().addPlayer(auth);
                            database.setLogged(name);
                        }
                        m.send(player, "valid_session");
                        // Restore Permission Group
                        utils.setGroup(player, Utils.groupType.LOGGEDIN);
                        plugin.getServer().getPluginManager().callEvent(new SessionEvent(auth, true));
                        return;
                    } else if (!Settings.sessionExpireOnIpChange) {
                        final GameMode gM = AuthMePlayerListener.gameMode.get(name);
                        sched.scheduleSyncDelayedTask(plugin, new Runnable() {

                            @Override
                            public void run() {
                                AuthMePlayerListener.causeByAuthMe.put(name, true);
                                player.setGameMode(gM);
                                AuthMePlayerListener.causeByAuthMe.put(name, false);
                                player.kickPlayer(m.send("unvalid_session")[0]);
                            }

                        });
                        return;
                    } else if (auth.getNickname().equalsIgnoreCase(name)) {
                        if (Settings.isForceSurvivalModeEnabled && !Settings.forceOnlyAfterLogin) {
                            sched.scheduleSyncDelayedTask(plugin, new Runnable() {

                                @Override
                                public void run() {
                                    AuthMePlayerListener.causeByAuthMe.put(name, true);
                                    Utils.forceGM(player);
                                    AuthMePlayerListener.causeByAuthMe.put(name, false);
                                }

                            });
                        }
                        // Player change his IP between 2 relog-in
                        PlayerCache.getInstance().removePlayer(name);
                        database.setUnlogged(name);
                    } else {
                        final GameMode gM = AuthMePlayerListener.gameMode.get(name);
                        sched.scheduleSyncDelayedTask(plugin, new Runnable() {

                            @Override
                            public void run() {
                                AuthMePlayerListener.causeByAuthMe.put(name, true);
                                player.setGameMode(gM);
                                AuthMePlayerListener.causeByAuthMe.put(name, false);
                                player.kickPlayer(m.send("unvalid_session")[0]);
                            }

                        });
                        return;
                    }
                } else {
                    // Session is ended correctly
                    PlayerCache.getInstance().removePlayer(name);
                    database.setUnlogged(name);
                }
            }
            // isent in session or session was ended correctly
            if (Settings.isForceSurvivalModeEnabled && !Settings.forceOnlyAfterLogin) {
                sched.scheduleSyncDelayedTask(plugin, new Runnable() {

                    @Override
                    public void run() {
                        AuthMePlayerListener.causeByAuthMe.put(name, true);
                        Utils.forceGM(player);
                        AuthMePlayerListener.causeByAuthMe.put(name, false);
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
                                if (player != null && player.isOnline() && tpEvent.getTo() != null) {
                                    if (tpEvent.getTo().getWorld() != null)
                                        player.teleport(tpEvent.getTo());
                                }
                            }
                        }

                    });
                }
            placePlayerSafely(player, spawnLoc);
            LimboCache.getInstance().updateLimboPlayer(player);
            try {
                DataFileCache dataFile = new DataFileCache(LimboCache.getInstance().getLimboPlayer(name).getInventory(), LimboCache.getInstance().getLimboPlayer(name).getArmour());
                playerBackup.createCache(player, dataFile, LimboCache.getInstance().getLimboPlayer(name).getGroup(), LimboCache.getInstance().getLimboPlayer(name).getOperator(), LimboCache.getInstance().getLimboPlayer(name).isFlying());
            } catch (Exception e) {
                ConsoleLogger.showError("Error on creating an inventory cache for " + name + ", maybe inventory wipe in preparation...");
            }
        } else {
            if (Settings.isForceSurvivalModeEnabled && !Settings.forceOnlyAfterLogin) {
                sched.scheduleSyncDelayedTask(plugin, new Runnable() {

                    @Override
                    public void run() {
                        AuthMePlayerListener.causeByAuthMe.put(name, true);
                        Utils.forceGM(player);
                        AuthMePlayerListener.causeByAuthMe.put(name, false);
                    }

                });
            }
            if (!Settings.unRegisteredGroup.isEmpty()) {
                utils.setGroup(player, Utils.groupType.UNREGISTERED);
            }
            if (!Settings.isForcedRegistrationEnabled) {
                return;
            }
            if (!Settings.noTeleport)
                if (Settings.isTeleportToSpawnEnabled || (Settings.isForceSpawnLocOnJoinEnabled && Settings.getForcedWorlds.contains(player.getWorld().getName()))) {
                    sched.scheduleSyncDelayedTask(plugin, new Runnable() {

                        @Override
                        public void run() {
                            SpawnTeleportEvent tpEvent = new SpawnTeleportEvent(player, player.getLocation(), spawnLoc, PlayerCache.getInstance().isAuthenticated(name));
                            plugin.getServer().getPluginManager().callEvent(tpEvent);
                            if (!tpEvent.isCancelled()) {
                                if (player != null && player.isOnline() && tpEvent.getTo() != null) {
                                    if (tpEvent.getTo().getWorld() != null)
                                        player.teleport(tpEvent.getTo());
                                }
                            }
                        }

                    });
                }

        }
        if (Settings.protectInventoryBeforeLogInEnabled) {
            sched.scheduleSyncDelayedTask(plugin, new Runnable() {

                @Override
                public void run() {
                    try {
                        LimboPlayer limbo = LimboCache.getInstance().getLimboPlayer(player.getName().toLowerCase());
                        ProtectInventoryEvent ev = new ProtectInventoryEvent(player, limbo.getInventory(), limbo.getArmour());
                        plugin.getServer().getPluginManager().callEvent(ev);
                        if (ev.isCancelled()) {
                            if (!Settings.noConsoleSpam)
                                ConsoleLogger.info("ProtectInventoryEvent has been cancelled for " + player.getName() + " ...");
                        } else {
                            API.setPlayerInventory(player, ev.getEmptyInventory(), ev.getEmptyArmor());
                        }
                    } catch (NullPointerException ex) {
                    }
                }

            });
        }
        String[] msg;
        if (Settings.emailRegistration) {
            msg = database.isAuthAvailable(name) ? m.send("login_msg") : m.send("reg_email_msg");
        } else {
            msg = database.isAuthAvailable(name) ? m.send("login_msg") : m.send("reg_msg");
        }
        int time = Settings.getRegistrationTimeout * 20;
        int msgInterval = Settings.getWarnMessageInterval;
        if (time != 0) {
            BukkitTask id = sched.runTaskLater(plugin, new TimeoutTask(plugin, name), time);
            if (!LimboCache.getInstance().hasLimboPlayer(name))
                LimboCache.getInstance().addLimboPlayer(player);
            LimboCache.getInstance().getLimboPlayer(name).setTimeoutTaskId(id);
        }
        if (!LimboCache.getInstance().hasLimboPlayer(name))
            LimboCache.getInstance().addLimboPlayer(player);
        if (database.isAuthAvailable(name)) {
            utils.setGroup(player, groupType.NOTLOGGEDIN);
        } else {
            utils.setGroup(player, groupType.UNREGISTERED);
        }
        sched.scheduleSyncDelayedTask(plugin, new Runnable() {

            @Override
            public void run() {
                if (player.isOp())
                    player.setOp(false);
                if (!Settings.isMovementAllowed) {
                    player.setAllowFlight(true);
                    player.setFlying(true);
                }
                player.setNoDamageTicks(Settings.getRegistrationTimeout * 20);
                if (Settings.useEssentialsMotd)
                    player.performCommand("motd");
                if (Settings.applyBlindEffect)
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Settings.getRegistrationTimeout * 20, 2));
            }

        });
        BukkitTask msgT = sched.runTask(plugin, new MessageTask(plugin, name, msg, msgInterval));
        LimboCache.getInstance().getLimboPlayer(name).setMessageTaskId(msgT);
    }

    private void placePlayerSafely(final Player player, final Location spawnLoc) {
        Location loc = null;
        if (spawnLoc == null)
            return;
        if (!Settings.noTeleport)
            return;
        if (Settings.isTeleportToSpawnEnabled || (Settings.isForceSpawnLocOnJoinEnabled && Settings.getForcedWorlds.contains(player.getWorld().getName())))
            return;
        Block b = player.getLocation().getBlock();
        if (b.getType() == Material.PORTAL || b.getType() == Material.ENDER_PORTAL || b.getType() == Material.LAVA || b.getType() == Material.STATIONARY_LAVA) {
            m.send(player, "unsafe_spawn");
            if (spawnLoc.getWorld() != null)
                loc = spawnLoc;
        } else {
            Block c = player.getLocation().add(0D, 1D, 0D).getBlock();
            if (c.getType() == Material.PORTAL || c.getType() == Material.ENDER_PORTAL || c.getType() == Material.LAVA || c.getType() == Material.STATIONARY_LAVA) {
                m.send(player, "unsafe_spawn");
                if (spawnLoc.getWorld() != null)
                    loc = spawnLoc;
            }
        }
        if (loc != null) {
            final Location floc = loc;
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {

                @Override
                public void run() {
                    player.teleport(floc);
                }

            });
        }
    }

}
