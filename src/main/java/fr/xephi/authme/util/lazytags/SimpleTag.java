package fr.xephi.authme.util.lazytags;

import org.jetbrains.annotations.NotNull;

import java.util.function.Supplier;

/**
 * Tag to be replaced that does not depend on an argument.
 *
 * @param <A> type of the argument (not used in this implementation)
 */
public class SimpleTag<A> implements Tag<A> {

    @NotNull
    private final String name;
    @NotNull
    private final Supplier<String> replacementFunction;

    public SimpleTag(@NotNull String name, @NotNull Supplier<String> replacementFunction) {
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
    public String getValue(A argument) {
        return replacementFunction.get();
    }

}
