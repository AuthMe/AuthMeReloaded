package fr.xephi.authme.util;

public class ServerUtils {

    /**
     * Check if the server implementation is based on Spigot
     * 
     * @return true if the implementation is based on Spigot
     */
    public static boolean isSpigot() {
        try {
            Class.forName("org.spigotmc.CustomTimingsHandler");
            return true;
        } catch (ClassNotFoundException ignored) {
        }
        return false;
    }
}
