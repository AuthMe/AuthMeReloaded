package fr.xephi.authme.data.limbo;

import org.bukkit.entity.Player;

import java.util.function.Function;

/**
 * Possible types to restore the "allow flight" property
 * from LimboPlayer to Bukkit Player.
 */
public enum AllowFlightRestoreType {

    /** Set value from LimboPlayer to Player. */
    RESTORE(LimboPlayer::isCanFly),

    /** Always set flight enabled to true. */
    ENABLE(l -> true),

    /** Always set flight enabled to false. */
    DISABLE(l -> false);

    private final Function<LimboPlayer, Boolean> valueGetter;

    /**
     * Constructor.
     *
     * @param valueGetter function with which the value to set on the player can be retrieved
     */
    AllowFlightRestoreType(Function<LimboPlayer, Boolean> valueGetter) {
        this.valueGetter = valueGetter;
    }

    /**
     * Restores the "allow flight" property from the LimboPlayer to the Player.
     * This method behaves differently for each restoration type.
     *
     * @param player the player to modify
     * @param limbo the limbo player to read from
     */
    public void restoreAllowFlight(Player player, LimboPlayer limbo) {
        player.setAllowFlight(valueGetter.apply(limbo));
    }
}
