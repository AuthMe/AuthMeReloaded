package fr.xephi.authme.util.lazytags;

import org.bukkit.entity.Player;

import java.util.function.Function;

/**
 * Replaceable tag whose value depends on the player.
 */
public class PlayerTag implements Tag {

    private final String name;
    private final Function<Player, String> replacementFunction;

    /**
     * Constructor.
     *
     * @param name the tag (placeholder) that will be replaced
     * @param replacementFunction the function producing the replacement
     */
    public PlayerTag(String name, Function<Player, String> replacementFunction) {
        this.name = name;
        this.replacementFunction = replacementFunction;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getValue(Player player) {
        return replacementFunction.apply(player);
    }
}
