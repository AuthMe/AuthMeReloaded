package fr.xephi.authme.listener;

import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import fr.xephi.authme.AntiBot;
import fr.xephi.authme.AntiBot.AntiBotStatus;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PlayerStatePermission;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.SpawnLoader;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.settings.properties.ProtectionSettings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.util.BukkitService;
import fr.xephi.authme.util.Utils;
import fr.xephi.authme.util.ValidationService;
import org.bukkit.Bukkit;
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

import javax.inject.Inject;
import java.util.concurrent.ConcurrentHashMap;

import static fr.xephi.authme.listener.ListenerService.shouldCancelEvent;
import static fr.xephi.authme.settings.properties.RestrictionSettings.ALLOWED_MOVEMENT_RADIUS;
import static fr.xephi.authme.settings.properties.RestrictionSettings.ALLOW_ALL_COMMANDS_IF_REGISTRATION_IS_OPTIONAL;
import static fr.xephi.authme.settings.properties.RestrictionSettings.ALLOW_UNAUTHED_MOVEMENT;

/**
 * Listener class for player events.
 */
public class AuthMePlayerListener implements Listener {

    public static final ConcurrentHashMap<String, String> joinMessage = new ConcurrentHashMap<>();
    public static final ConcurrentHashMap<String, Boolean> causeByAuthMe = new ConcurrentHashMap<>();
    @Inject
    private AuthMe plugin;
    @Inject
    private NewSetting settings;
    @Inject
    private Messages m;
    @Inject
    private DataSource dataSource;
    @Inject
    private AntiBot antiBot;
    @Inject
    private Management management;
    @Inject
    private BukkitService bukkitService;
    @Inject
    private SpawnLoader spawnLoader;
    @Inject
    private ValidationService validationService;

    private void handleChat(AsyncPlayerChatEvent event) {
        if (settings.getProperty(RestrictionSettings.ALLOW_CHAT)) {
            return;
        }

        final Player player = event.getPlayer();
        if (shouldCancelEvent(player)) {
            event.setCancelled(true);
            sendLoginOrRegisterMessage(player);
        } else if (settings.getProperty(RestrictionSettings.HIDE_CHAT)) {
            for (Player p : bukkitService.getOnlinePlayers()) {
                if (!PlayerCache.getInstance().isAuthenticated(p.getName())) {
                    event.getRecipients().remove(p);
                }
            }
        }
    }

    private void sendLoginOrRegisterMessage(final Player player) {
        bukkitService.runTaskAsynchronously(new Runnable() {
            @Override
            public void run() {
                if (dataSource.isAuthAvailable(player.getName().toLowerCase())) {
                    m.send(player, MessageKey.LOGIN_MESSAGE);
                } else {
                    if (settings.getProperty(RegistrationSettings.USE_EMAIL_REGISTRATION)) {
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
        if (settings.getProperty(HooksSettings.USE_ESSENTIALS_MOTD) && "/motd".equals(cmd)) {
            return;
        }
        if (!settings.getProperty(RegistrationSettings.FORCE)
            && settings.getProperty(ALLOW_ALL_COMMANDS_IF_REGISTRATION_IS_OPTIONAL)) {
            return;
        }
        if (settings.getProperty(RestrictionSettings.ALLOW_COMMANDS).contains(cmd)) {
            return;
        }
        if (Utils.checkAuth(event.getPlayer())) {
            return;
        }
        event.setCancelled(true);
        sendLoginOrRegisterMessage(event.getPlayer());
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
        if (settings.getProperty(ALLOW_UNAUTHED_MOVEMENT) && settings.getProperty(ALLOWED_MOVEMENT_RADIUS) <= 0) {
            return;
        }

        /* 
         * Limit player X and Z movements to 1 block
         * Deny player Y+ movements (allows falling)
         */
        if (event.getFrom().getBlockX() == event.getTo().getBlockX()
            && event.getFrom().getBlockZ() == event.getTo().getBlockZ()
            && event.getFrom().getY() - event.getTo().getY() >= 0) {
            return;
        }

        Player player = event.getPlayer();
        if (Utils.checkAuth(player)) {
            return;
        }

        if (!settings.getProperty(RestrictionSettings.ALLOW_UNAUTHED_MOVEMENT)) {
            event.setTo(event.getFrom());
            // sgdc3 TODO: remove this, maybe we should set the effect every x ticks, idk!
            if (settings.getProperty(RestrictionSettings.REMOVE_SPEED)) {
                player.setFlySpeed(0.0f);
                player.setWalkSpeed(0.0f);
            }
            return;
        }

        if (settings.getProperty(RestrictionSettings.NO_TELEPORT)) {
            return;
        }

        Location spawn = spawnLoader.getSpawnLocation(player);
        if (spawn != null && spawn.getWorld() != null) {
            if (!player.getWorld().equals(spawn.getWorld())) {
                player.teleport(spawn);
                return;
            }
            if (spawn.distance(player.getLocation()) > settings.getProperty(ALLOWED_MOVEMENT_RADIUS)) {
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

        if (settings.getProperty(RegistrationSettings.REMOVE_JOIN_MESSAGE)) {
            event.setJoinMessage(null);
            return;
        }
        if (!settings.getProperty(RegistrationSettings.DELAY_JOIN_MESSAGE)) {
            return;
        }

        String name = player.getName().toLowerCase();
        String joinMsg = event.getJoinMessage();

        // Remove the join message while the player isn't logging in
        if (joinMsg != null) {
            event.setJoinMessage(null);
            joinMessage.put(name, joinMsg);
        }
    }

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        if (player == null) {
            return;
        }

        if (settings.getProperty(RestrictionSettings.FORCE_SURVIVAL_MODE)
            && !player.hasPermission(PlayerStatePermission.BYPASS_FORCE_SURVIVAL.getNode())) {
            player.setGameMode(GameMode.SURVIVAL);
        }

        // Shedule login task so works after the prelogin
        // (Fix found by Koolaid5000)
        bukkitService.runTask(new Runnable() {
            @Override
            public void run() {
                management.performJoin(player);
            }
        });
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPreLogin(AsyncPlayerPreLoginEvent event) {
        PlayerAuth auth = dataSource.getAuth(event.getName());
        if (settings.getProperty(RegistrationSettings.PREVENT_OTHER_CASE) && auth != null && auth.getRealName() != null) {
            String realName = auth.getRealName();
            if (!realName.isEmpty() && !"Player".equals(realName) && !realName.equals(event.getName())) {
                event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
                event.setKickMessage(m.retrieveSingle(MessageKey.INVALID_NAME_CASE, realName, event.getName()));
                return;
            }
            if (realName.isEmpty() || "Player".equals(realName)) {
                dataSource.updateRealName(event.getName().toLowerCase(), event.getName());
            }
        }

        if (auth == null && settings.getProperty(ProtectionSettings.ENABLE_PROTECTION)) {
            String playerIp = event.getAddress().getHostAddress();
            if (!validationService.isCountryAdmitted(playerIp)) {
                event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
                event.setKickMessage(m.retrieveSingle(MessageKey.COUNTRY_BANNED_ERROR));
                return;
            }
        }

        final String name = event.getName().toLowerCase();
        final Player player = bukkitService.getPlayerExact(name);
        // Check if forceSingleSession is set to true, so kick player that has
        // joined with same nick of online player
        if (player != null && settings.getProperty(RestrictionSettings.FORCE_SINGLE_SESSION)) {
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
                int playersOnline = bukkitService.getOnlinePlayers().size();
                if (playersOnline > plugin.getServer().getMaxPlayers()) {
                    event.allow();
                } else {
                    Player pl = plugin.generateKickPlayer(bukkitService.getOnlinePlayers());
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
        boolean isAuthAvailable = dataSource.isAuthAvailable(name);

        if (antiBot.getAntiBotStatus() == AntiBotStatus.ACTIVE && !isAuthAvailable) {
            event.setKickMessage(m.retrieveSingle(MessageKey.KICK_ANTIBOT));
            event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
            return;
        }

        if (settings.getProperty(RestrictionSettings.KICK_NON_REGISTERED) && !isAuthAvailable) {
            event.setKickMessage(m.retrieveSingle(MessageKey.MUST_REGISTER_MESSAGE));
            event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
            return;
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

        antiBot.checkAntiBot(player);

        if (settings.getProperty(HooksSettings.BUNGEECORD)) {
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

        if (settings.getProperty(RegistrationSettings.REMOVE_LEAVE_MESSAGE)) {
            event.setQuitMessage(null);
        }

        management.performQuit(player, false);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerKick(PlayerKickEvent event) {
        Player player = event.getPlayer();

        if (player == null) {
            return;
        }

        if (!settings.getProperty(RestrictionSettings.FORCE_SINGLE_SESSION)
            && event.getReason().equals(m.retrieveSingle(MessageKey.USERNAME_ALREADY_ONLINE_ERROR))) {
            event.setCancelled(true);
            return;
        }

        plugin.getManagement().performQuit(player, true);
    }

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
        if (event.getWhoClicked() == null) {
            return;
        }
        if (!(event.getWhoClicked() instanceof Player)) {
            return;
        }
        Player player = (Player) event.getWhoClicked();
        if (Utils.checkAuth(player)) {
            return;
        }
        if (plugin.getPluginHooks().isNpc(player)) {
            return;
        }
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
        Location spawn = spawnLoader.getSpawnLocation(player);
        if (Settings.isSaveQuitLocationEnabled && dataSource.isAuthAvailable(name)) {
            PlayerAuth auth = PlayerAuth.builder()
                .name(name)
                .realName(player.getName())
                .location(spawn)
                .build();
            dataSource.updateQuitLoc(auth);
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
