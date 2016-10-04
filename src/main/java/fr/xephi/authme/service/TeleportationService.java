package fr.xephi.authme.service;

import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.data.limbo.LimboPlayer;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.AbstractTeleportEvent;
import fr.xephi.authme.events.AuthMeTeleportEvent;
import fr.xephi.authme.events.FirstSpawnTeleportEvent;
import fr.xephi.authme.events.SpawnTeleportEvent;
import fr.xephi.authme.initialization.Reloadable;
import fr.xephi.authme.settings.Settings;
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
    private Settings settings;

    @Inject
    private BukkitService bukkitService;

    @Inject
    private SpawnLoader spawnLoader;

    @Inject
    private PlayerCache playerCache;

    @Inject
    private DataSource dataSource;

    private Set<String> spawnOnLoginWorlds;

    TeleportationService() {
    }

    @PostConstruct
    @Override
    public void reload() {
        // Use a Set for better performance with #contains()
        spawnOnLoginWorlds = new HashSet<>(settings.getProperty(RestrictionSettings.FORCE_SPAWN_ON_WORLDS));
    }

    /**
     * Teleports the player according to the settings when he joins.
     * <p>
     * Note: this is triggered by Bukkit's PlayerLoginEvent, during which you cannot use
     * {@link Player#hasPlayedBefore()}: it always returns {@code false}. We trigger teleportation
     * from the PlayerLoginEvent and not the PlayerJoinEvent to ensure that the location is overridden
     * as fast as possible (cf. <a href="https://github.com/Xephi/AuthMeReloaded/issues/682">AuthMe #682</a>).
     *
     * @param player the player to process
     * @see <a href="https://bukkit.atlassian.net/browse/BUKKIT-3521">BUKKIT-3521: Player.hasPlayedBefore()
     * always false</a>
     */
    public void teleportOnJoin(final Player player) {
        if (!settings.getProperty(RestrictionSettings.NO_TELEPORT)
            && settings.getProperty(TELEPORT_UNAUTHED_TO_SPAWN)) {
            teleportToSpawn(player, playerCache.isAuthenticated(player.getName()));
        }
    }

    /**
     * Teleports the player to the first spawn if he is new and the first spawn is configured.
     *
     * @param player the player to process
     */
    public void teleportNewPlayerToFirstSpawn(final Player player) {
        if (settings.getProperty(RestrictionSettings.NO_TELEPORT)) {
            return;
        }

        Location firstSpawn = spawnLoader.getFirstSpawn();
        if (firstSpawn == null) {
            return;
        }

        if (!player.hasPlayedBefore() || !dataSource.isAuthAvailable(player.getName())) {
            performTeleportation(player, new FirstSpawnTeleportEvent(player, firstSpawn));
        }
    }

    /**
     * Teleports the player according to the settings after having successfully logged in.
     *
     * @param player the player
     * @param auth corresponding PlayerAuth object
     * @param limbo corresponding PlayerData object
     */
    public void teleportOnLogin(final Player player, PlayerAuth auth, LimboPlayer limbo) {
        if (settings.getProperty(RestrictionSettings.NO_TELEPORT)) {
            return;
        }

        // #856: If PlayerData comes from a persisted file, the Location might be null
        String worldName = (limbo != null && limbo.getLocation() != null)
            ? limbo.getLocation().getWorld().getName()
            : null;

        // The world in PlayerData is from where the player comes, before any teleportation by AuthMe
        if (mustForceSpawnAfterLogin(worldName)) {
            teleportToSpawn(player, true);
        } else if (settings.getProperty(TELEPORT_UNAUTHED_TO_SPAWN)) {
            if (settings.getProperty(RestrictionSettings.SAVE_QUIT_LOCATION) && auth.getQuitLocY() != 0) {
                Location location = buildLocationFromAuth(player, auth);
                teleportBackFromSpawn(player, location);
            } else if (limbo != null && limbo.getLocation() != null) {
                teleportBackFromSpawn(player, limbo.getLocation());
            }
        }
    }

    private boolean mustForceSpawnAfterLogin(String worldName) {
        return worldName != null && settings.getProperty(RestrictionSettings.FORCE_SPAWN_LOCATION_AFTER_LOGIN)
            && spawnOnLoginWorlds.contains(worldName);
    }

    private Location buildLocationFromAuth(Player player, PlayerAuth auth) {
        World world = bukkitService.getWorld(auth.getWorld());
        if (world == null) {
            world = player.getWorld();
        }
        return new Location(world, auth.getQuitLocX(), auth.getQuitLocY(), auth.getQuitLocZ());
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

    private static boolean isEventValid(AbstractTeleportEvent event) {
        return !event.isCancelled() && event.getTo() != null && event.getTo().getWorld() != null;
    }
}
