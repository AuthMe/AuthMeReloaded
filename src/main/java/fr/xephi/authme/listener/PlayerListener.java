package fr.xephi.authme.listener;

import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.service.AntiBotService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.SpawnLoader;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.TeleportationService;
import fr.xephi.authme.service.ValidationService;
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
import java.util.Iterator;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import static fr.xephi.authme.settings.properties.RestrictionSettings.ALLOWED_MOVEMENT_RADIUS;
import static fr.xephi.authme.settings.properties.RestrictionSettings.ALLOW_UNAUTHED_MOVEMENT;

/**
 * Listener class for player events.
 */
public class PlayerListener implements Listener {

    public static final ConcurrentHashMap<String, String> joinMessage = new ConcurrentHashMap<>();

    @Inject
    private Settings settings;
    @Inject
    private Messages m;
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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        String cmd = event.getMessage().split(" ")[0].toLowerCase();
        if (settings.getProperty(HooksSettings.USE_ESSENTIALS_MOTD) && cmd.equals("/motd")) {
            return;
        }
        if (settings.getProperty(RestrictionSettings.ALLOW_COMMANDS).contains(cmd)) {
            return;
        }
        final Player player = event.getPlayer();
        if (listenerService.shouldCancelEvent(player)) {
            event.setCancelled(true);
            m.send(player, MessageKey.DENIED_COMMAND);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (settings.getProperty(RestrictionSettings.ALLOW_CHAT)) {
            return;
        }

        final Player player = event.getPlayer();
        if (listenerService.shouldCancelEvent(player)) {
            event.setCancelled(true);
            m.send(player, MessageKey.DENIED_CHAT);
        } else if (settings.getProperty(RestrictionSettings.HIDE_CHAT)) {
            Set<Player> recipients = event.getRecipients();
            Iterator<Player> iter = recipients.iterator();
            while (iter.hasNext()) {
                Player p = iter.next();
                if (listenerService.shouldCancelEvent(p)) {
                    iter.remove();
                }
            }
            if (recipients.size() == 0) {
                event.setCancelled(true);
            }
        }
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
        Location from = event.getFrom();
        Location to = event.getTo();
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

    @EventHandler(priority = EventPriority.NORMAL)
    public void onPlayerJoin(PlayerJoinEvent event) {
        final Player player = event.getPlayer();
        teleportationService.teleportNewPlayerToFirstSpawn(player);
        management.performJoin(player);
    }

    // Note #831: AsyncPlayerPreLoginEvent is not fired by all servers in offline mode
    // e.g. CraftBukkit does not fire it. So we need to run crucial things with PlayerLoginEvent.
    // Single session feature can be implemented for Spigot and CraftBukkit by canceling a kick
    // event caused by "logged in from another location". The nicer way, but only for Spigot, would be
    // to check in the AsyncPlayerPreLoginEvent. To support all servers, we use the less nice way.

    @EventHandler(priority = EventPriority.LOW)
    public void onPlayerLogin(PlayerLoginEvent event) {
        final Player player = event.getPlayer();
        final String name = player.getName();
        if (validationService.isUnrestricted(name)) {
            return;
        } else if (onJoinVerifier.refusePlayerForFullServer(event)) {
            return;
        } else if (event.getResult() != PlayerLoginEvent.Result.ALLOWED) {
            return;
        }

        try {
            // Fast stuff
            onJoinVerifier.checkSingleSession(name);
            onJoinVerifier.checkIsValidName(name);

            // Get the auth later as this may cause the single session check to fail
            // Slow stuff
            final PlayerAuth auth = dataSource.getAuth(name);
            final boolean isAuthAvailable = (auth != null);
            onJoinVerifier.checkAntibot(player, isAuthAvailable);
            onJoinVerifier.checkKickNonRegistered(isAuthAvailable);
            onJoinVerifier.checkNameCasing(player, auth);
            onJoinVerifier.checkPlayerCountry(isAuthAvailable, event.getAddress().getHostAddress());
        } catch (FailedVerificationException e) {
            event.setKickMessage(m.retrieveSingle(e.getReason(), e.getArgs()));
            event.setResult(PlayerLoginEvent.Result.KICK_OTHER);
            return;
        }

        antiBotService.handlePlayerJoin();
        teleportationService.teleportOnJoin(player);
    }

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerPickupItem(PlayerPickupItemEvent event) {
        if (listenerService.shouldCancelEvent(event)) {
            event.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerInteract(PlayerInteractEvent event) {
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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerInventoryOpen(InventoryOpenEvent event) {
        final Player player = (Player) event.getPlayer();

        if (!listenerService.shouldCancelEvent(player)) {
            return;
        }
        event.setCancelled(true);

        /*
         * @note little hack cause InventoryOpenEvent cannot be cancelled for
         * real, cause no packet is send to server by client for the main inv
         */
        bukkitService.scheduleSyncDelayedTask(new Runnable() {
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
        if (!listenerService.shouldCancelEvent(player)) {
            return;
        }
        event.setCancelled(true);
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerHitPlayerEvent(EntityDamageByEntityEvent event) {
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
    public void onPlayerDropItem(PlayerDropItemEvent event) {
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
    public void onSignChange(SignChangeEvent event) {
        Player player = event.getPlayer();
        if (listenerService.shouldCancelEvent(player)) {
            event.setCancelled(true);
        }
    }

    // TODO: check this, why do we need to update the quit loc? -sgdc3
    @EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
    public void onPlayerRespawn(PlayerRespawnEvent event) {
        if (settings.getProperty(RestrictionSettings.NO_TELEPORT)) {
            return;
        }
        if (!listenerService.shouldCancelEvent(event)) {
            return;
        }
        Player player = event.getPlayer();
        String name = player.getName().toLowerCase();
        Location spawn = spawnLoader.getSpawnLocation(player);
        if (settings.getProperty(RestrictionSettings.SAVE_QUIT_LOCATION) && dataSource.isAuthAvailable(name)) {
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

}
