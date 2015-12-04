package fr.xephi.authme.api;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.util.Utils;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import java.security.NoSuchAlgorithmException;

/**
 */
public class API {

    public static final String newline = System.getProperty("line.separator");
    public static AuthMe instance;

    /**
     * Constructor for API.
     *
     * @param instance AuthMe
     */
    @Deprecated
    public API(AuthMe instance) {
        API.instance = instance;
    }

    /**
     * Hook into AuthMe
     *
     * @return AuthMe instance
     */
    @Deprecated
    public static AuthMe hookAuthMe() {
        if (instance != null)
            return instance;
        Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("AuthMe");
        if (plugin == null || !(plugin instanceof AuthMe)) {
            return null;
        }
        instance = (AuthMe) plugin;
        return instance;
    }

    /**
     * @param player
     *
     * @return true if player is authenticate
     */
    @Deprecated
    public static boolean isAuthenticated(Player player) {
        return PlayerCache.getInstance().isAuthenticated(player.getName());
    }

    /**
     * @param player
     *
     * @return true if the player is unrestricted
     */
    @Deprecated
    public static boolean isUnrestricted(Player player) {
        return Utils.isUnrestricted(player);
    }

    /**
     * Method getLastLocation.
     *
     * @param player Player
     *
     * @return Location
     */
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

    /**
     * Method setPlayerInventory.
     *
     * @param player  Player
     * @param content ItemStack[]
     * @param armor   ItemStack[]
     */
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
     * @param playerName
     *
     * @return true if player is registered
     */
    @Deprecated
    public static boolean isRegistered(String playerName) {
        String player = playerName.toLowerCase();
        return instance.database.isAuthAvailable(player);
    }

    /**
     * @param playerName      String
     * @param passwordToCheck String
     *
     * @return true if the password is correct , false else
     */
    @Deprecated
    public static boolean checkPassword(String playerName,
                                        String passwordToCheck) {
        if (!isRegistered(playerName))
            return false;
        String player = playerName.toLowerCase();
        PlayerAuth auth = instance.database.getAuth(player);
        try {
            return PasswordSecurity.comparePasswordWithHash(passwordToCheck, auth.getHash(), playerName);
        } catch (NoSuchAlgorithmException e) {
            return false;
        }
    }

    /**
     * Register a player
     *
     * @param playerName String
     * @param password   String
     *
     * @return true if the player is register correctly
     */
    @Deprecated
    public static boolean registerPlayer(String playerName, String password) {
        try {
            String name = playerName.toLowerCase();
            String hash = PasswordSecurity.getHash(Settings.getPasswordHash, password, name);
            if (isRegistered(name)) {
                return false;
            }
            PlayerAuth auth = new PlayerAuth(name, hash, "198.18.0.1", 0, "your@email.com", playerName);
            return instance.database.saveAuth(auth);
        } catch (NoSuchAlgorithmException ex) {
            return false;
        }
    }

    /**
     * Force a player to login
     *
     * @param player *            player
     */
    @Deprecated
    public static void forceLogin(Player player) {
        instance.getManagement().performLogin(player, "dontneed", true);
    }

    /**
     * Method getPlugin.
     *
     * @return AuthMe
     */
    @Deprecated
    public AuthMe getPlugin() {
        return instance;
    }

    /**
     * @param player
     *
     * @return true if player is a npc
     */
    @Deprecated
    public boolean isaNPC(Player player) {
        return Utils.isNPC(player);
    }

    /**
     * @param player
     *
     * @return true if player is a npc
     */
    @Deprecated
    public boolean isNPC(Player player) {
        return Utils.isNPC(player);
    }

}
