package fr.xephi.authme.util;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.regex.Pattern;

/**
 * Utility class for various operations used in the codebase.
 */
public final class Utils {

    /** Number of milliseconds in a minute. */
    public static final long MILLIS_PER_MINUTE = 60_000L;

    /** A pattern that matches anything. */
    public static final Pattern MATCH_ANYTHING_PATTERN = Pattern.compile(".*?");

    // Utility class
    private Utils() {
    }

    /**
     * Compile Pattern sneaky without throwing Exception.
     *
     * @param pattern  pattern string to compile
     * @param fallback the fallback pattern supplier
     *
     * @return the given regex compiled into Pattern object.
     */
    public static Pattern safePatternCompile(@NotNull String pattern, @NotNull Function<String, Pattern> fallback) {
        try {
            return Pattern.compile(pattern);
        } catch (Exception e) {
            return fallback.apply(pattern);
        }
    }

    /**
     * Returns whether the class exists in the current class loader.
     *
     * @param className the class name to check
     *
     * @return true if the class is loaded, false otherwise
     */
    public static boolean isClassLoaded(@NotNull String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Null-safe way to check whether a collection is empty or not.
     *
     * @param coll The collection to verify
     * @return True if the collection is null or empty, false otherwise
     */
    public static boolean isCollectionEmpty(Collection<?> coll) {
        return coll == null || coll.isEmpty();
    }

    /**
     * Returns whether the given email is empty or equal to the standard "undefined" email address.
     *
     * @param email the email to check
     *
     * @return true if the email is empty
     */
    public static boolean isEmailEmpty(String email) {
        return StringUtils.isEmpty(email) || "your@email.com".equalsIgnoreCase(email);
    }

    /**
     * Tries to parse a String as an Integer, returns null on fail.
     *
     * @return the parsed Integer value
     */
    public static Integer tryInteger(@NotNull String string) {
        try {
            return Integer.parseInt(string);
        } catch (NumberFormatException e) {
            return null;
        }
    }

}
