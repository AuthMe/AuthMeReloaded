package fr.xephi.authme.data.limbo;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.limbo.persistence.LimboPersistence;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.service.TeleportationService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.SpawnLoader;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EnderPearl;
import org.bukkit.entity.Entity;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.Collections;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import static fr.xephi.authme.settings.properties.LimboSettings.RESTORE_ALLOW_FLIGHT;
import static fr.xephi.authme.settings.properties.LimboSettings.RESTORE_FLY_SPEED;
import static fr.xephi.authme.settings.properties.LimboSettings.RESTORE_WALK_SPEED;

/**
 * Service for managing players that are in "limbo," a temporary state players are
 * put in which have joined but not yet logged in.
 */
public class LimboService {

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(LimboService.class);

    private final Map<String, LimboPlayer> entries = new ConcurrentHashMap<>();

    @Inject
    private Settings settings;

    @Inject
    private LimboPlayerTaskManager taskManager;

    @Inject
    private LimboServiceHelper helper;

    @Inject
    private LimboPersistence persistence;

    @Inject
    private AuthGroupHandler authGroupHandler;

    @Inject
    private SpawnLoader spawnLoader;

    @Inject
    private TeleportationService teleportationService;

    LimboService() {
    }

    /**
     * Creates a LimboPlayer for the given player and revokes all "limbo data" from the player.
     *
     * @param player the player to process
     * @param isRegistered whether or not the player is registered
     */
    public void createLimboPlayer(Player player, boolean isRegistered) {
        final String name = player.getName().toLowerCase(Locale.ROOT);

        LimboPlayer limboFromDisk = persistence.getLimboPlayer(player);
        if (limboFromDisk != null) {
            logger.debug("LimboPlayer for `{0}` already exists on disk", name);
        }

        LimboPlayer existingLimbo = entries.remove(name);
        if (existingLimbo != null) {
            existingLimbo.clearTasks();
            logger.debug("LimboPlayer for `{0}` already present in memory", name);
        }

        Location location = teleportationService.consumeOriginalJoinLocation(name, spawnLoader.getPlayerLocationOrSpawn(player));
        LimboPlayer limboPlayer = helper.merge(existingLimbo, limboFromDisk);
        limboPlayer = helper.merge(helper.createLimboPlayer(player, isRegistered, location), limboPlayer);

        taskManager.registerMessageTask(player, limboPlayer,
            isRegistered ? LimboMessageType.LOG_IN : LimboMessageType.REGISTER);
        taskManager.registerTimeoutTask(player, limboPlayer);
        helper.revokeLimboStates(player);
        authGroupHandler.setGroup(player, limboPlayer,
            isRegistered ? AuthGroupType.REGISTERED_UNAUTHENTICATED : AuthGroupType.UNREGISTERED);
        entries.put(name, limboPlayer);
        persistence.saveLimboPlayer(player, limboPlayer);
    }

    /**
     * Returns the limbo player for the given name, or null otherwise.
     *
     * @param name the name to retrieve the data for
     * @return the associated limbo player, or null if none available
     */
    public LimboPlayer getLimboPlayer(String name) {
        return entries.get(name.toLowerCase(Locale.ROOT));
    }

    /**
     * Returns whether there is a limbo player for the given name.
     *
     * @param name the name to check
     * @return true if present, false otherwise
     */
    public boolean hasLimboPlayer(String name) {
        return entries.containsKey(name.toLowerCase(Locale.ROOT));
    }

    /**
     * Restores the limbo data and subsequently deletes the entry.
     * <p>
     * Note that teleportation on the player is performed by {@link fr.xephi.authme.service.TeleportationService} and
     * changing the permission group is handled by {@link fr.xephi.authme.data.limbo.AuthGroupHandler}.
     *
     * @param player the player whose data should be restored
     */
    public void restoreData(Player player) {
        String lowerName = player.getName().toLowerCase(Locale.ROOT);
        LimboPlayer limbo = entries.remove(lowerName);

        if (limbo == null) {
            logger.debug("No LimboPlayer found for `{0}` - cannot restore", lowerName);
        } else {
            player.setOp(limbo.isOperator());
            settings.getProperty(RESTORE_ALLOW_FLIGHT).restoreAllowFlight(player, limbo);
            settings.getProperty(RESTORE_FLY_SPEED).restoreFlySpeed(player, limbo);
            settings.getProperty(RESTORE_WALK_SPEED).restoreWalkSpeed(player, limbo);
            limbo.clearTasks();
            logger.debug("Restored LimboPlayer stats for `{0}`", lowerName);
        }
        // Always remove the disk limbo regardless of whether an in-memory limbo was present.
        // If the player quits while in limbo before createLimboPlayer has run (race condition between
        // async join and async quit), the in-memory entry is null but the disk file may still exist.
        // Leaving it would cause the stale location to be reused on the next join.
        persistence.removeLimboPlayer(player);
        authGroupHandler.setGroup(player, limbo, AuthGroupType.LOGGED_IN);
    }

    /**
     * Creates new tasks for the given player and cancels the old ones for a newly registered player.
     * This resets his time to log in (TimeoutTask) and updates the message he is shown (MessageTask).
     *
     * @param player the player to reset the tasks for
     */
    public void replaceTasksAfterRegistration(Player player) {
        Optional<LimboPlayer> limboPlayer = getLimboOrLogError(player, "reset tasks");
        limboPlayer.ifPresent(limbo -> {
            taskManager.registerTimeoutTask(player, limbo);
            taskManager.registerMessageTask(player, limbo, LimboMessageType.LOG_IN);
        });
        authGroupHandler.setGroup(player, limboPlayer.orElse(null), AuthGroupType.REGISTERED_UNAUTHENTICATED);
    }

    /**
     * Resets the message task associated with the player's LimboPlayer.
     *
     * @param player the player to set a new message task for
     * @param messageType the message to show for the limbo player
     */
    public void resetMessageTask(Player player, LimboMessageType messageType) {
        getLimboOrLogError(player, "reset message task")
            .ifPresent(limbo -> taskManager.registerMessageTask(player, limbo, messageType));
    }

    /**
     * @param player the player whose message task should be muted
     */
    public void muteMessageTask(Player player) {
        getLimboOrLogError(player, "mute message task")
            .ifPresent(limbo -> LimboPlayerTaskManager.setMuted(limbo.getMessageTask(), true));
    }

    /**
     * @param player the player whose message task should be unmuted
     */
    public void unmuteMessageTask(Player player) {
        getLimboOrLogError(player, "unmute message task")
            .ifPresent(limbo -> LimboPlayerTaskManager.setMuted(limbo.getMessageTask(), false));
    }

    /**
     * Returns the limbo player for the given player or logs an error.
     *
     * @param player the player to retrieve the limbo player for
     * @param context the action for which the limbo player is being retrieved (for logging)
     * @return Optional with the limbo player
     */
    private Optional<LimboPlayer> getLimboOrLogError(Player player, String context) {
        LimboPlayer limbo = entries.get(player.getName().toLowerCase(Locale.ROOT));
        if (limbo == null) {
            logger.debug("No LimboPlayer found for `{0}`. Action: {1}", player.getName(), context);
        }
        return Optional.ofNullable(limbo);
    }

    /**
     * Saves the entity UUIDs of in-flight ender pearls to disk so they can be restored
     * to the player on reconnect (stasis chamber support). Called when an authenticated
     * player quits with pearls in flight.
     *
     * @param player     the authenticated player who is quitting
     * @param pearlUuids entity UUIDs of the player's in-flight ender pearls
     */
    public void saveEnderPearlsForPlayer(Player player, Set<UUID> pearlUuids) {
        LimboPlayer limbo = persistence.getLimboPlayer(player);
        if (limbo == null) {
            limbo = new LimboPlayer(null, player.isOp(), Collections.emptyList(), player.getAllowFlight(),
                player.getWalkSpeed(), player.getFlySpeed());
        }
        limbo.setEnderPearlUuids(pearlUuids);
        persistence.saveLimboPlayer(player, limbo);
        logger.debug("Saved {0} ender pearl(s) to disk for `{1}`",
            pearlUuids.size(), player.getName().toLowerCase(Locale.ROOT));
    }

    /**
     * Saves the vehicle the player was riding to disk so they can be remounted on reconnect.
     * Called when an authenticated player quits while inside a vehicle.
     *
     * @param player      the authenticated player who is quitting
     * @param vehicleUuid the entity UUID of the vehicle
     * @param vehicleType the entity type of the vehicle
     */
    public void saveVehicleForPlayer(Player player, UUID vehicleUuid, EntityType vehicleType) {
        LimboPlayer limbo = persistence.getLimboPlayer(player);
        if (limbo == null) {
            limbo = new LimboPlayer(null, player.isOp(), Collections.emptyList(), player.getAllowFlight(),
                player.getWalkSpeed(), player.getFlySpeed());
        }
        limbo.setVehicle(vehicleUuid, vehicleType);
        persistence.saveLimboPlayer(player, limbo);
        logger.debug("Saved vehicle {0} ({1}) to disk for `{2}`",
            vehicleUuid, vehicleType, player.getName().toLowerCase(Locale.ROOT));
    }

    /**
     * Restores ender pearl shooters and remounts the player's vehicle in a single world pass.
     * Must be called after {@link #createLimboPlayer} so the LimboPlayer is guaranteed to
     * exist in memory.
     *
     * @param player the player whose entities should be restored
     */
    public void restoreEntities(Player player) {
        String name = player.getName().toLowerCase(Locale.ROOT);
        LimboPlayer limbo = entries.get(name);
        if (limbo == null) {
            return;
        }
        Set<UUID> pearlUuids = limbo.getEnderPearlUuids();
        UUID vehicleUuid = limbo.getVehicleUuid();
        EntityType vehicleType = limbo.getVehicleType();

        if (pearlUuids.isEmpty() && vehicleUuid == null) {
            return;
        }

        int pearlTotal = pearlUuids.size();
        int pearlRestored = 0;
        boolean vehicleRestored = false;

        outer:
        for (World world : player.getServer().getWorlds()) {
            for (Entity entity : world.getEntities()) {
                if (!pearlUuids.isEmpty() && entity instanceof EnderPearl
                        && pearlUuids.contains(entity.getUniqueId())) {
                    ((EnderPearl) entity).setShooter(player);
                    pearlRestored++;
                } else if (vehicleUuid != null && !vehicleRestored
                        && vehicleUuid.equals(entity.getUniqueId())) {
                    entity.addPassenger(player);
                    vehicleRestored = true;
                }
                if (pearlUuids.isEmpty() && vehicleRestored) {
                    break outer;
                }
            }
        }

        pearlUuids.clear();
        limbo.setVehicle(null, null);

        if (pearlTotal > 0) {
            logger.debug("Restored {0}/{1} ender pearl(s) for `{2}`", pearlRestored, pearlTotal, name);
        }
        if (vehicleUuid != null) {
            if (vehicleRestored) {
                logger.debug("Restored vehicle ({0}) for `{1}`", vehicleType, name);
            } else {
                logger.debug("Vehicle ({0}) no longer exists for `{1}`, skipping", vehicleType, name);
            }
        }
    }
}
