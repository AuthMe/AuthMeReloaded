package fr.xephi.authme.api;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.crypts.HashResult;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

/**
 * Deprecated API of AuthMe. Please use {@link NewAPI} instead.
 */
@Deprecated
public class API {

    public static final String newline = System.getProperty("line.separator");
    public static AuthMe instance;
    private static PasswordSecurity passwordSecurity;

    /**
     * Constructor for the deprecated API.
     *
     * @param instance AuthMe
     */
    @Deprecated
    public API(AuthMe instance) {
        API.instance = instance;
        passwordSecurity = instance.getPasswordSecurity();
    }

    /**
     * Hook into AuthMe.
     *
     * @return AuthMe instance
     */
    @Deprecated
    public static AuthMe hookAuthMe() {
        if (instance != null) {
            return instance;
        }
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("AuthMe");
        if (plugin == null || !(plugin instanceof AuthMe)) {
            return null;
        }
        instance = (AuthMe) plugin;
        return instance;
    }

    /**
     * Return whether the player is authenticated.
     *
     * @param player The player to verify
     * @return true if the player is authenticated
     */
    @Deprecated
    public static boolean isAuthenticated(Player player) {
        return PlayerCache.getInstance().isAuthenticated(player.getName());
    }

    /**
     * Return whether the player is unrestricted.
     *
     * @param player The player to verify
     * @return true if the player is unrestricted
     */
    @Deprecated
    public static boolean isUnrestricted(Player player) {
        return Utils.isUnrestricted(player);
    }

    @Deprecated
    public static Location getLastLocation(Player player) {
        try {
            PlayerAuth auth = PlayerCache.getInstance().getAuth(player.getName().toLowerCase());

            if (auth != null) {
                Location loc = new Location(Bukkit.getWorld(auth.getWorld()), auth.getQuitLocX(), auth.getQuitLocY(), auth.getQuitLocZ());
                return loc;
            } else {
                return null;
            }

        } catch (NullPointerException ex) {
            return null;
        }
    }

    @Deprecated
    public static void setPlayerInventory(Player player, ItemStack[] content,
                                          ItemStack[] armor) {
        try {
            player.getInventory().setContents(content);
            player.getInventory().setArmorContents(armor);
        } catch (NullPointerException ignored) {
        }
    }

    /**
     * Check whether the given player name is registered.
     *
     * @param playerName The player name to verify
     * @return true if player is registered
     */
    @Deprecated
    public static boolean isRegistered(String playerName) {
        String player = playerName.toLowerCase();
        return instance.getDataSource().isAuthAvailable(player);
    }

    /**
     * Check the password for the given player.
     *
     * @param playerName      The name of the player
     * @param passwordToCheck The password to check
     * @return true if the password is correct, false otherwise
     */
    @Deprecated
    public static boolean checkPassword(String playerName,
                                        String passwordToCheck) {
        if (!isRegistered(playerName))
            return false;
        String player = playerName.toLowerCase();
        PlayerAuth auth = instance.getDataSource().getAuth(player);
        return passwordSecurity.comparePassword(passwordToCheck, auth.getHash(), playerName);
    }

    /**
     * Register a player.
     *
     * @param playerName The name of the player
     * @param password   The password
     * @return true if the player was registered correctly
     */
    @Deprecated
    public static boolean registerPlayer(String playerName, String password) {
        String name = playerName.toLowerCase();
        HashResult hashResult = passwordSecurity.computeHash(Settings.getPasswordHash, password, name);
        if (isRegistered(name)) {
            return false;
        }
        PlayerAuth auth = PlayerAuth.builder()
            .name(name)
            .hash(hashResult.getHash())
            .lastLogin(0)
            .realName(playerName)
            .build();
        return instance.getDataSource().saveAuth(auth);
    }

    /**
     * Force a player to log in.
     *
     * @param player The player to log in
     */
    @Deprecated
    public static void forceLogin(Player player) {
        instance.getManagement().performLogin(player, "dontneed", true);
    }

    @Deprecated
    public AuthMe getPlugin() {
        return instance;
    }

    /**
     * Check whether the player is an NPC.
     *
     * @param player The player to verify
     * @return true if player is an npc
     */
    @Deprecated
    public boolean isNPC(Player player) {
        return Utils.isNPC(player);
    }

}
