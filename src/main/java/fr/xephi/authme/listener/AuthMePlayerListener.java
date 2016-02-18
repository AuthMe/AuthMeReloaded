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
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PlayerStatePermission;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.util.GeoLiteAPI;
import fr.xephi.authme.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;

import java.util.concurrent.ConcurrentHashMap;

import static fr.xephi.authme.listener.ListenerService.shouldCancelEvent;

/**
 * Listener class for player's events
 */
public class AuthMePlayerListener implements Listener {

    public static final ConcurrentHashMap<String, String> joinMessage = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, Boolean> causeByAuthMe = new ConcurrentHashMap<>();
    private final AuthMe plugin;
    private final Messages m;

    public AuthMePlayerListener(AuthMe plugin) {
        this.m = plugin.getMessages();
        this.plugin = plugin;
    }

    private void handleChat(AsyncPlayerChatEvent event) {
        if (Settings.isChatAllowed) {
            return;
        }

        final Player player = event.getPlayer();
        if (Utils.checkAuth(player)) {
            for (Player p : Utils.getOnlinePlayers()) {
                if (!PlayerCache.getInstance().isAuthenticated(p.getName())) {
                    event.getRecipients().remove(p); // TODO: it should be configurable
                }
            }
            return;
        }

        event.setCancelled(true);
        sendLoginRegisterMSG(player);
    }

    // TODO: new name
    private void sendLoginRegisterMSG(final Player player) {
        plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable() {
            @Override
            public void run() {
                if (plugin.getDataSource().isAuthAvailable(player.getName().toLowerCase())) {
                    m.send(player, MessageKey.LOGIN_MESSAGE);
                } else {
                    if (Settings.emailRegistration) {
                        m.send(player, MessageKey.REGISTER_EMAIL_MESSAGE);
                    } else {
                        m.send(player, MessageKey.REGISTER_MESSAGE);
                    }
                }
            }
        });
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String cmd = event.getMessage().split(" ")[0].toLowerCase();
        if (Settings.useEssentialsMotd && cmd.equals("/motd")) {
            return;
        }
        if (!Settings.isForcedRegistrationEnabled && Settings.allowAllCommandsIfRegIsOptional) {
            return;
        }
        if (Settings.allowCommands.contains(cmd)) {
            return;
        }
        if (Utils.checkAuth(event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
        sendLoginRegisterMSG(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.NORMAL)
    public void onPlayerNormalChat(AsyncPlayerChatEvent event) {
        handleChat(event);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGH)
    public void onPlayerHighChat(AsyncPlayerChatEvent event) {
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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
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
            if (spawn.distance(player.getLocation()) > Settings.getMovementRadius) {
                player.teleport(spawn);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onJoinMessage(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        if (Settings.removeJoinMessage) {
            event.setJoinMessage(null);
            return;
        }
        if (!Settings.delayJoinMessage) {
            return;
        }

        String name = player.getName().toLowerCase();
        String joinMsg = event.getJoinMessage();

        // Remove the join message while the player isn't logging in
        if (joinMsg == null) {
            return;
        }
        event.setJoinMessage(null);
        joinMessage.put(name, joinMsg);
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        if (Settings.isForceSurvivalModeEnabled
            && !player.hasPermission(PlayerStatePermission.BYPASS_FORCE_SURVIVAL.getNode())) {
            player.setGameMode(GameMode.SURVIVAL);
        }

        // Shedule login task so works after the prelogin
        // (Fix found by Koolaid5000)
        Bukkit.getScheduler().runTask(plugin, new Runnable() {
            @Override
            public void run() {
                plugin.getManagement().performJoin(player);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        PlayerAuth auth = plugin.getDataSource().getAuth(event.getName());
        if (Settings.preventOtherCase && auth != null && auth.getRealName() != null) {
            String realName = auth.getRealName();
            if (!realName.isEmpty() && !realName.equals("Player") && !realName.equals(event.getName())) {
                event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
                // TODO: Add a message like : MessageKey.INVALID_NAME_CASE
                event.setKickMessage("You should join using username: " + ChatColor.AQUA + realName +
                    ChatColor.RESET + "\nnot: " + ChatColor.RED + event.getName());
                return;
            }
            if (realName.isEmpty() || realName.equals("Player")) {
                auth.setRealName(event.getName());
                plugin.getDataSource().saveAuth(auth);
            }
        }

        if (auth == null) {
            if (!Settings.countriesBlacklist.isEmpty() || !Settings.countries.isEmpty()) {
                String playerIP = event.getAddress().getHostAddress();
                String countryCode = GeoLiteAPI.getCountryCode(playerIP);
                if (Settings.countriesBlacklist.contains(countryCode)) {
                    event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
                    event.setKickMessage(m.retrieveSingle(MessageKey.COUNTRY_BANNED_ERROR));
                    return;
                }
                if (Settings.enableProtection && !Settings.countries.contains(countryCode)) {
                    event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
                    event.setKickMessage(m.retrieveSingle(MessageKey.COUNTRY_BANNED_ERROR));
                    return;
                }
            }
        }

        final String name = event.getName().toLowerCase();
        final Player player = Utils.getPlayer(name);
        // Check if forceSingleSession is set to true, so kick player that has
        // joined with same nick of online player
        if (player != null && Settings.isForceSingleSessionEnabled) {
            event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
            event.setKickMessage(m.retrieveSingle(MessageKey.USERNAME_ALREADY_ONLINE_ERROR));
            LimboPlayer limbo = LimboCache.getInstance().getLimboPlayer(name);
            if (limbo != null && PlayerCache.getInstance().isAuthenticated(name)) {
                Utils.addNormal(player, limbo.getGroup());
                LimboCache.getInstance().deleteLimboPlayer(name);
            }
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerLogin(PlayerLoginEvent event) {
        final Player player = event.getPlayer();
        if (player == null || Utils.isUnrestricted(player)) {
            return;
        }

        // Get the permissions manager
        PermissionsManager permsMan = plugin.getPermissionsManager();

        if (event.getResult() == PlayerLoginEvent.Result.KICK_FULL) {
            if (permsMan.hasPermission(player, PlayerStatePermission.IS_VIP)) {
                int playersOnline = Utils.getOnlinePlayers().size();
                if (playersOnline > plugin.getServer().getMaxPlayers()) {
                    event.allow();
                } else {
                    Player pl = plugin.generateKickPlayer(Utils.getOnlinePlayers());
                    if (pl != null) {
                        pl.kickPlayer(m.retrieveSingle(MessageKey.KICK_FOR_VIP));
                        event.allow();
                    } else {
                        ConsoleLogger.info("The player " + event.getPlayer().getName() + " tried to join, but the server was full");
                        event.setKickMessage(m.retrieveSingle(MessageKey.KICK_FULL_SERVER));
                        event.setResult(PlayerLoginEvent.Result.KICK_FULL);
                    }
                }
            } else {
                event.setKickMessage(m.retrieveSingle(MessageKey.KICK_FULL_SERVER));
                event.setResult(PlayerLoginEvent.Result.KICK_FULL);
                return;
            }
        }

        if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            return;
        }

        final String name = player.getName().toLowerCase();
        boolean isAuthAvailable = plugin.getDataSource().isAuthAvailable(name);

        if (Settings.isKickNonRegisteredEnabled && !isAuthAvailable) {
            if (Settings.antiBotInAction) {
                event.setKickMessage(m.retrieveSingle(MessageKey.KICK_ANTIBOT));
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (player == null) {
            return;
        }

        if (Settings.removeLeaveMessage) {
            event.setQuitMessage(null);
        }

        plugin.getManagement().performQuit(player, false);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerKick(PlayerKickEvent event) {
        Player player = event.getPlayer();

        if (player == null) {
            return;
        }

        if ((!Settings.isForceSingleSessionEnabled)
            && (event.getReason().equals(m.retrieveSingle(MessageKey.USERNAME_ALREADY_ONLINE_ERROR)))) {
            event.setCancelled(true);
            return;
        }

        plugin.getManagement().performQuit(player, true);
    }

    /*
     * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
     * Note #360: npc status can be used to bypass security!!!
     * <<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<<
     */

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        if (shouldCancelEvent(event)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (shouldCancelEvent(event)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerConsumeItem(PlayerItemConsumeEvent event) {
        if (shouldCancelEvent(event)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerInventoryOpen(InventoryOpenEvent event) {
        final Player player = (Player) event.getPlayer();

        if (!ListenerService.shouldCancelEvent(player)) {
            return;
        }
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
        if (Utils.isNPC((Player) event.getWhoClicked()))
            return;
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerHitPlayerEvent(EntityDamageByEntityEvent event) {
        if (ListenerService.shouldCancelEvent(event)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (shouldCancelEvent(event)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (shouldCancelEvent(event)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        if (shouldCancelEvent(event)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        if (ListenerService.shouldCancelEvent(player)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (!shouldCancelEvent(event)) {
            return;
        }

        Player player = event.getPlayer();
        String name = player.getName().toLowerCase();
        Location spawn = plugin.getSpawnLocation(player);
        if (Settings.isSaveQuitLocationEnabled && plugin.getDataSource().isAuthAvailable(name)) {
            PlayerAuth auth = PlayerAuth.builder()
                .name(name)
                .realName(player.getName())
                .location(spawn)
                .build();
            plugin.getDataSource().updateQuitLoc(auth);
        }
        if (spawn != null && spawn.getWorld() != null) {
            event.setRespawnLocation(spawn);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerShear(PlayerShearEntityEvent event) {
        if (shouldCancelEvent(event)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerFish(PlayerFishEvent event) {
        if (shouldCancelEvent(event)) {
            event.setCancelled(true);
        }
    }
}
