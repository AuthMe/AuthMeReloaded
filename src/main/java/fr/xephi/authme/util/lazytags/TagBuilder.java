package fr.xephi.authme.util.lazytags;

import org.jetbrains.annotations.NotNull;

import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Utility class for creating tags.
 */
public final class TagBuilder {

    private TagBuilder() {
    }

    @NotNull
    public static <A> Tag<A> createTag(@NotNull String name, @NotNull Function<A, String> replacementFunction) {
        return new DependentTag<>(name, replacementFunction);
    }

    @NotNull
    public static <A> Tag<A> createTag(@NotNull String name, @NotNull Supplier<String> replacementFunction) {
        return new SimpleTag<>(name, replacementFunction);
    }

}
