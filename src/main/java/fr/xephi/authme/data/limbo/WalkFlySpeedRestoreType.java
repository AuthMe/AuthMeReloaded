package fr.xephi.authme.data.limbo;

import org.bukkit.entity.Player;

/**
 * Possible types to restore the walk and fly speed from LimboPlayer
 * back to Bukkit Player.
 */
public enum WalkFlySpeedRestoreType {

    /** Restores from LimboPlayer to Player. */
    RESTORE {
        @Override
        public void restoreFlySpeed(Player player, LimboPlayer limbo) {
            player.setFlySpeed(limbo.getFlySpeed());
        }

        @Override
        public void restoreWalkSpeed(Player player, LimboPlayer limbo) {
            player.setWalkSpeed(limbo.getWalkSpeed());
        }
    },

    /** Restores from LimboPlayer, using the default speed if the speed on LimboPlayer is 0. */
    RESTORE_NO_ZERO {
        @Override
        public void restoreFlySpeed(Player player, LimboPlayer limbo) {
            float limboFlySpeed = limbo.getFlySpeed();
            player.setFlySpeed(limboFlySpeed > 0.01f ? limboFlySpeed : LimboPlayer.DEFAULT_FLY_SPEED);
        }

        @Override
        public void restoreWalkSpeed(Player player, LimboPlayer limbo) {
            float limboWalkSpeed = limbo.getWalkSpeed();
            player.setWalkSpeed(limboWalkSpeed > 0.01f ? limboWalkSpeed : LimboPlayer.DEFAULT_WALK_SPEED);
        }
    },

    /** Uses the max speed of Player (current speed) and the LimboPlayer. */
    MAX_RESTORE {
        @Override
        public void restoreFlySpeed(Player player, LimboPlayer limbo) {
            player.setFlySpeed(Math.max(player.getFlySpeed(), limbo.getFlySpeed()));
        }

        @Override
        public void restoreWalkSpeed(Player player, LimboPlayer limbo) {
            player.setWalkSpeed(Math.max(player.getWalkSpeed(), limbo.getWalkSpeed()));
        }
    },

    /** Always sets the default speed to the player. */
    DEFAULT {
        @Override
        public void restoreFlySpeed(Player player, LimboPlayer limbo) {
            player.setFlySpeed(LimboPlayer.DEFAULT_FLY_SPEED);
        }

        @Override
        public void restoreWalkSpeed(Player player, LimboPlayer limbo) {
            player.setWalkSpeed(LimboPlayer.DEFAULT_WALK_SPEED);
        }
    };

    /**
     * Restores the fly speed from Limbo to Player according to the restoration type.
     *
     * @param player the player to modify
     * @param limbo the limbo player to read from
     */
    public abstract void restoreFlySpeed(Player player, LimboPlayer limbo);

    /**
     * Restores the walk speed from Limbo to Player according to the restoration type.
     *
     * @param player the player to modify
     * @param limbo the limbo player to read from
     */
    public abstract void restoreWalkSpeed(Player player, LimboPlayer limbo);

}
