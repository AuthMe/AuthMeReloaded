package fr.xephi.authme;

import org.bukkit.Bukkit;

public final class ThreadSafetyUtils {

    private ThreadSafetyUtils() {
    }

    public static void requireSync() {
        if (Bukkit.isPrimaryThread()) {
            return;
        }
        System.err.println("Async call to sync method detected!");
        new Throwable().printStackTrace();
    }

    public static void shouldBeAsync() {
        if (!Bukkit.isPrimaryThread()) {
            return;
        }
        System.err.println("Sync call to async method detected!");
        new Throwable().printStackTrace();
    }
}
