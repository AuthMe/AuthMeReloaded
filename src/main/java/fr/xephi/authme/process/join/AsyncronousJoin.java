package fr.xephi.authme.process.join;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.Utils;
import fr.xephi.authme.Utils.GroupType;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.backup.DataFileCache;
import fr.xephi.authme.cache.backup.JsonCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.FirstSpawnTeleportEvent;
import fr.xephi.authme.events.ProtectInventoryEvent;
import fr.xephi.authme.events.SpawnTeleportEvent;
import fr.xephi.authme.listener.AuthMePlayerListener;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.Spawn;
import fr.xephi.authme.task.MessageTask;
import fr.xephi.authme.task.TimeoutTask;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;

public class AsyncronousJoin {

    protected Player player;
    protected DataSource database;
    protected AuthMe plugin;
    protected String name;
    private Messages m = Messages.getInstance();
    private JsonCache playerBackup;

    public AsyncronousJoin(Player player, AuthMe plugin, DataSource database) {
        this.player = player;
        this.plugin = plugin;
        this.database = database;
        this.playerBackup = new JsonCache(plugin);
        this.name = player.getName().toLowerCase();
    }

    public void process() {
        if (AuthMePlayerListener.gameMode.containsKey(name))
            AuthMePlayerListener.gameMode.remove(name);
        AuthMePlayerListener.gameMode.putIfAbsent(name, player.getGameMode());
        BukkitScheduler sched = plugin.getServer().getScheduler();

        if (Utils.isNPC(player) || Utils.isUnrestricted(player)) {
            return;
        }

        if (plugin.ess != null && Settings.disableSocialSpy) {
            plugin.ess.getUser(player).setSocialSpyEnabled(false);
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
        if (Settings.getMaxJoinPerIp > 0 && !plugin.authmePermissible(player, "authme.allow2accounts") && !ip.equalsIgnoreCase("127.0.0.1") && !ip.equalsIgnoreCase("localhost")) {
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
            DataFileCache dataFile = new DataFileCache(LimboCache.getInstance().getLimboPlayer(name).getInventory(), LimboCache.getInstance().getLimboPlayer(name).getArmour());
            playerBackup.createCache(player, dataFile);
            // protect inventory
            if (Settings.protectInventoryBeforeLogInEnabled) {
                LimboPlayer limbo = LimboCache.getInstance().getLimboPlayer(player.getName().toLowerCase());
                ProtectInventoryEvent ev = new ProtectInventoryEvent(player, limbo.getInventory(), limbo.getArmour());
                plugin.getServer().getPluginManager().callEvent(ev);
                if (ev.isCancelled()) {
                    if (!Settings.noConsoleSpam)
                        ConsoleLogger.info("ProtectInventoryEvent has been cancelled for " + player.getName() + " ...");
                } else {
                    final ItemStack[] inv = ev.getEmptyArmor();
                    final ItemStack[] armor = ev.getEmptyArmor();
                    sched.scheduleSyncDelayedTask(plugin, new Runnable() {

                        @Override
                        public void run() {
                            plugin.api.setPlayerInventory(player, inv, armor);
                        }

                    });
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
                if (!needFirstspawn() && Settings.isTeleportToSpawnEnabled || (Settings.isForceSpawnLocOnJoinEnabled && Settings.getForcedWorlds.contains(player.getWorld().getName()))) {
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
        String[] msg;
        if (Settings.emailRegistration) {
            msg = isAuthAvailable ? m.send("login_msg") : m.send("reg_email_msg");
        } else {
            msg = isAuthAvailable ? m.send("login_msg") : m.send("reg_msg");
        }
        int time = Settings.getRegistrationTimeout * 20;
        int msgInterval = Settings.getWarnMessageInterval;
        if (time != 0) {
            BukkitTask id = sched.runTaskLaterAsynchronously(plugin, new TimeoutTask(plugin, name, player), time);
            if (!LimboCache.getInstance().hasLimboPlayer(name))
                LimboCache.getInstance().addLimboPlayer(player);
            LimboCache.getInstance().getLimboPlayer(name).setTimeoutTaskId(id);
        }
        if (!LimboCache.getInstance().hasLimboPlayer(name))
            LimboCache.getInstance().addLimboPlayer(player);
        if (isAuthAvailable) {
            Utils.setGroup(player, GroupType.NOTLOGGEDIN);
        } else {
            Utils.setGroup(player, GroupType.UNREGISTERED);
        }
        sched.scheduleSyncDelayedTask(plugin, new Runnable() {

            @Override
            public void run() {
                if (player.isOp())
                    player.setOp(false);
                if (player.getGameMode() != GameMode.CREATIVE && !Settings.isMovementAllowed) {
                    player.setAllowFlight(true);
                    player.setFlying(true);
                }
                player.setNoDamageTicks(Settings.getRegistrationTimeout * 20);
                if (Settings.useEssentialsMotd)
                    player.performCommand("motd");
                if (Settings.applyBlindEffect)
                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Settings.getRegistrationTimeout * 20, 2));
                if (!Settings.isMovementAllowed && Settings.isRemoveSpeedEnabled) {
                    player.setWalkSpeed(0.0f);
                    player.setFlySpeed(0.0f);
                }
            }

        });
        if (Settings.isSessionsEnabled && isAuthAvailable && (PlayerCache.getInstance().isAuthenticated(name) || database.isLogged(name))) {
            if (plugin.sessions.containsKey(name))
                plugin.sessions.get(name).cancel();
            plugin.sessions.remove(name);
            PlayerAuth auth = database.getAuth(name);
            if (auth != null && auth.getIp().equals(ip)) {
                m.send(player, "valid_session");
                PlayerCache.getInstance().removePlayer(name);
                database.setUnlogged(name);
                plugin.management.performLogin(player, "dontneed", true);
            } else if (Settings.sessionExpireOnIpChange) {
                PlayerCache.getInstance().removePlayer(name);
                database.setUnlogged(name);
                m.send(player, "invalid_session");
            }
            return;
        }
        BukkitTask msgT = sched.runTaskAsynchronously(plugin, new MessageTask(plugin, name, msg, msgInterval));
        LimboCache.getInstance().getLimboPlayer(name).setMessageTaskId(msgT);
    }

    private boolean needFirstspawn() {
        if (player.hasPlayedBefore())
            return false;
        if (Spawn.getInstance().getFirstSpawn() == null || Spawn.getInstance().getFirstSpawn().getWorld() == null)
            return false;
        FirstSpawnTeleportEvent tpEvent = new FirstSpawnTeleportEvent(player, player.getLocation(), Spawn.getInstance().getFirstSpawn());
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

    private void placePlayerSafely(final Player player,
                                   final Location spawnLoc) {
        Location loc = null;
        if (spawnLoc == null)
            return;
        if (!Settings.noTeleport)
            return;
        if (Settings.isTeleportToSpawnEnabled || (Settings.isForceSpawnLocOnJoinEnabled && Settings.getForcedWorlds.contains(player.getWorld().getName())))
            return;
        if (!player.hasPlayedBefore())
            return;
        Block b = player.getLocation().getBlock();
        if (b.getType() == Material.PORTAL || b.getType() == Material.ENDER_PORTAL) {
            m.send(player, "unsafe_spawn");
            if (spawnLoc.getWorld() != null)
                loc = spawnLoc;
        } else {
            Block c = player.getLocation().add(0D, 1D, 0D).getBlock();
            if (c.getType() == Material.PORTAL || c.getType() == Material.ENDER_PORTAL) {
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
