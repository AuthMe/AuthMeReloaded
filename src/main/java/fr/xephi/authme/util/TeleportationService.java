package fr.xephi.authme.util;

import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.limbo.LimboPlayer;
import fr.xephi.authme.events.AbstractTeleportEvent;
import fr.xephi.authme.events.AuthMeTeleportEvent;
import fr.xephi.authme.events.FirstSpawnTeleportEvent;
import fr.xephi.authme.events.SpawnTeleportEvent;
import fr.xephi.authme.initialization.Reloadable;
import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.SpawnLoader;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.HashSet;
import java.util.Set;

import static fr.xephi.authme.settings.properties.RestrictionSettings.TELEPORT_UNAUTHED_TO_SPAWN;

/**
 * Handles teleportation (placement of player to spawn).
 */
public class TeleportationService implements Reloadable {

    @Inject
    private NewSetting settings;

    @Inject
    private BukkitService bukkitService;

    @Inject
    private SpawnLoader spawnLoader;

    @Inject
    private PlayerCache playerCache;

    private Set<String> spawnOnLoginWorlds;

    TeleportationService() {
    }

    private static boolean isEventValid(AbstractTeleportEvent event) {
        return !event.isCancelled() && event.getTo() != null && event.getTo().getWorld() != null;
    }

    @PostConstruct
    @Override
    public void reload() {
        // Use a Set for better performance with #contains()
        spawnOnLoginWorlds = new HashSet<>(settings.getProperty(RestrictionSettings.FORCE_SPAWN_ON_WORLDS));
    }

    public void teleportOnLoginEvent(final Player player) {
        if (settings.getProperty(RestrictionSettings.NO_TELEPORT)) {
            return;
        }

        if (settings.getProperty(TELEPORT_UNAUTHED_TO_SPAWN) || mustForceSpawnAfterLogin(player.getWorld().getName())) {
            teleportToSpawn(player, playerCache.isAuthenticated(player.getName()));
        }
    }

    public void teleportOnJoin(final Player player) {
        if (settings.getProperty(RestrictionSettings.NO_TELEPORT)) {
            return;
        }
        teleportToFirstSpawn(player);
    }

    public void teleportOnLogin(final Player player, PlayerAuth auth, LimboPlayer limbo) {
        if (settings.getProperty(RestrictionSettings.NO_TELEPORT)) {
            return;
        }

        // The world in LimboPlayer is from where the player comes, before any teleportation by AuthMe
        String worldName = limbo.getLoc().getWorld().getName();
        if (mustForceSpawnAfterLogin(worldName)) {
            teleportToSpawn(player, true);
        } else if (settings.getProperty(TELEPORT_UNAUTHED_TO_SPAWN)) {
            if (settings.getProperty(RestrictionSettings.SAVE_QUIT_LOCATION) && auth.getQuitLocY() != 0) {
                Location location = buildLocationFromAuth(player, auth);
                teleportBackFromSpawn(player, location);
            } else {
                teleportBackFromSpawn(player, limbo.getLoc());
            }
        }
    }

    private boolean mustForceSpawnAfterLogin(String worldName) {
        return settings.getProperty(RestrictionSettings.FORCE_SPAWN_LOCATION_AFTER_LOGIN)
            && spawnOnLoginWorlds.contains(worldName);
    }

    private Location buildLocationFromAuth(Player player, PlayerAuth auth) {
        World world = bukkitService.getWorld(auth.getWorld());
        if (world == null) {
            world = player.getWorld();
        }
        return new Location(world, auth.getQuitLocX(), auth.getQuitLocY(), auth.getQuitLocZ());
    }

    private boolean teleportToFirstSpawn(final Player player) {
        if (player.hasPlayedBefore()) {
            return false;
        }
        Location firstSpawn = spawnLoader.getFirstSpawn();
        if (firstSpawn == null) {
            return false;
        }

        performTeleportation(player, new FirstSpawnTeleportEvent(player, firstSpawn));
        return true;
    }

    private void teleportBackFromSpawn(final Player player, final Location location) {
        performTeleportation(player, new AuthMeTeleportEvent(player, location));
    }

    private void teleportToSpawn(final Player player, final boolean isAuthenticated) {
        final Location spawnLoc = spawnLoader.getSpawnLocation(player);
        performTeleportation(player, new SpawnTeleportEvent(player, spawnLoc, isAuthenticated));
    }

    /**
     * Emits the teleportation event and performs teleportation according to it (potentially modified
     * by external listeners). Note that not teleportation is performed if the event's location is empty.
     *
     * @param player the player to teleport
     * @param event  the event to emit and according to which to teleport
     */
    private void performTeleportation(final Player player, final AbstractTeleportEvent event) {
        bukkitService.scheduleSyncDelayedTask(new Runnable() {
            @Override
            public void run() {
                bukkitService.callEvent(event);
                if (player.isOnline() && isEventValid(event)) {
                    player.teleport(event.getTo());
                }
            }
        });
    }
}
