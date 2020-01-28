package fr.xephi.authme;

import org.bukkit.Bukkit;

public final class ThreadSafety {

    private static boolean enabled = false;

    private ThreadSafety() {
    }

    public static void setEnabled(boolean enabled) {
        ThreadSafety.enabled = enabled;
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
