package fr.xephi.authme.util.lazytags;

import org.bukkit.entity.Player;

import java.util.function.Supplier;

/**
 * Tag to be replaced that does not depend on the player.
 */
public class SimpleTag implements Tag {

    private final String name;
    private final Supplier<String> replacementFunction;

    public SimpleTag(String name, Supplier<String> replacementFunction) {
        this.name = name;
        this.replacementFunction = replacementFunction;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getValue(Player player) {
        return replacementFunction.get();
    }
}
