package fr.xephi.authme.util.lazytags;

import java.util.function.Function;

/**
 * Replaceable tag whose value depends on an argument.
 *
 * @param <A> the argument type
 */
public class DependentTag<A> implements Tag<A> {

    private final String name;
    private final Function<A, String> replacementFunction;

    /**
     * Constructor.
     *
     * @param name the tag (placeholder) that will be replaced
     * @param replacementFunction the function producing the replacement
     */
    public DependentTag(String name, Function<A, String> replacementFunction) {
        this.name = name;
        this.replacementFunction = replacementFunction;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public String getValue(A argument) {
        return replacementFunction.apply(argument);
    }
}
