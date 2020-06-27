package fr.xephi.authme.util.lazytags;

import org.jetbrains.annotations.NotNull;

import java.util.function.Function;

/**
 * Replaceable tag whose value depends on an argument.
 *
 * @param <A> the argument type
 */
public class DependentTag<A> implements Tag<A> {

    @NotNull
    private final String name;
    @NotNull
    private final Function<A, String> replacementFunction;

    /**
     * Constructor.
     *
     * @param name the tag (placeholder) that will be replaced
     * @param replacementFunction the function producing the replacement
     */
    public DependentTag(@NotNull String name,@NotNull Function<A, String> replacementFunction) {
        this.name = name;
        this.replacementFunction = replacementFunction;
    }

    @Override
    @NotNull
    public String getName() {
        return name;
    }

    @Override
    @NotNull
    public String getValue(@NotNull A argument) {
        return replacementFunction.apply(argument);
    }

}
