package fr.xephi.authme.api;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.util.Utils;

/**
 * The current API of AuthMe.
 */
public class NewAPI {

    public static NewAPI singleton;
    public final AuthMe plugin;

    /**
     * Constructor for NewAPI.
     *
     * @param plugin AuthMe
     */
    public NewAPI(AuthMe plugin) {
        this.plugin = plugin;
    }

    /**
     * Constructor for NewAPI.
     *
     * @param server Server
     */
    public NewAPI(Server server) {
        this.plugin = (AuthMe) server.getPluginManager().getPlugin("AuthMe");
    }

    /**
     * Hook into AuthMe
     *
     * @return The API object
     */
    public static NewAPI getInstance() {
        if (singleton != null) {
            return singleton;
        }
        Plugin p = Bukkit.getServer().getPluginManager().getPlugin("AuthMe");
        if (p == null || !(p instanceof AuthMe)) {
            return null;
        }
        AuthMe authme = (AuthMe) p;
        singleton = new NewAPI(authme);
        return singleton;
    }

    /**
     * Return the plugin instance.
     *
     * @return The AuthMe instance
     */
    public AuthMe getPlugin() {
        return plugin;
    }

    /**
     * Return whether the given player is authenticated.
     *
     * @param player The player to verify
     *
     * @return true if the player is authenticated
     */
    public boolean isAuthenticated(Player player) {
        return PlayerCache.getInstance().isAuthenticated(player.getName());
    }

    /**
     * @param player a Player
     *
     * @return true if player is a npc
     */
    public boolean isNPC(Player player) {
        return Utils.isNPC(player);
    }

    /**
     * @param player a Player
     *
     * @return true if the player is unrestricted
     */
    public boolean isUnrestricted(Player player) {
        return Utils.isUnrestricted(player);
    }

    /**
     * Get the last location of a player.
     *
     * @param player Player The player to process
     *
     * @return Location The location of the player
     */
    public Location getLastLocation(Player player) {
        try {
            PlayerAuth auth = PlayerCache.getInstance().getAuth(player.getName());

            if (auth != null) {
                return new Location(Bukkit.getWorld(auth.getWorld()), auth.getQuitLocX(), auth.getQuitLocY(), auth.getQuitLocZ());
            } else {
                return null;
            }

        } catch (NullPointerException ex) {
            return null;
        }
    }

    /**
     * Return whether the player is registered.
     *
     * @param playerName The player name to check
     *
     * @return true if player is registered, false otherwise
     */
    public boolean isRegistered(String playerName) {
        String player = playerName.toLowerCase();
        return plugin.getDataSource().isAuthAvailable(player);
    }

    /**
     * Check the password for the given player.
     *
     * @param playerName      The player to check the password for
     * @param passwordToCheck The password to check
     *
     * @return true if the password is correct, false otherwise
     */
    public boolean checkPassword(String playerName, String passwordToCheck) {
        return isRegistered(playerName) && plugin.getPasswordSecurity().comparePassword(passwordToCheck, playerName);
    }

    /**
     * Register a player.
     *
     * @param playerName The player to register
     * @param password   The password to register the player with
     *
     * @return true if the player was registered successfully
     */
    public boolean registerPlayer(String playerName, String password) {
        String name = playerName.toLowerCase();
        HashedPassword result = plugin.getPasswordSecurity().computeHash(password, name);
        if (isRegistered(name)) {
            return false;
        }
        PlayerAuth auth = PlayerAuth.builder()
            .name(name)
            .password(result)
            .realName(playerName)
            .build();
        return plugin.getDataSource().saveAuth(auth);
    }

    /**
     * Force a player to login.
     *
     * @param player The player to log in
     */
    public void forceLogin(Player player) {
        plugin.getManagement().performLogin(player, "dontneed", true);
    }

    /**
     * Force a player to logout.
     *
     * @param player The player to log out
     */
    public void forceLogout(Player player) {
        plugin.getManagement().performLogout(player);
    }

    /**
     * Force a player to register.
     *
     * @param player   The player to register
     * @param password The password to use
     */
    public void forceRegister(Player player, String password) {
        plugin.getManagement().performRegister(player, password, null);
    }

    /**
     * Force a player to unregister.
     *
     * @param player The player to unregister
     */
    public void forceUnregister(Player player) {
        plugin.getManagement().performUnregister(player, "", true);
    }
}
