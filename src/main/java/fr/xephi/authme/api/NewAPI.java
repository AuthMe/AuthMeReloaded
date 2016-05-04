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

import javax.inject.Inject;

/**
 * The current API of AuthMe. Recommended method of retrieving the API object:
 * <code>
 * NewAPI authmeApi = NewAPI.getInstance();
 * </code>
 */
public class NewAPI {

    public static NewAPI singleton;
    public final AuthMe plugin;

    /**
     * Constructor for NewAPI.
     *
     * @param plugin The AuthMe plugin instance
     */
    @Inject
    public NewAPI(AuthMe plugin) {
        this.plugin = plugin;
    }

    /**
     * Constructor for NewAPI.
     *
     * @param server The server instance
     */
    public NewAPI(Server server) {
        this.plugin = (AuthMe) server.getPluginManager().getPlugin("AuthMe");
    }

    /**
     * Get the API object for AuthMe.
     *
     * @return The API object, or null if the AuthMe plugin instance could not be retrieved
     * from the server environment
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
     * Gather the version number of the plugin.
     * This can be used to determine whether certain API features are available or not.
     *
     * @return Plugin version identifier as a string.
     */
    public String getPluginVersion() {
        return AuthMe.getPluginVersion();
    }

    /**
     * Return whether the given player is authenticated.
     *
     * @param player The player to verify
     * @return true if the player is authenticated
     */
    public boolean isAuthenticated(Player player) {
        return PlayerCache.getInstance().isAuthenticated(player.getName());
    }

    /**
     * Check whether the given player is an NPC.
     *
     * @param player The player to verify
     * @return true if the player is an npc
     */
    public boolean isNPC(Player player) {
        return plugin.getPluginHooks().isNpc(player);
    }

    /**
     * Check whether the given player is unrestricted. For such players, AuthMe will not require
     * them to authenticate.
     *
     * @param player The player to verify
     * @return true if the player is unrestricted
     * @see fr.xephi.authme.settings.properties.RestrictionSettings#UNRESTRICTED_NAMES
     */
    public boolean isUnrestricted(Player player) {
        return Utils.isUnrestricted(player);
    }

    /**
     * Get the last location of a player.
     *
     * @param player The player to process
     * @return Location The location of the player
     */
    public Location getLastLocation(Player player) {
        PlayerAuth auth = PlayerCache.getInstance().getAuth(player.getName());
        if (auth != null) {
            return new Location(Bukkit.getWorld(auth.getWorld()), auth.getQuitLocX(), auth.getQuitLocY(), auth.getQuitLocZ());
        }
        return null;
    }

    /**
     * Return whether the player is registered.
     *
     * @param playerName The player name to check
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
     * @return true if the password is correct, false otherwise
     */
    public boolean checkPassword(String playerName, String passwordToCheck) {
        return isRegistered(playerName) && plugin.getPasswordSecurity().comparePassword(passwordToCheck, playerName);
    }

    /**
     * Register a player with the given password.
     *
     * @param playerName The player to register
     * @param password   The password to register the player with
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
     * Force a player to login, i.e. the player is logged in without needing his password.
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
     * Register a player with the given password.
     *
     * @param player   The player to register
     * @param password The password to use
     */
    public void forceRegister(Player player, String password) {
        plugin.getManagement().performRegister(player, password, null);
    }

    /**
     * Unregister a player from AuthMe.
     *
     * @param player The player to unregister
     */
    public void forceUnregister(Player player) {
        plugin.getManagement().performUnregister(player, "", true);
    }
}
