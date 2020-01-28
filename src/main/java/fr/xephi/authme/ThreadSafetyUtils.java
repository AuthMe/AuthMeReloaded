package fr.xephi.authme;

import org.bukkit.Bukkit;

public final class ThreadSafetyUtils {

    private static boolean enabled = false;

    private ThreadSafetyUtils() {
    }

    public static void setEnabled(boolean enabled) {
        ThreadSafetyUtils.enabled = enabled;
    }

    public static void requireSync() {
        if (!enabled || Bukkit.isPrimaryThread()) {
            return;
        }
        System.err.println("Async call to sync method detected!");
        new Throwable().printStackTrace();
    }

    public static void shouldBeAsync() {
        if (!enabled || !Bukkit.isPrimaryThread()) {
            return;
        }
        System.err.println("Sync call to async method detected!");
        new Throwable().printStackTrace();
    }
}
