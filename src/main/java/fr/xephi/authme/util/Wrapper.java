package fr.xephi.authme.util;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.output.Messages;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.scheduler.BukkitScheduler;

import java.io.File;
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

    /**
     * Package-private setter of the singleton field used for tests to inject a mock instance.
     *
     * @param wrapper The wrapper to use as singleton
     */
    static void setSingleton(Wrapper wrapper) {
        Wrapper.singleton = wrapper;
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

    public Messages getMessages() {
        return getAuthMe().getMessages();
    }

    public PlayerCache getPlayerCache() {
        return PlayerCache.getInstance();
    }

    /**
     * Return the folder containing plugin data via the AuthMe instance.
     *
     * @return The plugin data folder
     * @see AuthMe#getDataFolder()
     */
    public File getDataFolder() {
        return getAuthMe().getDataFolder();
    }

    public BukkitScheduler getScheduler() {
        return Bukkit.getScheduler();
    }



}
