package fr.xephi.authme.listener;

import fr.xephi.authme.data.QuickCommandsProtectionManager;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PlayerStatePermission;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.service.AntiBotService;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.JoinMessageService;
import fr.xephi.authme.service.TeleportationService;
import fr.xephi.authme.service.ValidationService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.SpawnLoader;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.HumanEntity;
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
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.InventoryView;

import javax.inject.Inject;
import java.util.Locale;
import java.util.Set;

import static fr.xephi.authme.settings.properties.RestrictionSettings.ALLOWED_MOVEMENT_RADIUS;
import static fr.xephi.authme.settings.properties.RestrictionSettings.ALLOW_UNAUTHED_MOVEMENT;

/**
 * Listener class for player events.
 */
public class PlayerListener implements Listener {

    @Inject
    private Settings settings;
    @Inject
    private Messages messages;
    @Inject
    private DataSource dataSource;
    @Inject
    private AntiBotService antiBotService;
    @Inject
    private Management management;
    @Inject
    private BukkitService bukkitService;
    @Inject
    private SpawnLoader spawnLoader;
    @Inject
    private OnJoinVerifier onJoinVerifier;
    @Inject
    private ListenerService listenerService;
    @Inject
    private TeleportationService teleportationService;
    @Inject
    private ValidationService validationService;
    @Inject
    private JoinMessageService joinMessageService;
    @Inject
    private PermissionsManager permissionsManager;
    @Inject
    private QuickCommandsProtectionManager quickCommandsProtectionManager;

    // Lowest priority to apply fast protection checks
    @EventHandler(priority = EventPriority.LOWEST)
    public void onAsyncPlayerPreLoginEventLowest(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }
        final String name = event.getName();

        // NOTE: getAddress() sometimes returning null, we don't want to handle this race condition
        if (event.getAddress() == null) {
            event.disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER,
                messages.retrieveSingle(name, MessageKey.KICK_UNRESOLVED_HOSTNAME));
            return;
        }

        if (validationService.isUnrestricted(name)) {
            return;
        }

        // Non-blocking checks
        try {
            onJoinVerifier.checkIsValidName(name);
        } catch (FailedVerificationException e) {
            event.setKickMessage(messages.retrieveSingle(name, e.getReason(), e.getArgs()));
            event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
        }
    }

    /*
     * Login/join/leave events
     */

    // Note: at this stage (HIGHEST priority) the user's permission data should already have been loaded by
    // the permission handler, we don't need to call permissionsManager.loadUserData()

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onAsyncPlayerPreLoginEventHighest(AsyncPlayerPreLoginEvent event) {
        if (event.getLoginResult() != AsyncPlayerPreLoginEvent.Result.ALLOWED) {
            return;
        }
        final String name = event.getName();

        if (validationService.isUnrestricted(name)) {
            return;
        }

        // Slow, blocking checks
        try {
            final PlayerAuth auth = dataSource.getAuth(name);
            final boolean isAuthAvailable = auth != null;
            onJoinVerifier.checkKickNonRegistered(isAuthAvailable);
            onJoinVerifier.checkAntibot(name, isAuthAvailable);
            onJoinVerifier.checkNameCasing(name, auth);
            final String ip = event.getAddress().getHostAddress();
            onJoinVerifier.checkPlayerCountry(name, ip, isAuthAvailable);
        } catch (FailedVerificationException e) {
            event.setKickMessage(messages.retrieveSingle(name, e.getReason(), e.getArgs()));
            event.setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
        }
    }

    // Note: We can't teleport the player in the PlayerLoginEvent listener
    // as the new player location will be reverted by the server.

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerLogin(PlayerLoginEvent event) {
        final Player player = event.getPlayer();
        final String name = player.getName();

        try {
            onJoinVerifier.checkSingleSession(name);
        } catch (FailedVerificationException e) {
            event.setKickMessage(messages.retrieveSingle(name, e.getReason(), e.getArgs()));
            event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
            return;
        }

        if (validationService.isUnrestricted(name)) {
            return;
        }

        onJoinVerifier.refusePlayerForFullServer(event);
    }

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        if (!PlayerListener19Spigot.isPlayerSpawnLocationEventCalled()) {
            teleportationService.teleportOnJoin(player);
        }

        quickCommandsProtectionManager.processJoin(player);

        management.performJoin(player);

        teleportationService.teleportNewPlayerToFirstSpawn(player);
    }

    @EventHandler(priority = EventPriority.HIGH) // HIGH as EssentialsX listens at HIGHEST
    public void onJoinMessage(PlayerJoinEvent event) {
        final Player player = event.getPlayer();

        // Note: join message can be null, despite api documentation says not
        if (settings.getProperty(RegistrationSettings.REMOVE_JOIN_MESSAGE)) {
            event.setJoinMessage(null);
            return;
        }

        String customJoinMessage = settings.getProperty(RegistrationSettings.CUSTOM_JOIN_MESSAGE);
        if (!customJoinMessage.isEmpty()) {
            customJoinMessage = ChatColor.translateAlternateColorCodes('&', customJoinMessage);
            event.setJoinMessage(customJoinMessage
                .replace("{PLAYERNAME}", player.getName())
                .replace("{DISPLAYNAME}", player.getDisplayName())
                .replace("{DISPLAYNAMENOCOLOR}", ChatColor.stripColor(player.getDisplayName()))
            );
        }

        if (!settings.getProperty(RegistrationSettings.DELAY_JOIN_MESSAGE)) {
            return;
        }

        String name = player.getName().toLowerCase(Locale.ROOT);
        String joinMsg = event.getJoinMessage();

        // Remove the join message while the player isn't logging in
        if (joinMsg != null) {
            event.setJoinMessage(null);
            joinMessageService.putMessage(name, joinMsg);
        }
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        // Note: quit message can be null, despite api documentation says not
        if (settings.getProperty(RegistrationSettings.REMOVE_LEAVE_MESSAGE)) {
            event.setQuitMessage(null);
        } else if (settings.getProperty(RegistrationSettings.REMOVE_UNLOGGED_LEAVE_MESSAGE)) {
            if (listenerService.shouldCancelEvent(event)) {
                event.setQuitMessage(null);
            }
        }

        if (antiBotService.wasPlayerKicked(player.getName())) {
            return;
        }

        management.performQuit(player);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerKick(PlayerKickEvent event) {
        // Note #831: Especially for offline CraftBukkit, we need to catch players being kicked because of
        // "logged in from another location" and to cancel their kick
        if (settings.getProperty(RestrictionSettings.FORCE_SINGLE_SESSION)
            && event.getReason().contains("You logged in from another location")) {
            event.setCancelled(true);
            return;
        }

        final Player player = event.getPlayer();
        if (!antiBotService.wasPlayerKicked(player.getName())) {
            management.performQuit(player);
        }
    }

    /*
     * Chat/command events
     */

    private void removeUnauthorizedRecipients(AsyncPlayerChatEvent event) {
        if (settings.getProperty(RestrictionSettings.HIDE_CHAT)) {
            event.getRecipients().removeIf(listenerService::shouldCancelEvent);
            if (event.getRecipients().isEmpty()) {
                event.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (settings.getProperty(RestrictionSettings.ALLOW_CHAT)) {
            return;
        }

        final Player player = event.getPlayer();
        final boolean mayPlayerSendChat = !listenerService.shouldCancelEvent(player)
            || permissionsManager.hasPermission(player, PlayerStatePermission.ALLOW_CHAT_BEFORE_LOGIN);
        if (mayPlayerSendChat) {
            removeUnauthorizedRecipients(event);
        } else {
            event.setCancelled(true);
            messages.send(player, MessageKey.DENIED_CHAT);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String cmd = event.getMessage().split(" ")[0].toLowerCase(Locale.ROOT);
        if (settings.getProperty(HooksSettings.USE_ESSENTIALS_MOTD) && "/motd".equals(cmd)) {
            return;
        }
        if (settings.getProperty(RestrictionSettings.ALLOW_COMMANDS).contains(cmd)) {
            return;
        }
        final Player player = event.getPlayer();
        if (!quickCommandsProtectionManager.isAllowed(player.getName())) {
            event.setCancelled(true);
            player.kickPlayer(messages.retrieveSingle(player, MessageKey.QUICK_COMMAND_PROTECTION_KICK));
            return;
        }
        if (listenerService.shouldCancelEvent(player)) {
            event.setCancelled(true);
            messages.send(player, MessageKey.DENIED_COMMAND);
        }
    }

    /*
     * Movement events
     */

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerMove(PlayerMoveEvent event) {
        if (settings.getProperty(ALLOW_UNAUTHED_MOVEMENT) && settings.getProperty(ALLOWED_MOVEMENT_RADIUS) <= 0) {
            return;
        }

        Location from = event.getFrom();
        Location to = event.getTo();
        if (to == null) {
            return;
        }

        /*
         * Limit player X and Z movements to 1 block
         * Deny player Y+ movements (allows falling)
         */

        if (from.getBlockX() == to.getBlockX()
            && from.getBlockZ() == to.getBlockZ()
            && from.getY() - to.getY() >= 0) {
            return;
        }

        Player player = event.getPlayer();
        if (!listenerService.shouldCancelEvent(player)) {
            return;
        }

        if (!settings.getProperty(RestrictionSettings.ALLOW_UNAUTHED_MOVEMENT)) {
            // "cancel" the event
            event.setTo(event.getFrom());
            return;
        }

        if (settings.getProperty(RestrictionSettings.NO_TELEPORT)) {
            return;
        }

        Location spawn = spawnLoader.getSpawnLocation(player);
        if (spawn != null && spawn.getWorld() != null) {
            if (!player.getWorld().equals(spawn.getWorld())) {
                player.teleport(spawn);
            } else if (spawn.distance(player.getLocation()) > settings.getProperty(ALLOWED_MOVEMENT_RADIUS)) {
                player.teleport(spawn);
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (settings.getProperty(RestrictionSettings.NO_TELEPORT)) {
            return;
        }
        if (!listenerService.shouldCancelEvent(event)) {
            return;
        }
        Location spawn = spawnLoader.getSpawnLocation(event.getPlayer());
        if (spawn != null && spawn.getWorld() != null) {
            event.setRespawnLocation(spawn);
        }
    }

    /*
     * Entity/block interaction events
     */

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
        if (listenerService.shouldCancelEvent(event)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerInteractEntity(PlayerInteractEntityEvent event) {
        if (listenerService.shouldCancelEvent(event)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerInteractAtEntity(PlayerInteractAtEntityEvent event) {
        if (listenerService.shouldCancelEvent(event)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerHitPlayerEvent(EntityDamageByEntityEvent event) {
        if (listenerService.shouldCancelEvent(event)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerShear(PlayerShearEntityEvent event) {
        if (listenerService.shouldCancelEvent(event)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerFish(PlayerFishEvent event) {
        if (listenerService.shouldCancelEvent(event)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerBedEnter(PlayerBedEnterEvent event) {
        if (listenerService.shouldCancelEvent(event)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerEditBook(PlayerEditBookEvent event) {
        if (listenerService.shouldCancelEvent(event)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        if (listenerService.shouldCancelEvent(player)) {
            event.setCancelled(true);
        }
    }

    /*
     * Inventory interactions
     */

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        if (listenerService.shouldCancelEvent(event)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerDropItem(PlayerDropItemEvent event) {
        if (listenerService.shouldCancelEvent(event)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerHeldItem(PlayerItemHeldEvent event) {
        if (listenerService.shouldCancelEvent(event)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerConsumeItem(PlayerItemConsumeEvent event) {
        if (listenerService.shouldCancelEvent(event)) {
            event.setCancelled(true);
        }
    }

    private boolean isInventoryWhitelisted(InventoryView inventory) {
        if (inventory == null) {
            return false;
        }
        Set<String> whitelist = settings.getProperty(RestrictionSettings.UNRESTRICTED_INVENTORIES);
        return whitelist.contains(ChatColor.stripColor(inventory.getTitle()).toLowerCase(Locale.ROOT));
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerInventoryOpen(InventoryOpenEvent event) {
        final HumanEntity player = event.getPlayer();

        if (listenerService.shouldCancelEvent(player)
            && !isInventoryWhitelisted(event.getView())) {
            event.setCancelled(true);

            /*
             * @note little hack cause InventoryOpenEvent cannot be cancelled for
             * real, cause no packet is sent to server by client for the main inv
             */
            bukkitService.scheduleSyncDelayedTask(player::closeInventory, 1);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerInventoryClick(InventoryClickEvent event) {
        if (listenerService.shouldCancelEvent(event.getWhoClicked())
            && !isInventoryWhitelisted(event.getView())) {
            event.setCancelled(true);
        }
    }
}
