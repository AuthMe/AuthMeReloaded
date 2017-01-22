package fr.xephi.authme.util.lazytags;

import org.bukkit.entity.Player;

/**
 * Represents a tag in a text to be replaced with a value (which may depend on the Player).
 */
public interface Tag {

    /**
     * @return the tag to replace
     */
    String getName();

    /**
     * Returns the value to replace the tag with for the given player.
     *
     * @param player the player to evaluate the replacement for
     * @return the replacement
     */
    String getValue(Player player);
}
