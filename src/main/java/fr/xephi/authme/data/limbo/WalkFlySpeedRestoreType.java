package fr.xephi.authme.data.limbo;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import org.bukkit.entity.Player;

/**
 * Possible types to restore the walk and fly speed from LimboPlayer
 * back to Bukkit Player.
 */
public enum WalkFlySpeedRestoreType {

    /**
     * Restores from LimboPlayer to Player.
     */
    RESTORE {
        @Override
        public void restoreFlySpeed(Player player, LimboPlayer limbo) {
            logger.debug(() -> "Restoring fly speed for LimboPlayer " + player.getName() + " to "
                + limbo.getFlySpeed() + " (RESTORE mode)");
            player.setFlySpeed(limbo.getFlySpeed());
        }

        @Override
        public void restoreWalkSpeed(Player player, LimboPlayer limbo) {
            logger.debug(() -> "Restoring walk speed for LimboPlayer " + player.getName() + " to "
                + limbo.getWalkSpeed() + " (RESTORE mode)");
            player.setWalkSpeed(limbo.getWalkSpeed());
        }
    },

    /**
     * Restores from LimboPlayer, using the default speed if the speed on LimboPlayer is 0.
     */
    RESTORE_NO_ZERO {
        @Override
        public void restoreFlySpeed(Player player, LimboPlayer limbo) {
            float limboFlySpeed = limbo.getFlySpeed();
            if (limboFlySpeed > 0.01f) {
                logger.debug(() -> "Restoring fly speed for LimboPlayer " + player.getName() + " to "
                    + limboFlySpeed + " (RESTORE_NO_ZERO mode)");
                player.setFlySpeed(limboFlySpeed);
            } else {
                logger.debug(() -> "Restoring fly speed for LimboPlayer " + player.getName()
                    + " to DEFAULT, it was 0! (RESTORE_NO_ZERO mode)");
                player.setFlySpeed(LimboPlayer.DEFAULT_FLY_SPEED);
            }
        }

        @Override
        public void restoreWalkSpeed(Player player, LimboPlayer limbo) {
            float limboWalkSpeed = limbo.getWalkSpeed();
            if (limboWalkSpeed > 0.01f) {
                logger.debug(() -> "Restoring walk speed for LimboPlayer " + player.getName() + " to "
                    + limboWalkSpeed + " (RESTORE_NO_ZERO mode)");
                player.setWalkSpeed(limboWalkSpeed);
            } else {
                logger.debug(() -> "Restoring walk speed for LimboPlayer " + player.getName() + ""
                    + " to DEFAULT, it was 0! (RESTORE_NO_ZERO mode)");
                player.setWalkSpeed(LimboPlayer.DEFAULT_WALK_SPEED);
            }
        }
    },

    /**
     * Uses the max speed of Player (current speed) and the LimboPlayer.
     */
    MAX_RESTORE {
        @Override
        public void restoreFlySpeed(Player player, LimboPlayer limbo) {
            float newSpeed = Math.max(player.getFlySpeed(), limbo.getFlySpeed());
            logger.debug(() -> "Restoring fly speed for LimboPlayer " + player.getName() + " to " + newSpeed
                + " (Current: " + player.getFlySpeed() + ", Limbo: " + limbo.getFlySpeed() + ") (MAX_RESTORE mode)");
            player.setFlySpeed(newSpeed);
        }

        @Override
        public void restoreWalkSpeed(Player player, LimboPlayer limbo) {
            float newSpeed = Math.max(player.getWalkSpeed(), limbo.getWalkSpeed());
            logger.debug(() -> "Restoring walk speed for LimboPlayer " + player.getName() + " to " + newSpeed
                + " (Current: " + player.getWalkSpeed() + ", Limbo: " + limbo.getWalkSpeed() + ") (MAX_RESTORE mode)");
            player.setWalkSpeed(newSpeed);
        }
    },

    /**
     * Always sets the default speed to the player.
     */
    DEFAULT {
        @Override
        public void restoreFlySpeed(Player player, LimboPlayer limbo) {
            logger.debug(() -> "Restoring fly speed for LimboPlayer " + player.getName()
                + " to DEFAULT (DEFAULT mode)");
            player.setFlySpeed(LimboPlayer.DEFAULT_FLY_SPEED);
        }

        @Override
        public void restoreWalkSpeed(Player player, LimboPlayer limbo) {
            logger.debug(() -> "Restoring walk speed for LimboPlayer " + player.getName()
                + " to DEFAULT (DEFAULT mode)");
            player.setWalkSpeed(LimboPlayer.DEFAULT_WALK_SPEED);
        }
    };

    private static final ConsoleLogger logger = ConsoleLoggerFactory.get(WalkFlySpeedRestoreType.class);

    /**
     * Restores the fly speed from Limbo to Player according to the restoration type.
     *
     * @param player the player to modify
     * @param limbo  the limbo player to read from
     */
    public abstract void restoreFlySpeed(Player player, LimboPlayer limbo);

    /**
     * Restores the walk speed from Limbo to Player according to the restoration type.
     *
     * @param player the player to modify
     * @param limbo  the limbo player to read from
     */
    public abstract void restoreWalkSpeed(Player player, LimboPlayer limbo);

}
