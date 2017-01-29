package fr.xephi.authme.util.lazytags;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Utility class for creating tags.
 */
public final class TagBuilder {

    private TagBuilder() {
    }

    public static <A> Tag<A> createTag(String name, Function<A, String> replacementFunction) {
        return new DependentTag<>(name, replacementFunction);
    }

    public static <A> Tag<A> createTag(String name, Supplier<String> replacementFunction) {
        return new SimpleTag<>(name, replacementFunction);
    }
}
