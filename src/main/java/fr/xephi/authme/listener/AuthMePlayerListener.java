package fr.xephi.authme.listener;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.Utils;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;
import org.bukkit.Bukkit;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.PatternSyntaxException;

public class AuthMePlayerListener implements Listener {

    public static ConcurrentHashMap<String, GameMode> gameMode = new ConcurrentHashMap<>();
    public static ConcurrentHashMap<String, String> joinMessage = new ConcurrentHashMap<>();
    private Messages m = Messages.getInstance();
    public AuthMe plugin;
    public static ConcurrentHashMap<String, Boolean> causeByAuthMe = new ConcurrentHashMap<>();
    private List<String> antibot = new ArrayList<>();

    public AuthMePlayerListener(AuthMe plugin) {
        this.plugin = plugin;
    }

    private void handleChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        if (!Utils.checkAuth(player)) {
            String cmd = event.getMessage().split(" ")[0];
            if (!Settings.isChatAllowed && !(Settings.allowCommands.contains(cmd))) {
                event.setCancelled(true);
            }
            if (plugin.database.isAuthAvailable(player.getName().toLowerCase())) {
                m.send(player, "login_msg");
            } else {
                if (Settings.emailRegistration) {
                    m.send(player, "reg_email_msg");
                } else {
                    m.send(player, "reg_msg");
                }
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        if (Utils.checkAuth(event.getPlayer()))
            return;

        String msg = event.getMessage();
        if (msg.equalsIgnoreCase("/worldedit cui"))
            return;

        String cmd = msg.split(" ")[0];
        if (cmd.equalsIgnoreCase("/login") || cmd.equalsIgnoreCase("/register") || cmd.equalsIgnoreCase("/l") || cmd.equalsIgnoreCase("/reg") || cmd.equalsIgnoreCase("/email") || cmd.equalsIgnoreCase("/captcha"))
            return;
        if (Settings.useEssentialsMotd && cmd.equalsIgnoreCase("/motd"))
            return;
        if (Settings.allowCommands.contains(cmd))
            return;

        event.setMessage("/notloggedin");
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onPlayerNormalChat(AsyncPlayerChatEvent event) {
        handleChat(event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlayerHighChat(AsyncPlayerChatEvent event) {
        handleChat(event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        handleChat(event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerHighestChat(AsyncPlayerChatEvent event) {
        handleChat(event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerEarlyChat(AsyncPlayerChatEvent event) {
        handleChat(event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onPlayerLowChat(AsyncPlayerChatEvent event) {
        handleChat(event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        Player player = event.getPlayer();
        if (Utils.checkAuth(player))
            return;

        if (!Settings.isMovementAllowed) {
            if (!event.getFrom().getBlock().equals(event.getTo().getBlock()))
                event.setTo(event.getFrom());
            return;
        }

        if (Settings.getMovementRadius == 0) {
            return;
        }

        int radius = Settings.getMovementRadius;
        Location spawn = plugin.getSpawnLocation(player);

        if (spawn != null && spawn.getWorld() != null) {
            if (!event.getPlayer().getWorld().equals(spawn.getWorld())) {
                event.getPlayer().teleport(spawn);
                return;
            }
            if ((spawn.distance(player.getLocation()) > radius)) {
                event.getPlayer().teleport(spawn);
            }
        }
    }

    private void checkAntiBotMod(final Player player) {
        if (plugin.delayedAntiBot || plugin.antibotMod)
            return;
        if (plugin.authmePermissible(player, "authme.bypassantibot"))
            return;
        if (antibot.size() > Settings.antiBotSensibility) {
            plugin.switchAntiBotMod(true);
            for (String s : m.send("antibot_auto_enabled"))
                Bukkit.broadcastMessage(s);
            Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {

                @Override
                public void run() {
                    if (plugin.antibotMod) {
                        plugin.switchAntiBotMod(false);
                        antibot.clear();
                        for (String s : m.send("antibot_auto_disabled"))
                            Bukkit.broadcastMessage(s.replace("%m", "" + Settings.antiBotDuration));
                    }
                }
            }, Settings.antiBotDuration * 1200);
            return;
        }
        antibot.add(player.getName().toLowerCase());
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {

            @Override
            public void run() {
                antibot.remove(player.getName().toLowerCase());
            }
        }, 300);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(final PlayerJoinEvent event) {
        if (event.getPlayer() == null) {
            return;
        }

        // Shedule login task so works after the prelogin
        // (Fix found by Koolaid5000)
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                Player player = event.getPlayer();
                String name = player.getName().toLowerCase();

                plugin.management.performJoin(player);

                // Remove the join message while the player isn't logging in
                if ((Settings.enableProtection || Settings.delayJoinMessage) && event.getJoinMessage() != null) {
                    joinMessage.put(name, event.getJoinMessage());
                    event.setJoinMessage(null);
                }
            }
        });
    }

    @SuppressWarnings("deprecation")
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        final String name = event.getName().toLowerCase();
        final Player player = Bukkit.getServer().getPlayer(name);

        if (player == null)
            return;

        // Check if forceSingleSession is set to true, so kick player that has
        // joined with same nick of online player
        if (plugin.dataManager.isOnline(player, name) && Settings.isForceSingleSessionEnabled) {
            event.setKickMessage(m.send("same_nick")[0]);
            event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
            if (LimboCache.getInstance().hasLimboPlayer(name))
                Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {

                    @Override
                    public void run() {
                        LimboPlayer limbo = LimboCache.getInstance().getLimboPlayer(player.getName().toLowerCase());
                        if (limbo != null && PlayerCache.getInstance().isAuthenticated(player.getName().toLowerCase())) {
                            Utils.addNormal(player, limbo.getGroup());
                            LimboCache.getInstance().deleteLimboPlayer(player.getName().toLowerCase());
                        }
                    }

                });
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        final Player player = event.getPlayer();
        if (player == null)
            return;
        final String name = player.getName().toLowerCase();
        boolean isAuthAvailable = plugin.database.isAuthAvailable(name);

        if (Utils.isNPC(player) || Utils.isUnrestricted(player)) {
            return;
        }

        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED)
            return;

        if (!Settings.countriesBlacklist.isEmpty()) {
            String code = Utils.getCountryCode(event.getAddress().getHostAddress());
            if (((code == null) || (Settings.countriesBlacklist.contains(code) && !isAuthAvailable)) && !plugin.authmePermissible(player, "authme.bypassantibot")) {
                event.setKickMessage(m.send("country_banned")[0]);
                event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
                return;
            }
        }
        if (Settings.enableProtection && !Settings.countries.isEmpty()) {
            String code = Utils.getCountryCode(event.getAddress().getHostAddress());
            if (((code == null) || (!Settings.countries.contains(code) && !isAuthAvailable)) && !plugin.authmePermissible(player, "authme.bypassantibot")) {
                event.setKickMessage(m.send("country_banned")[0]);
                event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
                return;
            }
        }

        if (Settings.isKickNonRegisteredEnabled && !Settings.antiBotInAction) {
            if (!isAuthAvailable) {
                event.setKickMessage(m.send("reg_only")[0]);
                event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
                return;
            }
        }

        if (Settings.antiBotInAction) {
            if (!isAuthAvailable) {
                event.setKickMessage("AntiBot service in action! You actually need to be registered!");
                event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
                return;
            }
        }

        if (isAuthAvailable && plugin.database.getType() != DataSource.DataSourceType.FILE) {
            PlayerAuth auth = plugin.database.getAuth(name);
            if (auth.getRealName() != null && !auth.getRealName().isEmpty() && !auth.getRealName().equalsIgnoreCase("Player") && !auth.getRealName().equals(player.getName())) {
                event.setKickMessage(m.send("same_nick")[0]);
                event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
                if (Settings.banUnsafeIp)
                    plugin.getServer().banIP(player.getAddress().getAddress().getHostAddress());
                return;
            }
        }

        int min = Settings.getMinNickLength;
        int max = Settings.getMaxNickLength;
        String regex = Settings.getNickRegex;

        if (name.length() > max || name.length() < min) {
            event.setKickMessage(Arrays.toString(m.send("name_len")));
            event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
            return;
        }
        try {
            if (!player.getName().matches(regex) || name.equalsIgnoreCase("Player")) {
                try {
                    event.setKickMessage(m.send("regex")[0].replace("REG_EX", regex));
                    event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
                } catch (Exception exc) {
                    event.setKickMessage("allowed char : " + regex);
                    event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
                }
                return;
            }
        } catch (PatternSyntaxException pse) {
            if (regex == null || regex.isEmpty()) {
                event.setKickMessage("Your nickname do not match");
                event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
                return;
            }
            try {
                event.setKickMessage(m.send("regex")[0].replace("REG_EX", regex));
                event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
            } catch (Exception exc) {
                event.setKickMessage("allowed char : " + regex);
                event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
            }
            return;
        }

        if (event.getResult() == PlayerLoginEvent.Result.ALLOWED) {
            checkAntiBotMod(player);
            if (Settings.bungee) {
                ByteArrayDataOutput out = ByteStreams.newDataOutput();
                out.writeUTF("IP");
                player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
            }
            return;
        }
        if (event.getResult() != PlayerLoginEvent.Result.KICK_FULL)
            return;
        if (!plugin.authmePermissible(player, "authme.vip")) {
            event.setKickMessage(m.send("kick_fullserver")[0]);
            event.setResult(PlayerLoginEvent.Result.KICK_FULL);
            return;
        }

        int playersOnline = Utils.getOnlinePlayers().size();
        if (playersOnline > plugin.getServer().getMaxPlayers()) {
            event.allow();
        } else {
            final Player pl = plugin.generateKickPlayer(Utils.getOnlinePlayers());
            if (pl != null) {
                pl.kickPlayer(m.send("kick_forvip")[0]);
                event.allow();
            } else {
                ConsoleLogger.info("The player " + player.getName() + " tryed to join, but the server was full");
                event.setKickMessage(m.send("kick_fullserver")[0]);
                event.setResult(PlayerLoginEvent.Result.KICK_FULL);
            }
        }
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (event.getPlayer() == null) {
            return;
        }

        Player player = event.getPlayer();
        String name = player.getName().toLowerCase();

        plugin.management.performQuit(player, false);

        if (plugin.database.isAuthAvailable(name) && !PlayerCache.getInstance().isAuthenticated(name) && Settings.enableProtection)
            event.setQuitMessage(null);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerKick(PlayerKickEvent event) {
        if (event.getPlayer() == null) {
            return;
        }

        if ((!Settings.isForceSingleSessionEnabled) && (event.getReason().contains(m.getString("same_nick")))) {
            event.setCancelled(true);
            return;
        }

        Player player = event.getPlayer();
        plugin.management.performQuit(player, true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        if (Utils.checkAuth(event.getPlayer()))
            return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (player == null || Utils.checkAuth(player))
            return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onPlayerConsumeItem(PlayerItemConsumeEvent event) {
        if (Utils.checkAuth(event.getPlayer()))
            return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerInventoryOpen(InventoryOpenEvent event) {
        final Player player = (Player) event.getPlayer();
        if (Utils.checkAuth(player))
            return;
        event.setCancelled(true);

        /*
         * @note little hack cause InventoryOpenEvent cannot be cancelled for
         * real, cause no packet is send to server by client for the main inv
         */
        Bukkit.getScheduler().scheduleSyncDelayedTask(plugin, new Runnable() {

            @Override
            public void run() {
                player.closeInventory();
            }

        }, 1);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerInventoryClick(InventoryClickEvent event) {
        if (event.getWhoClicked() == null)
            return;
        if (!(event.getWhoClicked() instanceof Player))
            return;
        if (Utils.checkAuth((Player) event.getWhoClicked()))
            return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void playerHitPlayerEvent(EntityDamageByEntityEvent event) {
        Entity damager = event.getDamager();
        if (!(damager instanceof Player)) {
            return;
        }
        if (Utils.checkAuth((Player) damager))
            return;

        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (player == null || Utils.checkAuth(player))
            return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        Player player = event.getPlayer();
        if (player == null || Utils.checkAuth(player))
            return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (Utils.checkAuth(event.getPlayer()))
            return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        if (Utils.checkAuth(event.getPlayer()))
            return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onSignChange(SignChangeEvent event) {
        if (Utils.checkAuth(event.getPlayer()))
            return;
        event.setCancelled(true);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        Player player = event.getPlayer();
        if (player == null || Utils.checkAuth(player))
            return;
        String name = player.getName().toLowerCase();
        Location spawn = plugin.getSpawnLocation(player);
        if (Settings.isSaveQuitLocationEnabled && plugin.database.isAuthAvailable(name)) {
            final PlayerAuth auth = new PlayerAuth(name, spawn.getX(), spawn.getY(), spawn.getZ(), spawn.getWorld().getName(), player.getName());
            plugin.database.updateQuitLoc(auth);
        }
        if (spawn != null && spawn.getWorld() != null) {
            event.setRespawnLocation(spawn);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        if (player == null)
            return;
        if (plugin.authmePermissible(player, "authme.bypassforcesurvival"))
            return;
        if (Utils.checkAuth(player))
            return;

        String name = player.getName().toLowerCase();
        if (causeByAuthMe.containsKey(name)) {
            causeByAuthMe.remove(name);
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onPlayerShear(PlayerShearEntityEvent event) {
        Player player = event.getPlayer();
        if (player == null || Utils.checkAuth(player))
            return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        if (player == null || Utils.checkAuth(player))
            return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onPlayerEditBook(PlayerEditBookEvent event) {
        Player player = event.getPlayer();
        if (player == null || Utils.checkAuth(player))
            return;
        event.setCancelled(true);
    }
}
