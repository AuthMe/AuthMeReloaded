package fr.xephi.authme.api;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.hooks.PluginHooks;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.service.ValidationService;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import javax.inject.Inject;

/**
 * Deprecated API of AuthMe. Please use {@link NewAPI} instead.
 */
@Deprecated
public class API {

    public static final String newline = System.getProperty("line.separator");
    public static AuthMe instance;
    private static DataSource dataSource;
    private static PasswordSecurity passwordSecurity;
    private static Management management;
    private static PluginHooks pluginHooks;
    private static ValidationService validationService;

    /*
     * Constructor.
     */
    @Inject
    API(AuthMe instance, DataSource dataSource, PasswordSecurity passwordSecurity, Management management,
        PluginHooks pluginHooks, ValidationService validationService) {
        API.instance = instance;
        API.dataSource = dataSource;
        API.passwordSecurity = passwordSecurity;
        API.management = management;
        API.pluginHooks = pluginHooks;
        API.validationService = validationService;
    }

    /**
     * Hook into AuthMe.
     *
     * @return AuthMe instance
     */
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
    public static boolean isAuthenticated(Player player) {
        return PlayerCache.getInstance().isAuthenticated(player.getName());
    }

    /**
     * Return whether the player is unrestricted.
     *
     * @param player The player to verify
     * @return true if the player is unrestricted
     */
    public static boolean isUnrestricted(Player player) {
        return validationService.isUnrestricted(player.getName());
    }

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
    public static boolean isRegistered(String playerName) {
        String player = playerName.toLowerCase();
        return dataSource.isAuthAvailable(player);
    }

    /**
     * Check the password for the given player.
     *
     * @param playerName      The name of the player
     * @param passwordToCheck The password to check
     * @return true if the password is correct, false otherwise
     */
    public static boolean checkPassword(String playerName, String passwordToCheck) {
        return isRegistered(playerName) && passwordSecurity.comparePassword(passwordToCheck, playerName);
    }

    /**
     * Register a player.
     *
     * @param playerName The name of the player
     * @param password   The password
     * @return true if the player was registered correctly
     */
    public static boolean registerPlayer(String playerName, String password) {
        String name = playerName.toLowerCase();
        HashedPassword hashedPassword = passwordSecurity.computeHash(password, name);
        if (isRegistered(name)) {
            return false;
        }
        PlayerAuth auth = PlayerAuth.builder()
            .name(name)
            .password(hashedPassword)
            .lastLogin(0)
            .realName(playerName)
            .build();
        return dataSource.saveAuth(auth);
    }

    /**
     * Force a player to log in.
     *
     * @param player The player to log in
     */
    public static void forceLogin(Player player) {
        management.forceLogin(player);
    }

    public AuthMe getPlugin() {
        return instance;
    }

    /**
     * Check whether the player is an NPC.
     *
     * @param player The player to verify
     * @return true if player is an npc
     */
    public boolean isNPC(Player player) {
        return pluginHooks.isNpc(player);
    }

}
