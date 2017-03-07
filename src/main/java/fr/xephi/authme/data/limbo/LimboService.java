package fr.xephi.authme.data.limbo;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.SpawnLoader;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Service for managing players that are in "limbo," a temporary state players are
 * put in which have joined but not yet logged in yet.
 */
public class LimboService {

    private final Map<String, LimboPlayer> entries = new ConcurrentHashMap<>();

    @Inject
    private SpawnLoader spawnLoader;

    @Inject
    private PermissionsManager permissionsManager;

    @Inject
    private Settings settings;

    @Inject
    private LimboPlayerTaskManager taskManager;

    LimboService() {
    }


    /**
     * Restores the limbo data and subsequently deletes the entry.
     * <p>
     * Note that teleportation on the player is performed by {@link fr.xephi.authme.service.TeleportationService} and
     * changing the permission group is handled by {@link fr.xephi.authme.permission.AuthGroupHandler}.
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
            player.setAllowFlight(limbo.isCanFly());
            float walkSpeed = limbo.getWalkSpeed();
            float flySpeed = limbo.getFlySpeed();
            // Reset the speed value if it was 0
            if (walkSpeed < 0.01f) {
                walkSpeed = LimboPlayer.DEFAULT_WALK_SPEED;
            }
            if (flySpeed < 0.01f) {
                flySpeed = LimboPlayer.DEFAULT_FLY_SPEED;
            }
            player.setWalkSpeed(walkSpeed);
            player.setFlySpeed(flySpeed);
            limbo.clearTasks();
            ConsoleLogger.debug("Restored LimboPlayer stats for `{0}`", lowerName);
        }
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
     * Creates a LimboPlayer for the given player and revokes all "limbo data" from the player.
     *
     * @param player the player to process
     * @param isRegistered whether or not the player is registered
     */
    public void createLimboPlayer(Player player, boolean isRegistered) {
        final String name = player.getName().toLowerCase();

        LimboPlayer existingLimbo = entries.remove(name);
        if (existingLimbo != null) {
            existingLimbo.clearTasks();
            ConsoleLogger.debug("LimboPlayer for `{0}` was already present", name);
        }

        LimboPlayer limboPlayer = newLimboPlayer(player);
        taskManager.registerMessageTask(player, limboPlayer, isRegistered);
        taskManager.registerTimeoutTask(player, limboPlayer);
        revokeLimboStates(player);
        entries.put(name, limboPlayer);
    }

    /**
     * Creates new tasks for the given player and cancels the old ones for a newly registered player.
     * This resets his time to log in (TimeoutTask) and updates the messages he is shown (MessageTask).
     *
     * @param player the player to reset the tasks for
     */
    public void replaceTasksAfterRegistration(Player player) {
        getLimboOrLogError(player, "reset tasks")
            .ifPresent(limbo -> {
                taskManager.registerTimeoutTask(player, limbo);
                taskManager.registerMessageTask(player, limbo, true);
            });
    }

    /**
     * Resets the message task associated with the player's LimboPlayer.
     *
     * @param player the player to set a new message task for
     * @param isRegistered whether or not the player is registered
     */
    public void resetMessageTask(Player player, boolean isRegistered) {
        getLimboOrLogError(player, "reset message task")
            .ifPresent(limbo -> taskManager.registerMessageTask(player, limbo, isRegistered));
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
     * Creates a LimboPlayer with the given player's details.
     *
     * @param player the player to process
     * @return limbo player with the player's data
     */
    private LimboPlayer newLimboPlayer(Player player) {
        Location location = spawnLoader.getPlayerLocationOrSpawn(player);
        boolean isOperator = player.isOp();
        boolean flyEnabled = player.getAllowFlight();
        float walkSpeed = player.getWalkSpeed();
        float flySpeed = player.getFlySpeed();
        String playerGroup = "";
        if (permissionsManager.hasGroupSupport()) {
            playerGroup = permissionsManager.getPrimaryGroup(player);
        }
        ConsoleLogger.debug("Player `{0}` has primary group `{1}`", player.getName(), playerGroup);

        return new LimboPlayer(location, isOperator, playerGroup, flyEnabled, walkSpeed, flySpeed);
    }

    /**
     * Removes the data that is saved in a LimboPlayer from the player.
     * <p>
     * Note that teleportation on the player is performed by {@link fr.xephi.authme.service.TeleportationService} and
     * changing the permission group is handled by {@link fr.xephi.authme.permission.AuthGroupHandler}.
     *
     * @param player the player to set defaults to
     */
    private void revokeLimboStates(Player player) {
        player.setOp(false);
        player.setAllowFlight(false);

        if (!settings.getProperty(RestrictionSettings.ALLOW_UNAUTHED_MOVEMENT)
            && settings.getProperty(RestrictionSettings.REMOVE_SPEED)) {
            player.setFlySpeed(0.0f);
            player.setWalkSpeed(0.0f);
        }
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
