package fr.xephi.authme.util;

import org.bukkit.Bukkit;

/**
 * Utility class that provides static methods to ensure that methods are called from the right thread.
 */
public final class BukkitThreadSafety {

    private static boolean enabled = false;

    private BukkitThreadSafety() {
    }

    /**
     * Enables/disables the bukkit thread-safety warnings.
     *
     * @param enabled true if the warnings should be enabled, false otherwise.
     */
    public static void setEnabled(boolean enabled) {
        BukkitThreadSafety.enabled = enabled;
    }

    /**
     * Prints a warning if called by an async thread (not the main server thread).
     */
    public static void requireSync() {
        if (!enabled || Bukkit.isPrimaryThread()) {
            return;
        }
        System.err.println("Async call to sync method detected!");
        new Throwable().printStackTrace();
    }

    /**
     * Prints a warning if called by the main server thread.
     */
    public static void shouldBeAsync() {
        if (!enabled || !Bukkit.isPrimaryThread()) {
            return;
        }
        System.err.println("Sync call to async method detected!");
        new Throwable().printStackTrace();
    }
}
