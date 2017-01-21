package fr.xephi.authme.util.lazytags;

import org.bukkit.entity.Player;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Represents a tag in a text that can be replaced with data (which may depend on the Player).
 */
public class Tag {

    private final String name;
    private final Function<Player, String> replacementFunction;

    /**
     * Constructor.
     *
     * @param name the tag (placeholder) that will be replaced
     * @param replacementFunction the function producing the replacement
     */
    public Tag(String name, Function<Player, String> replacementFunction) {
        this.name = name;
        this.replacementFunction = replacementFunction;
    }

    /**
     * Constructor.
     *
     * @param name the tag (placeholder) that will be replaced
     * @param replacementFunction supplier providing the text to replace the tag with
     */
    public Tag(String name, Supplier<String> replacementFunction) {
        this(name, p -> replacementFunction.get());
    }

    /**
     * @return the tag
     */
    public String getName() {
        return name;
    }

    /**
     * Returns the value to replace the tag with for the given player.
     *
     * @param player the player to evaluate the replacement for
     * @return the replacement
     */
    public String getValue(Player player) {
        return replacementFunction.apply(player);
    }
}
