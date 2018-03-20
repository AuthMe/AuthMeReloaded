package fr.xephi.authme.data.limbo;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.limbo.persistence.LimboPersistence;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.SpawnLoader;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import static fr.xephi.authme.settings.properties.LimboSettings.RESTORE_ALLOW_FLIGHT;
import static fr.xephi.authme.settings.properties.LimboSettings.RESTORE_FLY_SPEED;
import static fr.xephi.authme.settings.properties.LimboSettings.RESTORE_WALK_SPEED;

/**
 * Service for managing players that are in "limbo," a temporary state players are
 * put in which have joined but not yet logged in.
 */
public class LimboService {

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

    LimboService() {
    }

    /**
     * Creates a LimboPlayer for the given player and revokes all "limbo data" from the player.
     *
     * @param player the player to process
     * @param isRegistered whether or not the player is registered
     */
    public void createLimboPlayer(Player player, boolean isRegistered) {
        final String name = player.getName().toLowerCase();

        LimboPlayer limboFromDisk = persistence.getLimboPlayer(player);
        if (limboFromDisk != null) {
            ConsoleLogger.debug("LimboPlayer for `{0}` already exists on disk", name);
        }

        LimboPlayer existingLimbo = entries.remove(name);
        if (existingLimbo != null) {
            existingLimbo.clearTasks();
            ConsoleLogger.debug("LimboPlayer for `{0}` already present in memory", name);
        }

        Location location = spawnLoader.getPlayerLocationOrSpawn(player);
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
        return entries.get(name.toLowerCase());
    }

    /**
     * Returns whether there is a limbo player for the given name.
     *
     * @param name the name to check
     * @return true if present, false otherwise
     */
    public boolean hasLimboPlayer(String name) {
        return entries.containsKey(name.toLowerCase());
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
        String lowerName = player.getName().toLowerCase();
        LimboPlayer limbo = entries.remove(lowerName);

        if (limbo == null) {
            ConsoleLogger.debug("No LimboPlayer found for `{0}` - cannot restore", lowerName);
        } else {
            player.setOp(limbo.isOperator());
            settings.getProperty(RESTORE_ALLOW_FLIGHT).restoreAllowFlight(player, limbo);
            settings.getProperty(RESTORE_FLY_SPEED).restoreFlySpeed(player, limbo);
            settings.getProperty(RESTORE_WALK_SPEED).restoreWalkSpeed(player, limbo);
            limbo.clearTasks();
            ConsoleLogger.debug("Restored LimboPlayer stats for `{0}`", lowerName);
            persistence.removeLimboPlayer(player);
        }
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
        LimboPlayer limbo = entries.get(player.getName().toLowerCase());
        if (limbo == null) {
            ConsoleLogger.debug("No LimboPlayer found for `{0}`. Action: {1}", player.getName(), context);
        }
        return Optional.ofNullable(limbo);
    }
}
