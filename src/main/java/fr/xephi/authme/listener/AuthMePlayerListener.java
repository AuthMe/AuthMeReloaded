package fr.xephi.authme.listener;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.regex.PatternSyntaxException;

import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerGameModeChangeEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerLoginEvent.Result;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.scheduler.BukkitScheduler;

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
import fr.xephi.authme.events.AuthMeTeleportEvent;
import fr.xephi.authme.events.ProtectInventoryEvent;
import fr.xephi.authme.events.RestoreInventoryEvent;
import fr.xephi.authme.events.SessionEvent;
import fr.xephi.authme.events.SpawnTeleportEvent;
import fr.xephi.authme.plugin.manager.CombatTagComunicator;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.task.MessageTask;
import fr.xephi.authme.task.TimeoutTask;

public class AuthMePlayerListener implements Listener {

    public static GameMode gm = GameMode.SURVIVAL;
    public static HashMap<String, GameMode> gameMode = new HashMap<String, GameMode>();
    public static HashMap<String, String> joinMessage = new HashMap<String, String>();
    private Utils utils = Utils.getInstance();
    private Messages m = Messages.getInstance();
    public AuthMe plugin;
    private DataSource data;
    private FileCache playerBackup;
    public static HashMap<String, Boolean> causeByAuthMe = new HashMap<String, Boolean>();
    private HashMap<String, PlayerLoginEvent> antibot = new HashMap<String, PlayerLoginEvent>();

    public AuthMePlayerListener(AuthMe plugin, DataSource data) {
        this.plugin = plugin;
        this.data = data;
        this.playerBackup = new FileCache(plugin);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (event.isCancelled() || event.getPlayer() == null)
            return;

        Player player = event.getPlayer();
        String name = player.getName();

        if (Utils.getInstance().isUnrestricted(player))
            return;

        if (PlayerCache.getInstance().isAuthenticated(name))
            return;

        if (!data.isAuthAvailable(name))
            if (!Settings.isForcedRegistrationEnabled)
                return;

        String msg = event.getMessage();
        // WorldEdit GUI Shit
        if (msg.equalsIgnoreCase("/worldedit cui"))
            return;

        String cmd = msg.split(" ")[0];
        if (cmd.equalsIgnoreCase("/login") || cmd.equalsIgnoreCase("/register") || cmd.equalsIgnoreCase("/passpartu") || cmd.equalsIgnoreCase("/l") || cmd.equalsIgnoreCase("/reg") || cmd.equalsIgnoreCase("/email") || cmd.equalsIgnoreCase("/captcha"))
            return;
        if (Settings.useEssentialsMotd && cmd.equalsIgnoreCase("/motd"))
            return;
        if (Settings.allowCommands.contains(cmd))
            return;

        event.setMessage("/notloggedin");
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerNormalChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled() || event.getPlayer() == null)
            return;

        final Player player = event.getPlayer();
        final String name = player.getName();

        if (Utils.getInstance().isUnrestricted(player))
            return;

        if (PlayerCache.getInstance().isAuthenticated(name))
            return;

        String cmd = event.getMessage().split(" ")[0];

        if (data.isAuthAvailable(name)) {
            m._(player, "login_msg");
        } else {
            if (!Settings.isForcedRegistrationEnabled) {
                return;
            }
            if (Settings.emailRegistration) {
                m._(player, "reg_email_msg");
                return;
            } else {
                m._(player, "reg_msg");
                return;
            }
        }

        if (!Settings.isChatAllowed && !(Settings.allowCommands.contains(cmd))) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGH)
    public void onPlayerHighChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled() || event.getPlayer() == null)
            return;

        final Player player = event.getPlayer();
        final String name = player.getName();

        if (Utils.getInstance().isUnrestricted(player))
            return;

        if (PlayerCache.getInstance().isAuthenticated(name))
            return;

        String cmd = event.getMessage().split(" ")[0];

        if (data.isAuthAvailable(name)) {
            m._(player, "login_msg");
        } else {
            if (!Settings.isForcedRegistrationEnabled) {
                return;
            }
            if (Settings.emailRegistration) {
                m._(player, "reg_email_msg");
                return;
            } else {
                m._(player, "reg_msg");
                return;
            }
        }

        if (!Settings.isChatAllowed && !(Settings.allowCommands.contains(cmd))) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled() || event.getPlayer() == null)
            return;

        final Player player = event.getPlayer();
        final String name = player.getName();

        if (Utils.getInstance().isUnrestricted(player))
            return;

        if (PlayerCache.getInstance().isAuthenticated(name))
            return;

        String cmd = event.getMessage().split(" ")[0];

        if (data.isAuthAvailable(name)) {
            m._(player, "login_msg");
        } else {
            if (!Settings.isForcedRegistrationEnabled) {
                return;
            }
            if (Settings.emailRegistration) {
                m._(player, "reg_email_msg");
                return;
            } else {
                m._(player, "reg_msg");
                return;
            }
        }

        if (!Settings.isChatAllowed && !(Settings.allowCommands.contains(cmd))) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerHighestChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled() || event.getPlayer() == null)
            return;

        final Player player = event.getPlayer();
        final String name = player.getName();

        if (Utils.getInstance().isUnrestricted(player))
            return;

        if (PlayerCache.getInstance().isAuthenticated(name))
            return;

        String cmd = event.getMessage().split(" ")[0];

        if (data.isAuthAvailable(name)) {
            m._(player, "login_msg");
        } else {
            if (!Settings.isForcedRegistrationEnabled) {
                return;
            }
            if (Settings.emailRegistration) {
                m._(player, "reg_email_msg");
                return;
            } else {
                m._(player, "reg_msg");
                return;
            }
        }

        if (!Settings.isChatAllowed && !(Settings.allowCommands.contains(cmd))) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerEarlyChat(final AsyncPlayerChatEvent event) {
        if (event.isCancelled() || event.getPlayer() == null)
            return;

        final Player player = event.getPlayer();
        final String name = player.getName();

        if (Utils.getInstance().isUnrestricted(player))
            return;

        if (PlayerCache.getInstance().isAuthenticated(name)) {
            return;
        }

        String cmd = event.getMessage().split(" ")[0];

        if (data.isAuthAvailable(name)) {
            m._(player, "login_msg");
        } else {
            if (!Settings.isForcedRegistrationEnabled) {
                return;
            }
            if (Settings.emailRegistration) {
                m._(player, "reg_email_msg");
                return;
            } else {
                m._(player, "reg_msg");
                return;
            }
        }

        if (!Settings.isChatAllowed && !(Settings.allowCommands.contains(cmd))) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerLowChat(AsyncPlayerChatEvent event) {
        if (event.isCancelled() || event.getPlayer() == null)
            return;

        final Player player = event.getPlayer();
        final String name = player.getName();

        if (Utils.getInstance().isUnrestricted(player))
            return;

        if (PlayerCache.getInstance().isAuthenticated(name))
            return;

        String cmd = event.getMessage().split(" ")[0];

        if (data.isAuthAvailable(name)) {
            m._(player, "login_msg");
        } else {
            if (!Settings.isForcedRegistrationEnabled) {
                return;
            }
            if (Settings.emailRegistration) {
                m._(player, "reg_email_msg");
            } else {
                m._(player, "reg_msg");
            }
        }

        if (!Settings.isChatAllowed && !(Settings.allowCommands.contains(cmd))) {
            event.setCancelled(true);
            return;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (event.isCancelled() || event.getPlayer() == null) {
            return;
        }

        Player player = event.getPlayer();
        String name = player.getName();

        if (plugin.getCitizensCommunicator().isNPC(player, plugin) || Utils.getInstance().isUnrestricted(player) || CombatTagComunicator.isNPC(player)) {
            return;
        }

        if (PlayerCache.getInstance().isAuthenticated(name)) {
            return;
        }

        if (!Settings.isForcedRegistrationEnabled) {
            return;
        }

        if (!Settings.isMovementAllowed) {
            event.setTo(event.getFrom());
            return;
        }

        if (Settings.getMovementRadius == 0) {
            return;
        }

        int radius = Settings.getMovementRadius;
        Location spawn = plugin.getSpawnLocation(player);

        if (spawn != null && spawn.getWorld() != null)
        	if (!event.getPlayer().getWorld().equals(spawn.getWorld())) {
        		event.getPlayer().teleport(spawn);
        		return;
        	}
        if ((spawn.distance(player.getLocation()) > radius) && spawn.getWorld() != null) {
            event.getPlayer().teleport(spawn);
            return;
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {

        final Player player = event.getPlayer();
        final String lowname = player.getName().toLowerCase();
        final String name = player.getName();

        if (!lowname.equals(name)) {
            // Little workaround to be sure registered player is the same as this
            if (player.hasPlayedBefore() && !player.isOnline())
                // Make sure it's the correct player
                if (data.isAuthAvailable(lowname)) {
                    if (data.getAuth(lowname).getIp().equalsIgnoreCase(player.getAddress().getAddress().getHostAddress())) {
                        data.updateName(lowname, name);
                    } else {
                        event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
                        event.setKickMessage(m._("same_nick")[0]);
                    }
                }
        }

        if (plugin.getCitizensCommunicator().isNPC(player, plugin) || Utils.getInstance().isUnrestricted(player) || CombatTagComunicator.isNPC(player)) {
            return;
        }

        if (!Settings.countriesBlacklist.isEmpty()) {
            String code = plugin.getCountryCode(event.getAddress().getHostAddress());
            if (((code == null) || (Settings.countriesBlacklist.contains(code) && !API.isRegistered(name))) && !plugin.authmePermissible(player, "authme.bypassantibot")) {
                event.disallow(PlayerLoginEvent.Result.KICK_OTHER, m._("country_banned")[0]);
                return;
            }
        }
        if (Settings.enableProtection && !Settings.countries.isEmpty()) {
            String code = plugin.getCountryCode(event.getAddress().getHostAddress());
            if (((code == null) || (!Settings.countries.contains(code) && !API.isRegistered(name))) && !plugin.authmePermissible(player, "authme.bypassantibot")) {
                event.disallow(PlayerLoginEvent.Result.KICK_OTHER, m._("country_banned")[0]);
                return;
            }
        }

        if (Settings.isKickNonRegisteredEnabled) {
            if (!data.isAuthAvailable(name)) {
                event.disallow(PlayerLoginEvent.Result.KICK_OTHER, m._("reg_only")[0]);
                return;
            }
        }

        if (player.isOnline() && Settings.isForceSingleSessionEnabled) {
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, m._("same_nick")[0]);
            return;
        }

        if (data.isAuthAvailable(name) && !LimboCache.getInstance().hasLimboPlayer(name)) {
            if (!Settings.isSessionsEnabled) {
            } else if (PlayerCache.getInstance().isAuthenticated(name)) {
                if (!Settings.sessionExpireOnIpChange)
                    if (LimboCache.getInstance().hasLimboPlayer(player.getName())) {
                        LimboCache.getInstance().deleteLimboPlayer(name);
                    }
            }
        }
        // Check if forceSingleSession is set to true, so kick player that has
        // joined with same nick of online player
        if (player.isOnline() && Settings.isForceSingleSessionEnabled) {
            LimboPlayer limbo = LimboCache.getInstance().getLimboPlayer(player.getName());
            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, m._("same_nick")[0]);
            if (PlayerCache.getInstance().isAuthenticated(player.getName())) {
                utils.addNormal(player, limbo.getGroup());
                LimboCache.getInstance().deleteLimboPlayer(player.getName());
            }
            return;
        }

        int min = Settings.getMinNickLength;
        int max = Settings.getMaxNickLength;
        String regex = Settings.getNickRegex;

        if (name.length() > max || name.length() < min) {

            event.disallow(PlayerLoginEvent.Result.KICK_OTHER, m._("name_len")[0]);
            return;
        }
        try {
            if (!player.getName().matches(regex) || name.equals("Player")) {
                try {
                    event.disallow(PlayerLoginEvent.Result.KICK_OTHER, m._("regex")[0].replace("REG_EX", regex));
                } catch (Exception exc) {
                    event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "allowed char : " + regex);
                }
                return;
            }
        } catch (PatternSyntaxException pse) {
            if (regex == null || regex.isEmpty()) {
                event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "Your nickname do not match");
                return;
            }
            try {
                event.disallow(PlayerLoginEvent.Result.KICK_OTHER, m._("regex")[0].replace("REG_EX", regex));
            } catch (Exception exc) {
                event.disallow(PlayerLoginEvent.Result.KICK_OTHER, "allowed char : " + regex);
            }
            return;
        }

        if (event.getResult() == PlayerLoginEvent.Result.ALLOWED) {
            checkAntiBotMod(event);
            if (Settings.bungee) {
                final ByteArrayOutputStream b = new ByteArrayOutputStream();
                DataOutputStream out = new DataOutputStream(b);

                try {
                    out.writeUTF("IP");
                } catch (IOException e) {
                }
                player.sendPluginMessage(plugin, "BungeeCord", b.toByteArray());
            }
            return;
        }
        if (event.getResult() != PlayerLoginEvent.Result.KICK_FULL)
            return;
        if (player.isBanned())
            return;
        if (!plugin.authmePermissible(player, "authme.vip")) {
            event.disallow(Result.KICK_FULL, m._("kick_fullserver")[0]);
            return;
        }

        if (plugin.getServer().getOnlinePlayers().length > plugin.getServer().getMaxPlayers()) {
            event.allow();
            return;
        } else {
            final Player pl = plugin.generateKickPlayer(plugin.getServer().getOnlinePlayers());
            if (pl != null) {
                pl.kickPlayer(m._("kick_forvip")[0]);
                event.allow();
                return;
            } else {
                ConsoleLogger.info("The player " + player.getName() + " wants to join, but the server is full");
                event.disallow(Result.KICK_FULL, m._("kick_fullserver")[0]);
                return;
            }
        }
    }

    private void checkAntiBotMod(final PlayerLoginEvent event) {
        if (plugin.delayedAntiBot || plugin.antibotMod)
            return;
        if (plugin.authmePermissible(event.getPlayer(), "authme.bypassantibot"))
            return;
        if (antibot.keySet().size() > Settings.antiBotSensibility) {
            plugin.switchAntiBotMod(true);
            for (String s : m._("antibot_auto_enabled"))
                Bukkit.broadcastMessage(s);
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {

                @Override
                public void run() {
                    if (plugin.antibotMod) {
                        plugin.switchAntiBotMod(false);
                        antibot.clear();
                        for (String s : m._("antibot_auto_disabled"))
                            Bukkit.broadcastMessage(s.replace("%m", "" + Settings.antiBotDuration));
                    }
                }
            }, Settings.antiBotDuration * 1200);
            return;
        }
        antibot.put(event.getPlayer().getName(), event);
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {

            @Override
            public void run() {
                antibot.remove(event.getPlayer().getName());
            }
        }, 300);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (event.getPlayer() == null) {
            return;
        }
        Player player = event.getPlayer();
        final String name = player.getName();
        Location spawnLoc = plugin.getSpawnLocation(player);
        gm = player.getGameMode();
        gameMode.put(name, gm);
        BukkitScheduler sched = plugin.getServer().getScheduler();

        if (plugin.getCitizensCommunicator().isNPC(player, plugin) || Utils.getInstance().isUnrestricted(player) || CombatTagComunicator.isNPC(player)) {
            return;
        }

        if (plugin.ess != null && Settings.disableSocialSpy) {
            try {
                plugin.ess.getUser(player.getName()).setSocialSpyEnabled(false);
            } catch (Exception e) {
            } catch (NoSuchMethodError e) {
            }
        }

        String ip = plugin.getIP(player);
        if (Settings.isAllowRestrictedIp && !Settings.getRestrictedIp(name, ip)) {
            GameMode gM = gameMode.get(name);
            causeByAuthMe.put(name, true);
            player.setGameMode(gM);
            causeByAuthMe.put(name, false);
            player.kickPlayer("You are not the Owner of this account, please try another name!");
            if (Settings.banUnsafeIp)
                plugin.getServer().banIP(ip);
            return;
        }
        if (Settings.getMaxJoinPerIp > 0 && !plugin.authmePermissible(player, "authme.allow2accounts") && !ip.equalsIgnoreCase("127.0.0.1") && !ip.equalsIgnoreCase("localhost")) {
            if (plugin.hasJoinedIp(player.getName(), ip)) {
                player.kickPlayer("A player with the same IP is already in game!");
                return;
            }
        }
        if (data.isAuthAvailable(name)) {
            if (Settings.isSessionsEnabled) {
                PlayerAuth auth = data.getAuth(name);
                long timeout = Settings.getSessionTimeout * 60000;
                long lastLogin = auth.getLastLogin();
                long cur = new Date().getTime();
                if ((cur - lastLogin < timeout || timeout == 0) && !auth.getIp().equals("198.18.0.1")) {
                    if (auth.getNickname().equalsIgnoreCase(name) && auth.getIp().equals(ip)) {
                        if (PlayerCache.getInstance().getAuth(name) != null) {
                            PlayerCache.getInstance().updatePlayer(auth);
                        } else {
                            PlayerCache.getInstance().addPlayer(auth);
                            data.setLogged(name);
                        }
                        m._(player, "valid_session");
                        // Restore Permission Group
                        utils.setGroup(player, Utils.groupType.LOGGEDIN);
                        plugin.getServer().getPluginManager().callEvent(new SessionEvent(auth, true));
                        return;
                    } else if (!Settings.sessionExpireOnIpChange) {
                        GameMode gM = gameMode.get(name);
                        causeByAuthMe.put(name, true);
                        player.setGameMode(gM);
                        causeByAuthMe.put(name, false);
                        player.kickPlayer(m._("unvalid_session")[0]);
                        return;
                    } else if (auth.getNickname().equalsIgnoreCase(name)) {
                        if (Settings.isForceSurvivalModeEnabled && !Settings.forceOnlyAfterLogin) {
                            causeByAuthMe.put(name, true);
                            Utils.forceGM(player);
                            causeByAuthMe.put(name, false);
                        }
                        // Player change his IP between 2 relog-in
                        PlayerCache.getInstance().removePlayer(name);
                        data.setUnlogged(name);
                    } else {
                        GameMode gM = gameMode.get(name);
                        causeByAuthMe.put(name, true);
                        player.setGameMode(gM);
                        causeByAuthMe.put(name, false);
                        player.kickPlayer(m._("unvalid_session")[0]);
                        return;
                    }
                } else {
                    // Session is ended correctly
                    PlayerCache.getInstance().removePlayer(name);
                    data.setUnlogged(name);
                }
            }
            // isent in session or session was ended correctly
            if (Settings.isForceSurvivalModeEnabled && !Settings.forceOnlyAfterLogin) {
                causeByAuthMe.put(name, true);
                Utils.forceGM(player);
                causeByAuthMe.put(name, false);
            }
            if (!Settings.noTeleport)
                if (Settings.isTeleportToSpawnEnabled || (Settings.isForceSpawnLocOnJoinEnabled && Settings.getForcedWorlds.contains(player.getWorld().getName()))) {
                    SpawnTeleportEvent tpEvent = new SpawnTeleportEvent(player, player.getLocation(), spawnLoc, PlayerCache.getInstance().isAuthenticated(name));
                    plugin.getServer().getPluginManager().callEvent(tpEvent);
                    if (!tpEvent.isCancelled()) {
                        if (player != null && player.isOnline() && tpEvent.getTo() != null) {
                        	if (tpEvent.getTo().getWorld() != null)
                        		player.teleport(tpEvent.getTo());
                        }
                    }
                }
            placePlayerSafely(player, spawnLoc);
            LimboCache.getInstance().updateLimboPlayer(player);
            DataFileCache dataFile = new DataFileCache(LimboCache.getInstance().getLimboPlayer(name).getInventory(), LimboCache.getInstance().getLimboPlayer(name).getArmour());
            playerBackup.createCache(player, dataFile, LimboCache.getInstance().getLimboPlayer(name).getGroup(), LimboCache.getInstance().getLimboPlayer(name).getOperator(), LimboCache.getInstance().getLimboPlayer(name).isFlying());
        } else {
            if (Settings.isForceSurvivalModeEnabled && !Settings.forceOnlyAfterLogin) {
                causeByAuthMe.put(name, true);
                Utils.forceGM(player);
                causeByAuthMe.put(name, false);
            }
            if (!Settings.unRegisteredGroup.isEmpty()) {
                utils.setGroup(player, Utils.groupType.UNREGISTERED);
            }
            if (!Settings.noTeleport)
                if (Settings.isTeleportToSpawnEnabled || (Settings.isForceSpawnLocOnJoinEnabled && Settings.getForcedWorlds.contains(player.getWorld().getName()))) {
                    SpawnTeleportEvent tpEvent = new SpawnTeleportEvent(player, player.getLocation(), spawnLoc, PlayerCache.getInstance().isAuthenticated(name));
                    plugin.getServer().getPluginManager().callEvent(tpEvent);
                    if (!tpEvent.isCancelled()) {
                        if (player != null && player.isOnline() && tpEvent.getTo() != null) {
                        	if (tpEvent.getTo().getWorld() != null)
                        		player.teleport(tpEvent.getTo());
                        }
                    }
                }
            if (!Settings.isForcedRegistrationEnabled) {
                return;
            }

        }
        if (Settings.protectInventoryBeforeLogInEnabled) {
            try {
                LimboPlayer limbo = LimboCache.getInstance().getLimboPlayer(player.getName());
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
        String[] msg;
        if (Settings.emailRegistration) {
            msg = data.isAuthAvailable(name) ? m._("login_msg") : m._("reg_email_msg");
        } else {
            msg = data.isAuthAvailable(name) ? m._("login_msg") : m._("reg_msg");
        }
        int time = Settings.getRegistrationTimeout * 20;
        int msgInterval = Settings.getWarnMessageInterval;
        if (time != 0) {
            int id = sched.scheduleSyncDelayedTask(plugin, new TimeoutTask(plugin, name), time);
            if (!LimboCache.getInstance().hasLimboPlayer(name))
                LimboCache.getInstance().addLimboPlayer(player);
            LimboCache.getInstance().getLimboPlayer(name).setTimeoutTaskId(id);
        }
        if (!LimboCache.getInstance().hasLimboPlayer(name))
            LimboCache.getInstance().addLimboPlayer(player);
        if (data.isAuthAvailable(name)) {
            utils.setGroup(player, groupType.NOTLOGGEDIN);
        } else {
            utils.setGroup(player, groupType.UNREGISTERED);
        }
        if (player.isOp())
            player.setOp(false);
        if (!Settings.isMovementAllowed) {
            player.setAllowFlight(true);
            player.setFlying(true);
        }
        int msgT = sched.scheduleSyncDelayedTask(plugin, new MessageTask(plugin, name, msg, msgInterval));
        LimboCache.getInstance().getLimboPlayer(name).setMessageTaskId(msgT);
        player.setNoDamageTicks(Settings.getRegistrationTimeout * 20);
        if (Settings.useEssentialsMotd)
            player.performCommand("motd");
        if (Settings.applyBlindEffect)
            player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, Settings.getRegistrationTimeout * 20, 2));

        // Remove the join message while the player isn't logging in
        if (Settings.enableProtection || Settings.delayJoinMessage) {
            joinMessage.put(name, event.getJoinMessage());
            event.setJoinMessage(null);
        }
    }

    private void placePlayerSafely(Player player, Location spawnLoc) {
        if (!Settings.noTeleport)
            return;
        if (Settings.isTeleportToSpawnEnabled || (Settings.isForceSpawnLocOnJoinEnabled && Settings.getForcedWorlds.contains(player.getWorld().getName())))
            return;
        Block b = player.getLocation().getBlock();
        if (b.getType() == Material.PORTAL || b.getType() == Material.ENDER_PORTAL || b.getType() == Material.LAVA || b.getType() == Material.STATIONARY_LAVA) {
            m._(player, "unsafe_spawn");
            if (spawnLoc.getWorld() != null)
            	player.teleport(spawnLoc);
            return;
        }
        Block c = player.getLocation().add(0D, 1D, 0D).getBlock();
        if (c.getType() == Material.PORTAL || c.getType() == Material.ENDER_PORTAL || c.getType() == Material.LAVA || c.getType() == Material.STATIONARY_LAVA) {
            m._(player, "unsafe_spawn");
            if (spawnLoc.getWorld() != null)
            	player.teleport(spawnLoc);
            return;
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (event.getPlayer() == null) {
            return;
        }

        Player player = event.getPlayer();
        String name = player.getName();
        Location loc = player.getLocation();

        if (plugin.getCitizensCommunicator().isNPC(player, plugin) || Utils.getInstance().isUnrestricted(player) || CombatTagComunicator.isNPC(player)) {
            return;
        }

        String ip = plugin.getIP(player);

        if (PlayerCache.getInstance().isAuthenticated(name) && !player.isDead()) {
            if (Settings.isSaveQuitLocationEnabled && data.isAuthAvailable(name)) {
                final PlayerAuth auth = new PlayerAuth(name, loc.getX(), loc.getY(), loc.getZ(), loc.getWorld().getName());
                try {
                    data.updateQuitLoc(auth);
                } catch (NullPointerException npe) {
                }
            }
            PlayerAuth auth = new PlayerAuth(name, ip, System.currentTimeMillis());
            data.updateSession(auth);
        }

        if (data.getAuth(name) != null && !PlayerCache.getInstance().isAuthenticated(name) && Settings.enableProtection)
            event.setQuitMessage(null);

        if (LimboCache.getInstance().hasLimboPlayer(name)) {
            LimboPlayer limbo = LimboCache.getInstance().getLimboPlayer(name);
            if (Settings.protectInventoryBeforeLogInEnabled && player.hasPlayedBefore()) {
                RestoreInventoryEvent ev = new RestoreInventoryEvent(player, limbo.getInventory(), limbo.getArmour());
                plugin.getServer().getPluginManager().callEvent(ev);
                if (!ev.isCancelled()) {
                    API.setPlayerInventory(player, ev.getInventory(), ev.getArmor());
                }
            }
            utils.addNormal(player, limbo.getGroup());
            player.setOp(limbo.getOperator());
            if (player.getGameMode() != GameMode.CREATIVE && !Settings.isMovementAllowed) {
                player.setAllowFlight(limbo.isFlying());
                player.setFlying(limbo.isFlying());
            }
            this.plugin.getServer().getScheduler().cancelTask(limbo.getTimeoutTaskId());
            this.plugin.getServer().getScheduler().cancelTask(limbo.getMessageTaskId());
            LimboCache.getInstance().deleteLimboPlayer(name);
            if (playerBackup.doesCacheExist(player)) {
                playerBackup.removeCache(player);
            }
        }
        PlayerCache.getInstance().removePlayer(name);
        data.setUnlogged(name);
        try {
            player.getVehicle().eject();
        } catch (NullPointerException ex) {
        }
        if (gameMode.containsKey(name))
            gameMode.remove(name);
        player.saveData();
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerKick(PlayerKickEvent event) {
        if (event.getPlayer() == null) {
            return;
        }
        if (event.isCancelled()) {
            return;
        }

        Player player = event.getPlayer();
        Location loc = player.getLocation();

        if ((plugin.getCitizensCommunicator().isNPC(player, plugin)) || (Utils.getInstance().isUnrestricted(player)) || (CombatTagComunicator.isNPC(player))) {
            return;
        }

        if ((Settings.isForceSingleSessionEnabled) && (event.getReason().contains("You logged in from another location"))) {
            event.setCancelled(true);
            return;
        }

        String name = player.getName();

        String ip = plugin.getIP(player);
        if ((PlayerCache.getInstance().isAuthenticated(name)) && (!player.isDead())) {
            if ((Settings.isSaveQuitLocationEnabled) && data.isAuthAvailable(name)) {
                final PlayerAuth auth = new PlayerAuth(name, loc.getX(), loc.getY(), loc.getZ(), loc.getWorld().getName());
                try {
                    data.updateQuitLoc(auth);
                } catch (NullPointerException npe) {
                }
            }
            PlayerAuth auth = new PlayerAuth(name, ip, System.currentTimeMillis());
            data.updateSession(auth);
        }

        if (data.getAuth(name) != null && !PlayerCache.getInstance().isAuthenticated(name) && Settings.enableProtection)
            event.setLeaveMessage(null);

        if (LimboCache.getInstance().hasLimboPlayer(name)) {
            LimboPlayer limbo = LimboCache.getInstance().getLimboPlayer(name);
            if (Settings.protectInventoryBeforeLogInEnabled) {
                try {
                    RestoreInventoryEvent ev = new RestoreInventoryEvent(player, limbo.getInventory(), limbo.getArmour());
                    plugin.getServer().getPluginManager().callEvent(ev);
                    if (!ev.isCancelled()) {
                        API.setPlayerInventory(player, ev.getInventory(), ev.getArmor());
                    }
                } catch (NullPointerException npe) {
                    ConsoleLogger.showError("Problem while restore " + name + " inventory after a kick");
                }
            }
            if (!Settings.noTeleport)
                try {
                    AuthMeTeleportEvent tpEvent = new AuthMeTeleportEvent(player, limbo.getLoc());
                    plugin.getServer().getPluginManager().callEvent(tpEvent);
                    if (!tpEvent.isCancelled()) {
                        if (player != null && player.isOnline() && tpEvent.getTo() != null) {
                        	if (tpEvent.getTo().getWorld() != null)
                            	player.teleport(tpEvent.getTo());
                        }
                    }
                } catch (NullPointerException npe) {
                }
            this.utils.addNormal(player, limbo.getGroup());
            player.setOp(limbo.getOperator());
            if (player.getGameMode() != GameMode.CREATIVE && !Settings.isMovementAllowed) {
                player.setAllowFlight(limbo.isFlying());
                player.setFlying(limbo.isFlying());
            }
            this.plugin.getServer().getScheduler().cancelTask(limbo.getTimeoutTaskId());
            this.plugin.getServer().getScheduler().cancelTask(limbo.getMessageTaskId());
            LimboCache.getInstance().deleteLimboPlayer(name);
            if (this.playerBackup.doesCacheExist(player)) {
                this.playerBackup.removeCache(player);
            }
        }
        PlayerCache.getInstance().removePlayer(name);
        data.setUnlogged(name);
        if (gameMode.containsKey(name))
            gameMode.remove(name);
        try {
            player.getVehicle().eject();
        } catch (NullPointerException ex) {
        }
        player.saveData();
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        if (event.isCancelled() || event.getPlayer() == null) {
            return;
        }

        Player player = event.getPlayer();
        String name = player.getName();

        if (Utils.getInstance().isUnrestricted(player)) {
            return;
        }

        if (plugin.getCitizensCommunicator().isNPC(player, plugin))
            return;

        if (PlayerCache.getInstance().isAuthenticated(player.getName())) {
            return;
        }

        if (!data.isAuthAvailable(name)) {
            if (!Settings.isForcedRegistrationEnabled) {
                return;
            }
        }

        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (event.isCancelled() || event.getPlayer() == null)
            return;

        Player player = event.getPlayer();
        String name = player.getName();

        if (Utils.getInstance().isUnrestricted(player)) {
            return;
        }

        if (plugin.getCitizensCommunicator().isNPC(player, plugin))
            return;

        if (PlayerCache.getInstance().isAuthenticated(player.getName())) {
            return;
        }

        if (!data.isAuthAvailable(name)) {
            if (!Settings.isForcedRegistrationEnabled) {
                return;
            }
        }
        if (event.getClickedBlock() != null && event.getClickedBlock().getType() != Material.AIR)
            event.setUseInteractedBlock(org.bukkit.event.Event.Result.DENY);
        event.setUseItemInHand(org.bukkit.event.Event.Result.DENY);
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInventoryOpen(InventoryOpenEvent event) {
        if (event.isCancelled() || event.getPlayer() == null)
            return;
        Player player = (Player) event.getPlayer();
        String name = player.getName();

        if (Utils.getInstance().isUnrestricted(player)) {
            return;
        }

        if (plugin.getCitizensCommunicator().isNPC(player, plugin))
            return;

        if (PlayerCache.getInstance().isAuthenticated(player.getName())) {
            return;
        }

        if (!data.isAuthAvailable(name)) {
            if (!Settings.isForcedRegistrationEnabled) {
                return;
            }
        }
        event.setCancelled(true);
        player.closeInventory();
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInventoryClick(InventoryClickEvent event) {
        if (event.isCancelled() || event.getWhoClicked() == null)
            return;
        if (!(event.getWhoClicked() instanceof Player))
            return;
        Player player = (Player) event.getWhoClicked();
        String name = player.getName();

        if (Utils.getInstance().isUnrestricted(player)) {
            return;
        }

        if (plugin.getCitizensCommunicator().isNPC(player, plugin))
            return;

        if (PlayerCache.getInstance().isAuthenticated(player.getName())) {
            return;
        }

        if (!data.isAuthAvailable(name)) {
            if (!Settings.isForcedRegistrationEnabled) {
                return;
            }
        }
        event.setResult(org.bukkit.event.Event.Result.DENY);
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (event.isCancelled() || event.getPlayer() == null) {
            return;
        }

        Player player = event.getPlayer();
        String name = player.getName();

        if (plugin.getCitizensCommunicator().isNPC(player, plugin) || Utils.getInstance().isUnrestricted(player) || CombatTagComunicator.isNPC(player)) {
            return;
        }

        if (PlayerCache.getInstance().isAuthenticated(player.getName())) {
            return;
        }

        if (!data.isAuthAvailable(name)) {
            if (!Settings.isForcedRegistrationEnabled) {
                return;
            }
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (event.isCancelled() || event.getPlayer() == null) {
            return;
        }
        Player player = event.getPlayer();
        String name = player.getName();

        if (Utils.getInstance().isUnrestricted(player) || CombatTagComunicator.isNPC(player)) {
            return;
        }

        if (plugin.getCitizensCommunicator().isNPC(player, plugin))
            return;

        if (PlayerCache.getInstance().isAuthenticated(player.getName())) {
            return;
        }

        if (!data.isAuthAvailable(name)) {
            if (!Settings.isForcedRegistrationEnabled) {
                return;
            }
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        if (event.isCancelled() || event.getPlayer() == null) {
            return;
        }
        Player player = event.getPlayer();
        String name = player.getName();

        if (Utils.getInstance().isUnrestricted(player)) {
            return;
        }

        if (PlayerCache.getInstance().isAuthenticated(player.getName())) {
            return;
        }

        if (!data.isAuthAvailable(name)) {
            if (!Settings.isForcedRegistrationEnabled) {
                return;
            }
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.LOWEST)
    public void onSignChange(SignChangeEvent event) {
        if (event.isCancelled() || event.getPlayer() == null || event == null) {
            return;
        }
        Player player = event.getPlayer();
        String name = player.getName();
        if (Utils.getInstance().isUnrestricted(player)) {
            return;
        }
        if (PlayerCache.getInstance().isAuthenticated(name)) {
            return;
        }
        if (!data.isAuthAvailable(name)) {
            if (!Settings.isForcedRegistrationEnabled) {
                return;
            }
        }
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (event.getPlayer() == null || event == null) {
            return;
        }

        Player player = event.getPlayer();
        String name = player.getName();

        if (Utils.getInstance().isUnrestricted(player) || CombatTagComunicator.isNPC(player))
            return;

        if (plugin.getCitizensCommunicator().isNPC(player, plugin))
            return;

        if (PlayerCache.getInstance().isAuthenticated(name))
            return;

        if (!data.isAuthAvailable(name))
            if (!Settings.isForcedRegistrationEnabled)
                return;

        Location spawn = plugin.getSpawnLocation(player);
        if (Settings.isSaveQuitLocationEnabled && data.isAuthAvailable(name)) {
            final PlayerAuth auth = new PlayerAuth(name, spawn.getX(), spawn.getY(), spawn.getZ(), spawn.getWorld().getName());
            try {
                data.updateQuitLoc(auth);
            } catch (NullPointerException npe) {
            }
        }
        event.setRespawnLocation(spawn);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
        if (event.isCancelled())
            return;
        if (event.getPlayer() == null || event == null)
            return;
        if (!Settings.isForceSurvivalModeEnabled)
            return;

        Player player = event.getPlayer();

        if (plugin.authmePermissible(player, "authme.bypassforcesurvival"))
            return;

        String name = player.getName();

        if (Utils.getInstance().isUnrestricted(player) || CombatTagComunicator.isNPC(player))
            return;

        if (plugin.getCitizensCommunicator().isNPC(player, plugin))
            return;

        if (PlayerCache.getInstance().isAuthenticated(name))
            return;

        if (!data.isAuthAvailable(name))
            if (!Settings.isForcedRegistrationEnabled)
                return;

        if (causeByAuthMe.containsKey(name) && causeByAuthMe.get(name))
            return;
        event.setCancelled(true);
    }
}
