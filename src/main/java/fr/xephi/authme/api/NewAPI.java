package fr.xephi.authme.api;

import java.security.NoSuchAlgorithmException;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.Utils;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.plugin.manager.CombatTagComunicator;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.settings.Settings;

public class NewAPI {

    public static final String newline = System.getProperty("line.separator");
    public static NewAPI singleton;
    public AuthMe plugin;

    public NewAPI(AuthMe plugin) {
        this.plugin = plugin;
    }

    public NewAPI(Server serv) {
        this.plugin = (AuthMe) serv.getPluginManager().getPlugin("AuthMe");
    }

    /**
     * Hook into AuthMe
     * 
     * @return
     * 
     * @return AuthMe plugin
     */
    public static NewAPI getInstance() {
        if (singleton != null)
            return singleton;
        Plugin p = Bukkit.getServer().getPluginManager().getPlugin("AuthMe");
        if (p == null || !(p instanceof AuthMe)) {
            return null;
        }
        AuthMe authme = (AuthMe) p;
        singleton = (new NewAPI(authme));
        return singleton;
    }

    public AuthMe getPlugin() {
        return plugin;
    }

    /**
     * 
     * @param player
     * @return true if player is authenticate
     */
    public boolean isAuthenticated(Player player) {
        return PlayerCache.getInstance().isAuthenticated(player.getName());
    }

    /**
     * 
     * @param player
     * @return true if player is a npc
     */
    public boolean isNPC(Player player) {
        if (plugin.getCitizensCommunicator().isNPC(player))
            return true;
        return CombatTagComunicator.isNPC(player);
    }

    /**
     * 
     * @param player
     * @return true if the player is unrestricted
     */
    public boolean isUnrestricted(Player player) {
        return Utils.getInstance().isUnrestricted(player);
    }

    public Location getLastLocation(Player player) {
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

    public void setPlayerInventory(Player player, ItemStack[] content,
            ItemStack[] armor) {
        try {
            player.getInventory().setContents(content);
            player.getInventory().setArmorContents(armor);
        } catch (Exception npe) {
            ConsoleLogger.showError("Some error appear while trying to set inventory for " + player.getName());
        }
    }

    /**
     * 
     * @param playerName
     * @return true if player is registered
     */
    public boolean isRegistered(String playerName) {
        String player = playerName.toLowerCase();
        return plugin.database.isAuthAvailable(player);
    }

    /**
     * @param String
     *            playerName, String passwordToCheck
     * @return true if the password is correct , false else
     */
    public boolean checkPassword(String playerName, String passwordToCheck) {
        if (!isRegistered(playerName))
            return false;
        String player = playerName.toLowerCase();
        PlayerAuth auth = plugin.database.getAuth(player);
        try {
            return PasswordSecurity.comparePasswordWithHash(passwordToCheck, auth.getHash(), playerName);
        } catch (NoSuchAlgorithmException e) {
            return false;
        }
    }

    /**
     * Register a player
     * 
     * @param String
     *            playerName, String password
     * @return true if the player is register correctly
     */
    public boolean registerPlayer(String playerName, String password) {
        try {
            String name = playerName.toLowerCase();
            String hash = PasswordSecurity.getHash(Settings.getPasswordHash, password, name);
            if (isRegistered(name)) {
                return false;
            }
            PlayerAuth auth = new PlayerAuth(name, hash, "192.168.0.1", 0, "your@email.com");
            if (!plugin.database.saveAuth(auth)) {
                return false;
            }
            return true;
        } catch (NoSuchAlgorithmException ex) {
            return false;
        }
    }

    /**
     * Force a player to login
     * 
     * @param Player
     *            player
     */
    public void forceLogin(Player player) {
        plugin.management.performLogin(player, "dontneed", true);
    }

}
