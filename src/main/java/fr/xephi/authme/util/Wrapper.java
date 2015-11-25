package fr.xephi.authme.util;

import fr.xephi.authme.AuthMe;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.scheduler.BukkitScheduler;

import java.util.logging.Logger;

/**
 * Wrapper for the retrieval of common singletons used throughout the application.
 * This class simply delegates the calls.
 */
public class Wrapper {

    private static Wrapper singleton;

    /**
     * Package-private constructor for testing purposes to inject a mock instance.
     */
    Wrapper() {
    }

    public static Wrapper getInstance() {
        if (singleton == null) {
            singleton = new Wrapper();
        }
        return singleton;
    }

    public AuthMe getAuthMe() {
        return AuthMe.getInstance();
    }

    public Server getServer() {
        return getAuthMe().getServer();
    }

    public Logger getLogger() {
        return getAuthMe().getLogger();
    }

    public BukkitScheduler getScheduler() {
        return Bukkit.getScheduler();
    }



}
