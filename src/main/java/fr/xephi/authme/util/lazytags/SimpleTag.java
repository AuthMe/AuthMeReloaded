package fr.xephi.authme.util.lazytags;

import java.util.function.Supplier;

/**
 * Tag to be replaced that does not depend on an argument.
 *
 * @param <A> type of the argument (not used in this implementation)
 */
public class SimpleTag<A> implements Tag<A> {

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
    public String getValue(A argument) {
        return replacementFunction.get();
    }
}
