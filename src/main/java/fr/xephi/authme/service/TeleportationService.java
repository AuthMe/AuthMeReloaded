package fr.xephi.authme.service;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.data.limbo.LimboPlayer;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.events.AbstractTeleportEvent;
import fr.xephi.authme.events.AuthMeTeleportEvent;
import fr.xephi.authme.events.FirstSpawnTeleportEvent;
import fr.xephi.authme.events.SpawnTeleportEvent;
import fr.xephi.authme.initialization.Reloadable;
import fr.xephi.authme.output.ConsoleLoggerFactory;
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
    
    private final ConsoleLogger logger = ConsoleLoggerFactory.get(TeleportationService.class);

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
     *
     * @param player the player to process
     */
    public void teleportOnJoin(final Player player) {
        if (!settings.getProperty(RestrictionSettings.NO_TELEPORT)
            && settings.getProperty(TELEPORT_UNAUTHED_TO_SPAWN)) {
            logger.debug("Teleport on join for player `{0}`", player.getName());
            teleportToSpawn(player, playerCache.isAuthenticated(player.getName()));
        }
    }

    /**
     * Returns the player's custom on join location.
     *
     * @param player the player to process
     *
     * @return the custom spawn location, null if the player should spawn at the original location
     */
    public Location prepareOnJoinSpawnLocation(final Player player) {
        if (!settings.getProperty(RestrictionSettings.NO_TELEPORT)
            && settings.getProperty(TELEPORT_UNAUTHED_TO_SPAWN)) {
            final Location location = spawnLoader.getSpawnLocation(player);

            SpawnTeleportEvent event = new SpawnTeleportEvent(player, location,
                playerCache.isAuthenticated(player.getName()));
            bukkitService.callEvent(event);
            if (!isEventValid(event)) {
                return null;
            }

            logger.debug("Returning custom location for >1.9 join event for player `{0}`", player.getName());
            return location;
        }
        return null;
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
            logger.debug("Attempting to teleport player `{0}` to first spawn", player.getName());
            performTeleportation(player, new FirstSpawnTeleportEvent(player, firstSpawn));
        }
    }

    /**
     * Teleports the player according to the settings after having successfully logged in.
     *
     * @param player the player
     * @param auth corresponding PlayerAuth object
     * @param limbo corresponding LimboPlayer object
     */
    public void teleportOnLogin(final Player player, PlayerAuth auth, LimboPlayer limbo) {
        if (settings.getProperty(RestrictionSettings.NO_TELEPORT)) {
            return;
        }

        // #856: If LimboPlayer comes from a persisted file, the Location might be null
        String worldName = (limbo != null && limbo.getLocation() != null)
            ? limbo.getLocation().getWorld().getName()
            : null;

        // The world in LimboPlayer is from where the player comes, before any teleportation by AuthMe
        if (mustForceSpawnAfterLogin(worldName)) {
            logger.debug("Teleporting `{0}` to spawn because of 'force-spawn after login'", player.getName());
            teleportToSpawn(player, true);
        } else if (settings.getProperty(TELEPORT_UNAUTHED_TO_SPAWN)) {
            if (settings.getProperty(RestrictionSettings.SAVE_QUIT_LOCATION) && auth.getQuitLocY() != 0) {
                Location location = buildLocationFromAuth(player, auth);
                logger.debug("Teleporting `{0}` after login, based on the player auth", player.getName());
                teleportBackFromSpawn(player, location);
            } else if (limbo != null && limbo.getLocation() != null) {
                logger.debug("Teleporting `{0}` after login, based on the limbo player", player.getName());
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
        return new Location(world, auth.getQuitLocX(), auth.getQuitLocY(), auth.getQuitLocZ(),
            auth.getYaw(), auth.getPitch());
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
     * by external listeners). Note that no teleportation is performed if the event's location is empty.
     *
     * @param player the player to teleport
     * @param event  the event to emit and according to which to teleport
     */
    private void performTeleportation(final Player player, final AbstractTeleportEvent event) {
        bukkitService.scheduleSyncTaskFromOptionallyAsyncTask(() -> {
            bukkitService.callEvent(event);
            if (player.isOnline() && isEventValid(event)) {
                player.teleport(event.getTo());
            }
        });
    }

    private static boolean isEventValid(AbstractTeleportEvent event) {
        return !event.isCancelled() && event.getTo() != null && event.getTo().getWorld() != null;
    }
}
