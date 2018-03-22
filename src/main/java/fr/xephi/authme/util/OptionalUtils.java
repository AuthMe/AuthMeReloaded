package fr.xephi.authme.util;

import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Utility class for Optional operations.
 */
public final class OptionalUtils {

    // Utility class
    private OptionalUtils() {
    }

    /**
     * Handles the value of an Optional object.
     *
     * @param optional  the optional object
     * @param ifPresent the Function invoked when the Optional is filled
     * @param ifAbsent  the Supplier invoked when the Optional is empty
     * @param <T>       the Optional object type
     * @param <R>       the return type
     *
     * @return the object returned by the Function or Supplier
     */
    public static <T, R> R handleOptional(Optional<T> optional, Function<T, R> ifPresent, Supplier<R> ifAbsent) {
        if (optional.isPresent()) {
            return ifPresent.apply(optional.get());
        } else {
            return ifAbsent.get();
        }
    }

    /**
     * Handles the value of an Optional object.
     *
     * @param optional  the optional object
     * @param ifPresent the Function invoked when the Optional is filled
     * @param ifAbsent  the result when the Optional is empty
     * @param <T>       the Optional object type
     * @param <R>       the return type
     *
     * @return the object returned by the Function or Supplier
     */
    public static <T, R> R handleOptional(Optional<T> optional, Function<T, R> ifPresent, R ifAbsent) {
        if (optional.isPresent()) {
            return ifPresent.apply(optional.get());
        } else {
            return ifAbsent;
        }
    }

}
