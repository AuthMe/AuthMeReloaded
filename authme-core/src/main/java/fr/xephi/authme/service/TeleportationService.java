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
import fr.xephi.authme.platform.TeleportAdapter;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.SpawnLoader;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

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

    @Inject
    private TeleportAdapter teleportAdapter;

    private Set<String> spawnOnLoginWorlds;
    private final ConcurrentHashMap<String, Boolean> preloadedAuthStatus = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Location> originalJoinLocations = new ConcurrentHashMap<>();

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
            && settings.getProperty(TELEPORT_UNAUTHED_TO_SPAWN)
            && !isUnregisteredWithOptionalAuth(player.getName())) {
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
        return prepareOnJoinSpawnLocation(player, null);
    }

    /**
     * Returns the player's custom on join location and stores the original spawn location for later restoration.
     *
     * @param player the player to process
     * @param originalLocation the server-selected spawn location before AuthMe modifies it
     * @return the custom spawn location, null if the player should spawn at the original location
     */
    public Location prepareOnJoinSpawnLocation(final Player player, final Location originalLocation) {
        rememberOriginalJoinLocation(player.getName(), originalLocation);
        if (shouldApplyOnJoinSpawnLocation(player.getName())) {
            final Location location = spawnLoader.getSpawnLocation(player);

            SpawnTeleportEvent event = new SpawnTeleportEvent(player, location,
                playerCache.isAuthenticated(player.getName()));
            bukkitService.callEvent(event);
            if (!isEventValid(event)) {
                return null;
            }

            logger.debug("Returning custom location for join event for player `{0}`", player.getName());
            return event.getTo();
        }
        return null;
    }

    /**
     * Returns the player's custom on join location without requiring a {@link Player} instance.
     * Intended for platform hooks that run before the player object is available.
     *
     * @param playerName the player's name
     * @param world the world the player is about to join in
     * @return the custom spawn location, null if the player should spawn at the original location
     */
    public Location prepareOnJoinSpawnLocation(String playerName, World world) {
        return prepareOnJoinSpawnLocationInternal(playerName, world);
    }

    /**
     * Returns the player's custom on join location without requiring a {@link Player} instance and stores the
     * original spawn location for later restoration.
     *
     * @param playerName the player's name
     * @param originalLocation the server-selected spawn location before AuthMe modifies it
     * @return the custom spawn location, null if the player should spawn at the original location
     */
    public Location prepareOnJoinSpawnLocation(String playerName, Location originalLocation) {
        rememberOriginalJoinLocation(playerName, originalLocation);
        return prepareOnJoinSpawnLocationInternal(playerName,
            originalLocation == null ? null : originalLocation.getWorld());
    }

    private Location prepareOnJoinSpawnLocationInternal(String playerName, World world) {
        if (!shouldApplyOnJoinSpawnLocation(playerName) || world == null) {
            return null;
        }

        logger.debug("Returning custom location for async join event for player `{0}`", playerName);
        return spawnLoader.getSpawnLocation(world);
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
        Location joinLocation = limbo != null && limbo.getLocation() != null
            ? limbo.getLocation()
            : peekOriginalJoinLocation(player.getName());
        String worldName = (joinLocation != null && joinLocation.getWorld() != null)
            ? joinLocation.getWorld().getName()
            : null;

        // The world in LimboPlayer is from where the player comes, before any teleportation by AuthMe
        if (mustForceSpawnAfterLogin(worldName)) {
            logger.debug("Teleporting `{0}` to spawn because of 'force-spawn after login'", player.getName());
            teleportToSpawn(player, true);
        } else if (settings.getProperty(TELEPORT_UNAUTHED_TO_SPAWN)) {
            if (settings.getProperty(RestrictionSettings.SAVE_QUIT_LOCATION)) {
                Location location = buildLocationFromAuth(player, auth);
                if (shouldUseJoinLocationAsFallback(auth, joinLocation)) {
                    logger.debug("Teleporting `{0}` after login, based on the remembered join location",
                        player.getName());
                    location = joinLocation;
                } else {
                    logger.debug("Teleporting `{0}` after login, based on the player auth", player.getName());
                }
                if (hasSamePositionAndWorld(location, player.getLocation())) {
                    logger.debug("Skipping teleport of `{0}` after login because the player is already there",
                        player.getName());
                    return;
                }
                teleportBackFromSpawn(player, location);
            } else if (joinLocation != null) {
                logger.debug("Teleporting `{0}` after login, based on the remembered join location", player.getName());
                teleportBackFromSpawn(player, joinLocation);
            }
        }
    }

    /**
     * Caches the player's registration status from the async pre-login event to avoid a blocking
     * DB call on the main thread later. Must be cleared with {@link #clearPreloadedAuthStatus}.
     *
     * @param name the player name (case-insensitive)
     * @param isRegistered whether the player has an account
     */
    public void preloadAuthStatus(String name, boolean isRegistered) {
        String normalizedName = normalizePlayerName(name);
        if (normalizedName != null) {
            preloadedAuthStatus.put(normalizedName, isRegistered);
        }
    }

    /**
     * Clears the pre-loaded registration status for the given player after it has been consumed.
     *
     * @param name the player name
     */
    public void clearPreloadedAuthStatus(String name) {
        String normalizedName = normalizePlayerName(name);
        if (normalizedName != null) {
            preloadedAuthStatus.remove(normalizedName);
        }
    }

    /**
     * Stores the server-selected join location before AuthMe redirects the player to its configured spawn.
     *
     * @param name the player name
     * @param location the original join location
     */
    public void rememberOriginalJoinLocation(String name, Location location) {
        String normalizedName = normalizePlayerName(name);
        if (normalizedName == null) {
            return;
        }
        if (location == null || location.getWorld() == null) {
            clearOriginalJoinLocation(normalizedName);
            return;
        }
        originalJoinLocations.put(normalizedName, location.clone());
    }

    /**
     * Returns the remembered original join location without consuming it.
     *
     * @param name the player name
     * @return the remembered location, or null if none exists
     */
    public Location peekOriginalJoinLocation(String name) {
        String normalizedName = normalizePlayerName(name);
        return normalizedName == null ? null : originalJoinLocations.get(normalizedName);
    }

    /**
     * Returns and clears the remembered original join location for the player.
     *
     * @param name the player name
     * @param fallback the location to return if no join location was remembered
     * @return the remembered location, or the fallback if absent
     */
    public Location consumeOriginalJoinLocation(String name, Location fallback) {
        String normalizedName = normalizePlayerName(name);
        if (normalizedName == null) {
            return fallback;
        }
        Location location = originalJoinLocations.remove(normalizedName);
        return location != null ? location : fallback;
    }

    /**
     * Clears the remembered original join location for the given player.
     *
     * @param name the player name
     */
    public void clearOriginalJoinLocation(String name) {
        String normalizedName = normalizePlayerName(name);
        if (normalizedName != null) {
            originalJoinLocations.remove(normalizedName);
        }
    }

    private boolean shouldApplyOnJoinSpawnLocation(String playerName) {
        return !settings.getProperty(RestrictionSettings.NO_TELEPORT)
            && settings.getProperty(TELEPORT_UNAUTHED_TO_SPAWN)
            && !isUnregisteredWithOptionalAuth(playerName);
    }

    private boolean isUnregisteredWithOptionalAuth(String playerName) {
        if (settings.getProperty(RegistrationSettings.FORCE)) {
            return false;
        }
        String key = normalizePlayerName(playerName);
        if (key == null) {
            return false;
        }
        Boolean cached = preloadedAuthStatus.get(key);
        boolean isRegistered = cached != null ? cached : dataSource.isAuthAvailable(playerName);
        return !isRegistered;
    }

    private boolean mustForceSpawnAfterLogin(String worldName) {
        return worldName != null && settings.getProperty(RestrictionSettings.FORCE_SPAWN_LOCATION_AFTER_LOGIN)
            && spawnOnLoginWorlds.contains(worldName);
    }

    private static boolean shouldUseJoinLocationAsFallback(PlayerAuth auth, Location joinLocation) {
        return joinLocation != null
            && hasDefaultStoredQuitLocation(auth)
            && !matchesStoredQuitLocation(auth, joinLocation);
    }

    private Location buildLocationFromAuth(Player player, PlayerAuth auth) {
        World world = bukkitService.getWorld(auth.getWorld());
        if (world == null) {
            world = player.getWorld();
        }
        return new Location(world, auth.getQuitLocX(), auth.getQuitLocY(), auth.getQuitLocZ(),
            auth.getYaw(), auth.getPitch());
    }

    private static boolean hasDefaultStoredQuitLocation(PlayerAuth auth) {
        return auth != null
            && Double.compare(auth.getQuitLocX(), 0.0) == 0
            && Double.compare(auth.getQuitLocY(), 0.0) == 0
            && Double.compare(auth.getQuitLocZ(), 0.0) == 0
            && "world".equals(auth.getWorld());
    }

    private static boolean matchesStoredQuitLocation(PlayerAuth auth, Location location) {
        return location != null && location.getWorld() != null
            && Double.compare(auth.getQuitLocX(), location.getX()) == 0
            && Double.compare(auth.getQuitLocY(), location.getY()) == 0
            && Double.compare(auth.getQuitLocZ(), location.getZ()) == 0
            && auth.getWorld().equals(location.getWorld().getName());
    }

    private static String normalizePlayerName(String name) {
        return name == null ? null : name.toLowerCase(Locale.ROOT);
    }

    private static boolean hasSamePositionAndWorld(Location first, Location second) {
        return first != null && second != null
            && Double.compare(first.getX(), second.getX()) == 0
            && Double.compare(first.getY(), second.getY()) == 0
            && Double.compare(first.getZ(), second.getZ()) == 0
            && first.getWorld() == second.getWorld();
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
        bukkitService.scheduleSyncTaskFromOptionallyAsyncTask(player, () -> {
            bukkitService.callEvent(event);
            if (player.isOnline() && isEventValid(event)) {
                teleportAdapter.teleportPlayer(player, event.getTo());
            }
        });
    }

    private static boolean isEventValid(AbstractTeleportEvent event) {
        return !event.isCancelled() && event.getTo() != null && event.getTo().getWorld() != null;
    }
}
