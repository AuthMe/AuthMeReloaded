package fr.xephi.authme.data.limbo;

import org.bukkit.entity.Player;

/**
 * Possible types to restore the "allow flight" property
 * from LimboPlayer to Bukkit Player.
 */
public enum AllowFlightRestoreType {

    /** Set value from LimboPlayer to Player. */
    RESTORE {
        @Override
        public void restoreAllowFlight(Player player, LimboPlayer limbo) {
            player.setAllowFlight(limbo.isCanFly());
        }
    },

    /** Always set flight enabled to true. */
    ENABLE {
        @Override
        public void restoreAllowFlight(Player player, LimboPlayer limbo) {
            player.setAllowFlight(true);
        }
    },

    /** Always set flight enabled to false. */
    DISABLE {
        @Override
        public void restoreAllowFlight(Player player, LimboPlayer limbo) {
            player.setAllowFlight(false);
        }
    },

    /** The user's flight handling is not modified. */
    NOTHING {
        @Override
        public void restoreAllowFlight(Player player, LimboPlayer limbo) {
            // noop
        }

        @Override
        public void processPlayer(Player player) {
            // noop
        }
    };

    /**
     * Restores the "allow flight" property from the LimboPlayer to the Player.
     * This method behaves differently for each restoration type.
     *
     * @param player the player to modify
     * @param limbo the limbo player to read from
     */
    public abstract void restoreAllowFlight(Player player, LimboPlayer limbo);

    /**
     * Processes the player when a LimboPlayer instance is created based on him. Typically this
     * method revokes the {@code allowFlight} property to be restored again later.
     *
     * @param player the player to process
     */
    public void processPlayer(Player player) {
        player.setAllowFlight(false);
    }
}
