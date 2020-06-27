package fr.xephi.authme.util.lazytags;

import org.jetbrains.annotations.NotNull;

/**
 * Represents a tag in a text to be replaced with a value (which may depend on some argument).
 *
 * @param <A> argument type the replacement may depend on
 */
public interface Tag<A> {

    /**
     * @return the tag to replace
     */
    @NotNull
    String getName();

    /**
     * Returns the value to replace the tag with for the given argument.
     *
     * @param argument the argument to evaluate the replacement for
     * @return the replacement
     */
    @NotNull
    String getValue(A argument);

}
