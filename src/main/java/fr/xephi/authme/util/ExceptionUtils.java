package fr.xephi.authme.util;

import org.jetbrains.annotations.NotNull;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.IdentityHashMap;
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
    public static <T extends Throwable> T findThrowableInCause(@NotNull Class<T> wantedThrowableType,
                                                               @NotNull Throwable throwable) {
        Set<Throwable> visitedObjects = Collections.newSetFromMap(new IdentityHashMap<>());
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
    @NotNull
    public static String formatException(@NotNull Throwable th) {
        return "[" + th.getClass().getSimpleName() + "]: " + th.getMessage();
    }

    /**
     * Returns a string containing the result of {@link Throwable#toString()}, followed by the full, recursive
     * stack trace of {@code throwable}.
     *
     * @param throwable the throwable to precess
     * @return string containing the stacktrace
     */
    public static String getStackTraceAsString(Throwable throwable) {
        StringWriter stringWriter = new StringWriter();
        throwable.printStackTrace(new PrintWriter(stringWriter));
        return stringWriter.toString();
    }

}
