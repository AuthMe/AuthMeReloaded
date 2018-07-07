package fr.xephi.authme.util;

import com.google.common.collect.Sets;

import java.util.Set;

/**
 * Utilities for exceptions.
 */
public final class ExceptionUtils {

    private ExceptionUtils() {
    }

    /**
     * Returns the first throwable of the given {@code wantedThrowableType} by visiting the provided
     * throwable and its causes recursively.
     *
     * @param wantedThrowableType the throwable type to find
     * @param throwable the throwable to start with
     * @param <T> the desired throwable subtype
     * @return the first throwable found of the given type, or null if none found
     */
    public static <T extends Throwable> T findThrowableInCause(Class<T> wantedThrowableType, Throwable throwable) {
        Set<Throwable> visitedObjects = Sets.newIdentityHashSet();
        Throwable currentThrowable = throwable;
        while (currentThrowable != null && !visitedObjects.contains(currentThrowable)) {
            if (wantedThrowableType.isInstance(currentThrowable)) {
                return wantedThrowableType.cast(currentThrowable);
            }
            visitedObjects.add(currentThrowable);
            currentThrowable = currentThrowable.getCause();
        }
        return null;
    }

    /**
     * Format the information from a Throwable as string, retaining the type and its message.
     *
     * @param th the throwable to process
     * @return string with the type of the Throwable and its message, e.g. "[IOException]: Could not open stream"
     */
    public static String formatException(Throwable th) {
        return "[" + th.getClass().getSimpleName() + "]: " + th.getMessage();
    }
}
