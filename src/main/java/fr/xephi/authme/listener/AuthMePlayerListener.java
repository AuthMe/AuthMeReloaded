package fr.xephi.authme.listener;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import fr.xephi.authme.AntiBot;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.settings.MessageKey;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.util.GeoLiteAPI;
import fr.xephi.authme.util.Utils;
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

import java.util.concurrent.ConcurrentHashMap;

/**
 */
public class AuthMePlayerListener implements Listener {

    public static final ConcurrentHashMap<String, GameMode> gameMode = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, String> joinMessage = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, Boolean> causeByAuthMe = new ConcurrentHashMap<>();
    public final AuthMe plugin;
    private final Messages m;

    /**
     * Constructor for AuthMePlayerListener.
     *
     * @param plugin AuthMe
     */
    public AuthMePlayerListener(AuthMe plugin) {
        this.m = plugin.getMessages();
        this.plugin = plugin;
    }

    /**
     * Method handleChat.
     *
     * @param event AsyncPlayerChatEvent
     */
    private void handleChat(AsyncPlayerChatEvent event) {
        Player player = event.getPlayer();
        for (Player p : Utils.getOnlinePlayers()) {
            if (!PlayerCache.getInstance().isAuthenticated(p.getName())) {
                event.getRecipients().remove(p); // TODO: it should be configurable
            }
        }

        if (Settings.isChatAllowed) {
            return;
        }

        if (Utils.checkAuth(player)) {
            return;
        }

        event.setCancelled(true);
        if (plugin.database.isAuthAvailable(player.getName().toLowerCase())) {
            m.send(player, MessageKey.LOGIN_MESSAGE);
        } else {
            if (Settings.emailRegistration) {
                m.send(player, MessageKey.REGISTER_EMAIL_MESSAGE);
            } else {
                m.send(player, MessageKey.REGISTER_MESSAGE);
            }
        }

    }

    /**
     * Method onPlayerCommandPreprocess.
     *
     * @param event PlayerCommandPreprocessEvent
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String cmd = event.getMessage().split(" ")[0].toLowerCase();
        if (Settings.useEssentialsMotd && cmd.equals("/motd")) {
            return;
        }
        if (Settings.allowCommands.contains(cmd)) {
            return;
        }
        if (Utils.checkAuth(event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
    }

    /**
     * Method onPlayerNormalChat.
     *
     * @param event AsyncPlayerChatEvent
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onPlayerNormalChat(AsyncPlayerChatEvent event) {
        handleChat(event);
    }

    /**
     * Method onPlayerHighChat.
     *
     * @param event AsyncPlayerChatEvent
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlayerHighChat(AsyncPlayerChatEvent event) {
        handleChat(event);
    }

    /**
     * Method onPlayerChat.
     *
     * @param event AsyncPlayerChatEvent
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        handleChat(event);
    }

    /**
     * Method onPlayerHighestChat.
     *
     * @param event AsyncPlayerChatEvent
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerHighestChat(AsyncPlayerChatEvent event) {
        handleChat(event);
    }

    /**
     * Method onPlayerEarlyChat.
     *
     * @param event AsyncPlayerChatEvent
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerEarlyChat(AsyncPlayerChatEvent event) {
        handleChat(event);
    }

    /**
     * Method onPlayerLowChat.
     *
     * @param event AsyncPlayerChatEvent
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOW)
    public void onPlayerLowChat(AsyncPlayerChatEvent event) {
        handleChat(event);
    }

    /**
     * Method onPlayerMove.
     *
     * @param event PlayerMoveEvent
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (Settings.isMovementAllowed && Settings.getMovementRadius <= 0) {
            return;
        }

        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
            && event.getFrom().getBlockY() == event.getTo().getBlockY()
            && event.getFrom().getBlockZ() == event.getTo().getBlockZ()) {
            return;
        }

        Player player = event.getPlayer();
        if (Utils.checkAuth(player)) {
            return;
        }

        if (!Settings.isMovementAllowed) {
            event.setTo(event.getFrom());
            if (Settings.isRemoveSpeedEnabled) {
                player.setFlySpeed(0.0f);
                player.setWalkSpeed(0.0f);
            }
            return;
        }

        if (Settings.noTeleport) {
            return;
        }

        Location spawn = plugin.getSpawnLocation(player);
        if (spawn != null && spawn.getWorld() != null) {
            if (!player.getWorld().equals(spawn.getWorld())) {
                player.teleport(spawn);
                return;
            }
            if ((spawn.distance(player.getLocation()) > Settings.getMovementRadius)) {
                player.teleport(spawn);
            }
        }
    }

    /**
     * Method onPlayerJoin.
     *
     * @param event PlayerJoinEvent
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerJoin(PlayerJoinEvent event) {
        if (event.getPlayer() == null || Utils.isNPC(event.getPlayer())) {
            return;
        }

        final Player player = event.getPlayer();
        final String name = player.getName().toLowerCase();
        final String joinMsg = event.getJoinMessage();
        final boolean delay = Settings.delayJoinLeaveMessages && joinMsg != null;

        // Remove the join message while the player isn't logging in
        if (delay) {
            event.setJoinMessage(null);
        }

        // Shedule login task so works after the prelogin
        // (Fix found by Koolaid5000)
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                if (delay) {
                    joinMessage.put(name, joinMsg);
                }
                plugin.getManagement().performJoin(player);
            }
        });
    }

    /**
     * Method onPreLogin.
     *
     * @param event AsyncPlayerPreLoginEvent
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        final String name = event.getName().toLowerCase();
        final Player player = Utils.getPlayer(name);
        if (player == null || Utils.isNPC(player)) {
            return;
        }

        // Check if forceSingleSession is set to true, so kick player that has
        // joined with same nick of online player
        if (Settings.isForceSingleSessionEnabled && player.isOnline()) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, m.getString("same_nick"));
            if (LimboCache.getInstance().hasLimboPlayer(name))
                Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {

                    @Override
                    public void run() {
                        LimboPlayer limbo = LimboCache.getInstance().getLimboPlayer(name);
                        if (limbo != null && PlayerCache.getInstance().isAuthenticated(name)) {
                            Utils.addNormal(player, limbo.getGroup());
                            LimboCache.getInstance().deleteLimboPlayer(name);
                        }
                    }
                });
        }
    }

    /**
     * Method onPlayerLogin.
     *
     * @param event PlayerLoginEvent
     */
    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        if (event.getPlayer() == null) {
            return;
        }

        if (event.getResult() == PlayerLoginEvent.Result.KICK_FULL) {
            int playersOnline = Utils.getOnlinePlayers().size();
            if (playersOnline > plugin.getServer().getMaxPlayers()) {
                event.allow();
            } else {
                Player pl = plugin.generateKickPlayer(Utils.getOnlinePlayers());
                if (pl != null) {
                    pl.kickPlayer(m.retrieveSingle(MessageKey.KICK_FOR_VIP));
                    event.allow();
                } else {
                    ConsoleLogger.info("The player " + event.getPlayer().getName() + " tryed to join, but the server was full");
                    event.setKickMessage(m.retrieveSingle(MessageKey.KICK_FULL_SERVER));
                    event.setResult(PlayerLoginEvent.Result.KICK_FULL);
                }
            }
        }

        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            return;
        }

        // Get the permissions manager
        PermissionsManager permsMan = plugin.getPermissionsManager();

        final Player player = event.getPlayer();
        if (event.getResult() == PlayerLoginEvent.Result.KICK_FULL && !permsMan.hasPermission(player, "authme.vip")) {
            event.setKickMessage(m.retrieveSingle(MessageKey.KICK_FULL_SERVER));
            event.setResult(PlayerLoginEvent.Result.KICK_FULL);
            return;
        }

        if (Utils.isNPC(player) || Utils.isUnrestricted(player)) {
            return;
        }

        final String name = player.getName().toLowerCase();
        boolean isAuthAvailable = plugin.database.isAuthAvailable(name);

        if (!Settings.countriesBlacklist.isEmpty() && !isAuthAvailable && !permsMan.hasPermission(player, "authme.bypassantibot")) {
            String code = GeoLiteAPI.getCountryCode(event.getAddress().getHostAddress());
            if (Settings.countriesBlacklist.contains(code)) {
                event.setKickMessage(m.retrieveSingle(MessageKey.COUNTRY_BANNED_ERROR));
                event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
                return;
            }
        }

        if (Settings.enableProtection && !Settings.countries.isEmpty() && !isAuthAvailable && !permsMan.hasPermission(player, "authme.bypassantibot")) {
            String code = GeoLiteAPI.getCountryCode(event.getAddress().getHostAddress());
            if (!Settings.countries.contains(code)) {
                event.setKickMessage(m.retrieveSingle(MessageKey.COUNTRY_BANNED_ERROR));
                event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
                return;
            }
        }

        // TODO: Add message to the messages file!!!
        if (Settings.isKickNonRegisteredEnabled && !isAuthAvailable) {
            if (Settings.antiBotInAction) {
                event.setKickMessage("AntiBot service in action! You actually need to be registered!");
                event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
                return;
            } else {
                event.setKickMessage(m.retrieveSingle(MessageKey.MUST_REGISTER_MESSAGE));
                event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
                return;
            }
        }

        if (name.length() > Settings.getMaxNickLength || name.length() < Settings.getMinNickLength) {
            event.setKickMessage(m.retrieveSingle(MessageKey.INVALID_NAME_LENGTH));
            event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
            return;
        }

        if (!Settings.nickPattern.matcher(player.getName()).matches() || name.equalsIgnoreCase("Player")) {
            event.setKickMessage(m.retrieveSingle(MessageKey.INVALID_NAME_CHARACTERS).replace("REG_EX", Settings.getNickRegex));
            event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
            return;
        }

        AntiBot.checkAntiBot(player);

        if (Settings.bungee) {
            ByteArrayDataOutput out = ByteStreams.newDataOutput();
            out.writeUTF("IP");
            player.sendPluginMessage(plugin, "BungeeCord", out.toByteArray());
        }
    }

    /**
     * Method onPlayerQuit.
     *
     * @param event PlayerQuitEvent
     */
    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        if (event.getPlayer() == null) {
            return;
        }

        Player player = event.getPlayer();

        if (!Utils.checkAuth(player) && Settings.delayJoinLeaveMessages) {
            event.setQuitMessage(null);
        }

        plugin.management.performQuit(player, false);
    }

    /**
     * Method onPlayerKick.
     *
     * @param event PlayerKickEvent
     */
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

    /**
     * Method onPlayerPickupItem.
     *
     * @param event PlayerPickupItemEvent
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        if (Utils.checkAuth(event.getPlayer()))
            return;
        event.setCancelled(true);
    }

    /**
     * Method onPlayerInteract.
     *
     * @param event PlayerInteractEvent
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        Player player = event.getPlayer();
        if (player == null || Utils.checkAuth(player))
            return;
        event.setCancelled(true);
    }

    /**
     * Method onPlayerConsumeItem.
     *
     * @param event PlayerItemConsumeEvent
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onPlayerConsumeItem(PlayerItemConsumeEvent event) {
        if (Utils.checkAuth(event.getPlayer()))
            return;
        event.setCancelled(true);
    }

    /**
     * Method onPlayerInventoryOpen.
     *
     * @param event InventoryOpenEvent
     */
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

    /**
     * Method onPlayerInventoryClick.
     *
     * @param event InventoryClickEvent
     */
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

    /**
     * Method playerHitPlayerEvent.
     *
     * @param event EntityDamageByEntityEvent
     */
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

    /**
     * Method onPlayerInteractEntity.
     *
     * @param event PlayerInteractEntityEvent
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        Player player = event.getPlayer();
        if (player == null || Utils.checkAuth(player))
            return;
        event.setCancelled(true);
    }

    /**
     * Method onPlayerDropItem.
     *
     * @param event PlayerDropItemEvent
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (Utils.checkAuth(event.getPlayer()))
            return;
        event.setCancelled(true);
    }

    /**
     * Method onPlayerBedEnter.
     *
     * @param event PlayerBedEnterEvent
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        if (Utils.checkAuth(event.getPlayer()))
            return;
        event.setCancelled(true);
    }

    /**
     * Method onSignChange.
     *
     * @param event SignChangeEvent
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onSignChange(SignChangeEvent event) {
        if (Utils.checkAuth(event.getPlayer()))
            return;
        event.setCancelled(true);
    }

    /**
     * Method onPlayerRespawn.
     *
     * @param event PlayerRespawnEvent
     */
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

    /**
     * Method onPlayerGameModeChange.
     *
     * @param event PlayerGameModeChangeEvent
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerGameModeChange(PlayerGameModeChangeEvent event) {
        Player player = event.getPlayer();
        if (player == null)
            return;
        if (plugin.getPermissionsManager().hasPermission(player, "authme.bypassforcesurvival"))
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

    /**
     * Method onPlayerShear.
     *
     * @param event PlayerShearEntityEvent
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onPlayerShear(PlayerShearEntityEvent event) {
        Player player = event.getPlayer();
        if (player == null || Utils.checkAuth(player))
            return;
        event.setCancelled(true);
    }

    /**
     * Method onPlayerFish.
     *
     * @param event PlayerFishEvent
     */
    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onPlayerFish(PlayerFishEvent event) {
        Player player = event.getPlayer();
        if (player == null || Utils.checkAuth(player))
            return;
        event.setCancelled(true);
    }

}
