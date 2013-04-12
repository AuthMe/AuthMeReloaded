package uk.org.whoami.authme.api;

import java.lang.reflect.Array;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;

import uk.org.whoami.authme.AuthMe;
import uk.org.whoami.authme.Utils;
import uk.org.whoami.authme.cache.auth.PlayerAuth;
import uk.org.whoami.authme.cache.auth.PlayerCache;
import uk.org.whoami.authme.datasource.DataSource;
import uk.org.whoami.authme.datasource.DataSource.DataSourceType;
import uk.org.whoami.authme.security.PasswordSecurity.HashAlgorithm;
import uk.org.whoami.authme.settings.Settings;

public class API {

	public AuthMe instance;
	public DataSource database;

	public API(AuthMe instance, DataSource database) {
		this.instance = instance;
		this.database = database;
	}
	/**
	 * Hook into AuthMe
	 * @return AuthMe instance
	 */
    public static AuthMe hookAuthMe() {
    	Plugin plugin = Bukkit.getServer().getPluginManager().getPlugin("AuthMe");
        if (plugin == null && !(plugin instanceof AuthMe)) {
        	return null;
         }
    	return (AuthMe) plugin;
    }

    public AuthMe getPlugin() {
    	return instance;
    }

    /**
     * 
     * @param player
     * @return true if player is authenticate
     */
    public static boolean isAuthenticated(Player player) {
    	return PlayerCache.getInstance().isAuthenticated(player.getName());
    }

    /**
     * 
     * @param player
     * @return true if player is a npc
     */
    public boolean isaNPC(Player player) {
    	return instance.getCitizensCommunicator().isNPC(player, instance);
    }

    /**
     * 
     * @param player
     * @return true if the player is unrestricted
     */
    public static boolean isUnrestricted(Player player) {
    	return Utils.getInstance().isUnrestricted(player);
    }

    public static boolean isChatAllowed() {
    	return Settings.isChatAllowed;
    }

    public static boolean isAllowRestrictedIp() {
    	return Settings.isAllowRestrictedIp;
    }

    public static boolean isBackupActivated() {
    	return Settings.isBackupActivated;
    }

    public static boolean isForceSpawnLocOnJoinEnabled() {
    	return Settings.isForceSpawnLocOnJoinEnabled;
    }

    public static DataSourceType getDataSource() {
    	return Settings.getDataSource;
    }

    public static int getMovementRadius() {
    	return Settings.getMovementRadius;
    }

    public static List<String> getJoinPermissions() {
    	return Settings.getJoinPermissions;
    }

    public static Boolean isPasspartuEnable() {
    	return Settings.enablePasspartu;
    }

    public static String getcUnrestrictedName() {
    	return Settings.getcUnrestrictedName;
    }

    public static Boolean getEnablePasswordVerifier() {
    	return Settings.getEnablePasswordVerifier;
    }

    public static int getMaxNickLength() {
    	return Settings.getMaxNickLength;
    }

    public static int getMinNickLength() {
    	return Settings.getMinNickLength;
    }

    public static Array getLastLocationColumns() {
    	Array columns = null;
    	Array.set(columns, 0, Settings.getMySQLlastlocX);
    	Array.set(columns, 1, Settings.getMySQLlastlocY);
    	Array.set(columns, 2, Settings.getMySQLlastlocZ);
    	return columns;
    }

    public static Location getLastLocation(Player player) {
    	try {
    		PlayerAuth auth = PlayerCache.getInstance().getAuth(player.getName());
        	
        	if (auth != null) {
        		Location loc = new Location(player.getWorld(), auth.getQuitLocX(), auth.getQuitLocY() , auth.getQuitLocZ());
        		return loc;
        	} else {
        		return null;
        	}
        	
    	} catch (NullPointerException ex) {
    		return null;
    	}
    }

    public static String getNickRegex() {
    	return Settings.getNickRegex;
    }

    public static int getPasswordMinLen() {
    	return Settings.getPasswordMinLen;
    }

    public static HashAlgorithm getPasswordHash() {
    	return Settings.getPasswordHash;
    }

    public static int getRegistrationTimeout() {
    	return Settings.getRegistrationTimeout;
    }

    public static int getSessionTimeout() {
    	return Settings.getSessionTimeout;
    }

    public static String getUnloggedinGroup() {
    	return Settings.getUnloggedinGroup;
    }

    public static void setPlayerInventory(Player player, ItemStack[] content, ItemStack[] armor) {
    	try {
        	player.getInventory().setContents(content);
        	player.getInventory().setArmorContents(armor);
    	} catch (NullPointerException npe) {
    	}
    }

    public void saveAuth(final PlayerAuth auth) {
    	instance.getServer().getScheduler().runTask(instance, new Runnable() {
			@Override
			public void run() {
				database.saveAuth(auth);
			}
    	});
    }

}
