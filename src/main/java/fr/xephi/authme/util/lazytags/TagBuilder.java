package fr.xephi.authme.util.lazytags;

import org.bukkit.entity.Player;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Utility class for creating tags.
 */
public final class TagBuilder {

    private TagBuilder() {
    }

    public static Tag createTag(String name, Function<Player, String> replacementFunction) {
        return new PlayerTag(name, replacementFunction);
    }

    public static Tag createTag(String name, Supplier<String> replacementFunction) {
        return new SimpleTag(name, replacementFunction);
    }
}
