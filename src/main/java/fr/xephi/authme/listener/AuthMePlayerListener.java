package fr.xephi.authme.listener;

import fr.xephi.authme.AntiBot;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.hooks.PluginHooks;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.SpawnLoader;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.util.BukkitService;
import fr.xephi.authme.util.Utils;
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
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;

import javax.inject.Inject;
import java.util.Iterator;
import java.util.Set;
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
    private PluginHooks pluginHooks;

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

    @EventHandler(ignoreCancelled = true, priority = EventPriority.LOWEST)
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        if (settings.getProperty(RestrictionSettings.ALLOW_CHAT)) {
            return;
        }

        final Player player = event.getPlayer();
        if (shouldCancelEvent(player)) {
            event.setCancelled(true);
            bukkitService.runTaskAsynchronously(new Runnable() {
                @Override
                public void run() {
                    m.send(player, MessageKey.DENIED_CHAT);
                }
            });
        } else if (settings.getProperty(RestrictionSettings.HIDE_CHAT)) {
            Set<Player> recipients = event.getRecipients();
            Iterator<Player> iter = recipients.iterator();
            while (iter.hasNext()) {
                Player p = iter.next();
                if (shouldCancelEvent(p)) {
                    iter.remove();
                }
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

    @EventHandler(priority = EventPriority.HIGHEST)
    public void onPlayerQuit(PlayerQuitEvent event) {
        Player player = event.getPlayer();

        if (player == null) {
            return;
        }

        if (settings.getProperty(RegistrationSettings.REMOVE_LEAVE_MESSAGE)) {
            event.setQuitMessage(null);
        }

        if (antiBot.antibotKicked.contains(player.getName())) {
            return;
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

        if (!antiBot.antibotKicked.contains(player.getName())) {
            management.performQuit(player, true);
        }
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
        if (Utils.checkAuth(player)) {
            return;
        }
        if (pluginHooks.isNpc(player)) {
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
        if (settings.getProperty(RestrictionSettings.NO_TELEPORT)) {
            return;
        }
        if (!shouldCancelEvent(event)) {
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
