package fr.xephi.authme.util;

import fr.xephi.authme.AuthMe;
import org.bukkit.Server;

import java.util.logging.Logger;

/**
 * Wrapper for the retrieval of common singletons used throughout the application.
 * This class simply delegates the calls.
 */
public class Wrapper {

    private AuthMe authMe;

    public Wrapper(AuthMe authMe) {
        this.authMe = authMe;
    }

    public AuthMe getAuthMe() {
        return authMe;
    }

    public Server getServer() {
        return authMe.getServer();
    }

    public Logger getLogger() {
        return authMe.getLogger();
    }



}
