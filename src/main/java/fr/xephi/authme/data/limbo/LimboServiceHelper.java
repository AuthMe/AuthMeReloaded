package fr.xephi.authme.data.limbo;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.LimboSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import static fr.xephi.authme.util.Utils.isCollectionEmpty;
import static java.util.stream.Collectors.toList;

/**
 * Helper class for the LimboService.
 */
class LimboServiceHelper {

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(LimboServiceHelper.class);

    @Inject
    private PermissionsManager permissionsManager;

    @Inject
    private Settings settings;

    /**
     * Creates a LimboPlayer with the given player's details.
     *
     * @param player       the player to process
     * @param isRegistered whether the player is registered
     * @param location     the player location
     * @return limbo player with the player's data
     */
    LimboPlayer createLimboPlayer(Player player, boolean isRegistered, Location location) {
        // For safety reasons an unregistered player should not have OP status after registration
        boolean isOperator = isRegistered && player.isOp();
        boolean flyEnabled = player.getAllowFlight();
        float walkSpeed = player.getWalkSpeed();
        float flySpeed = player.getFlySpeed();
        GameMode gameMode = player.getGameMode();
        Collection<UserGroup> playerGroups = permissionsManager.hasGroupSupport()
            ? permissionsManager.getGroups(player) : Collections.emptyList();

        List<String> groupNames = playerGroups.stream()
            .map(UserGroup::getGroupName)
            .collect(toList());

        logger.debug("Player `{0}` has groups `{1}`", player.getName(), String.join(", ", groupNames));
        return new LimboPlayer(location, isOperator, playerGroups, flyEnabled, walkSpeed, flySpeed, gameMode);
    }

    /**
     * Removes the data that is saved in a LimboPlayer from the player.
     * <p>
     * Note that teleportation on the player is performed by {@link fr.xephi.authme.service.TeleportationService} and
     * changing the permission group is handled by {@link fr.xephi.authme.data.limbo.AuthGroupHandler}.
     *
     * @param player the player to set defaults to
     */
    void revokeLimboStates(Player player) {
        player.setOp(false);
        settings.getProperty(LimboSettings.RESTORE_ALLOW_FLIGHT)
            .processPlayer(player);

        if (!settings.getProperty(RestrictionSettings.ALLOW_UNAUTHED_MOVEMENT)) {
            player.setFlySpeed(0.0f);
            player.setWalkSpeed(0.0f);
        }
    }

    /**
     * Merges two existing LimboPlayer instances of a player. Merging is done the following way:
     * <ul>
     *  <li><code>isOperator, allowFlight</code>: true if either limbo has true</li>
     *  <li><code>flySpeed, walkSpeed</code>: maximum value of either limbo player</li>
     *  <li><code>groups, location</code>: from old limbo if not empty/null, otherwise from new limbo</li>
     * </ul>
     *
     * @param newLimbo the new limbo player
     * @param oldLimbo the old limbo player
     * @return merged limbo player if both arguments are not null, otherwise the first non-null argument
     */
    LimboPlayer merge(LimboPlayer newLimbo, LimboPlayer oldLimbo) {
        if (newLimbo == null) {
            return oldLimbo;
        } else if (oldLimbo == null) {
            return newLimbo;
        }

        boolean isOperator = newLimbo.isOperator() || oldLimbo.isOperator();
        boolean canFly = newLimbo.isCanFly() || oldLimbo.isCanFly();
        float flySpeed = Math.max(newLimbo.getFlySpeed(), oldLimbo.getFlySpeed());
        float walkSpeed = Math.max(newLimbo.getWalkSpeed(), oldLimbo.getWalkSpeed());
        GameMode gameMode = Objects.isNull(newLimbo.getGameMode()) ? oldLimbo.getGameMode() : newLimbo.getGameMode();
        Collection<UserGroup> groups = getLimboGroups(oldLimbo.getGroups(), newLimbo.getGroups());
        Location location = firstNotNull(oldLimbo.getLocation(), newLimbo.getLocation());

        return new LimboPlayer(location, isOperator, groups, canFly, walkSpeed, flySpeed, gameMode);
    }

    private static Location firstNotNull(Location first, Location second) {
        return first == null ? second : first;
    }

    private Collection<UserGroup> getLimboGroups(Collection<UserGroup> oldLimboGroups,
                                                 Collection<UserGroup> newLimboGroups) {
        logger.debug("Limbo merge: new and old groups are `{0}` and `{1}`", newLimboGroups, oldLimboGroups);
        return isCollectionEmpty(oldLimboGroups) ? newLimboGroups : oldLimboGroups;
    }
}
