package fr.xephi.authme.permission;

import com.google.common.annotations.VisibleForTesting;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.initialization.Reloadable;
import fr.xephi.authme.listener.JoiningPlayer;
import fr.xephi.authme.permission.handlers.BPermissionsHandler;
import fr.xephi.authme.permission.handlers.LuckPermsHandler;
import fr.xephi.authme.permission.handlers.PermissionHandler;
import fr.xephi.authme.permission.handlers.PermissionHandlerException;
import fr.xephi.authme.permission.handlers.PermissionLoadUserException;
import fr.xephi.authme.permission.handlers.PermissionsExHandler;
import fr.xephi.authme.permission.handlers.VaultHandler;
import fr.xephi.authme.permission.handlers.ZPermissionsHandler;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.util.StringUtils;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.Collection;
import java.util.Collections;
import java.util.UUID;

/**
 * PermissionsManager.
 * <p>
 * A permissions manager, to manage and use various permissions systems.
 * This manager supports dynamic plugin hooking and various other features.
 * <p>
 * Written by Tim Visée.
 *
 * @author Tim Visée, http://timvisee.com
 * @version 0.3
 */
public class PermissionsManager implements Reloadable {

    private final Server server;
    private final PluginManager pluginManager;
    private final Settings settings;

    /**
     * The permission handler that is currently in use.
     * Null if no permission system is hooked.
     */
    private PermissionHandler handler = null;

    @Inject
    PermissionsManager(Server server, PluginManager pluginManager, Settings settings) {
        this.server = server;
        this.pluginManager = pluginManager;
        this.settings = settings;
    }

    /**
     * Check if the permissions manager is currently hooked into any of the supported permissions systems.
     *
     * @return False if there isn't any permissions system used.
     */
    public boolean isEnabled() {
        return handler != null;
    }

    /**
     * Setup and hook into the permissions systems.
     */
    @PostConstruct
    @VisibleForTesting
    void setup() {
        if (settings.getProperty(PluginSettings.FORCE_VAULT_HOOK)) {
            try {
                PermissionHandler handler = createPermissionHandler(PermissionsSystemType.VAULT);
                if (handler != null) {
                    // Show a success message and return
                    this.handler = handler;
                    ConsoleLogger.info("Hooked into " + PermissionsSystemType.VAULT.getDisplayName() + "!");
                    return;
                }
            } catch (PermissionHandlerException e) {
                ConsoleLogger.logException("Failed to create Vault hook (forced):", e);
            }
        } else {
            // Loop through all the available permissions system types
            for (PermissionsSystemType type : PermissionsSystemType.values()) {
                try {
                    PermissionHandler handler = createPermissionHandler(type);
                    if (handler != null) {
                        // Show a success message and return
                        this.handler = handler;
                        ConsoleLogger.info("Hooked into " + type.getDisplayName() + "!");
                        return;
                    }
                } catch (Exception ex) {
                    // An error occurred, show a warning message
                    ConsoleLogger.logException("Error while hooking into " + type.getDisplayName(), ex);
                }
            }
        }

        // No recognized permissions system found, show a message and return
        ConsoleLogger.info("No supported permissions system found! Permissions are disabled!");
    }

    /**
     * Creates a permission handler for the provided permission systems if possible.
     *
     * @param type the permission systems type for which to create a corresponding permission handler
     *
     * @return the permission handler, or {@code null} if not possible
     *
     * @throws PermissionHandlerException during initialization of the permission handler
     */
    private PermissionHandler createPermissionHandler(PermissionsSystemType type) throws PermissionHandlerException {
        // Try to find the plugin for the current permissions system
        Plugin plugin = pluginManager.getPlugin(type.getPluginName());

        if (plugin == null) {
            return null;
        }

        // Make sure the plugin is enabled before hooking
        if (!plugin.isEnabled()) {
            ConsoleLogger.info("Not hooking into " + type.getDisplayName() + " because it's disabled!");
            return null;
        }

        switch (type) {
            case LUCK_PERMS:
                return new LuckPermsHandler();
            case PERMISSIONS_EX:
                return new PermissionsExHandler();
            case Z_PERMISSIONS:
                return new ZPermissionsHandler();
            case VAULT:
                return new VaultHandler(server);
            case B_PERMISSIONS:
                return new BPermissionsHandler();
            default:
                throw new IllegalStateException("Unhandled permission type '" + type + "'");
        }
    }

    /**
     * Break the hook with all permission systems.
     */
    private void unhook() {
        // Reset the current used permissions system
        this.handler = null;

        // Print a status message to the console
        ConsoleLogger.info("Unhooked from Permissions!");
    }

    /**
     * Reload the permissions manager, and re-hook all permission plugins.
     */
    @Override
    public void reload() {
        // Unhook all permission plugins
        unhook();

        // Set up the permissions manager again
        setup();
    }

    /**
     * Method called when a plugin is being enabled.
     *
     * @param pluginName The name of the plugin being enabled.
     */
    public void onPluginEnable(String pluginName) {
        // Check if any known permissions system is enabling
        if (PermissionsSystemType.isPermissionSystem(pluginName)) {
            ConsoleLogger.info(pluginName + " plugin enabled, dynamically updating permissions hooks!");
            setup();
        }
    }

    /**
     * Method called when a plugin is being disabled.
     *
     * @param pluginName The name of the plugin being disabled.
     */
    public void onPluginDisable(String pluginName) {
        // Check if any known permission system is being disabled
        if (PermissionsSystemType.isPermissionSystem(pluginName)) {
            ConsoleLogger.info(pluginName + " plugin disabled, updating hooks!");
            setup();
        }
    }

    /**
     * Return the permissions system that is hooked into.
     *
     * @return The permissions system, or null.
     */
    public PermissionsSystemType getPermissionSystem() {
        return isEnabled() ? handler.getPermissionSystem() : null;
    }

    /**
     * Check if the command sender has permission for the given permissions node. If no permissions system is used or
     * if the sender is not a player (e.g. console user), the player has to be OP in order to have the permission.
     *
     * @param sender         The command sender.
     * @param permissionNode The permissions node to verify.
     *
     * @return True if the sender has the permission, false otherwise.
     */
    public boolean hasPermission(CommandSender sender, PermissionNode permissionNode) {
        // Check if the permission node is null
        if (permissionNode == null) {
            return true;
        }

        // Return default if sender is not a player or no permission system is in use
        if (!(sender instanceof Player) || !isEnabled()) {
            return permissionNode.getDefaultPermission().evaluate(sender);
        }

        Player player = (Player) sender;
        return player.hasPermission(permissionNode.getNode());
    }

    /**
     * Check if the given player has permission for the given permission node.
     *
     * @param joiningPlayer  The player to check
     * @param permissionNode The permission node to verify
     *
     * @return true if the player has permission, false otherwise
     */
    public boolean hasPermission(JoiningPlayer joiningPlayer, PermissionNode permissionNode) {
        return joiningPlayer.getPermissionLookupFunction().apply(this, permissionNode);
    }

    /**
     * Check if a player has permission for the given permission node. This is for offline player checks.
     * If no permissions system is used, then the player will not have permission.
     *
     * @param player         The offline player
     * @param permissionNode The permission node to verify
     *
     * @return true if the player has permission, false otherwise
     */
    public boolean hasPermissionOffline(OfflinePlayer player, PermissionNode permissionNode) {
        // Check if the permission node is null
        if (permissionNode == null) {
            return true;
        }

        if (!isEnabled()) {
            return permissionNode.getDefaultPermission().evaluate(player);
        }

        return handler.hasPermissionOffline(player.getName(), permissionNode);
    }

    /**
     * Check whether the offline player with the given name has permission for the given permission node.
     * This method is used as a last resort when nothing besides the name is known.
     *
     * @param name           The name of the player
     * @param permissionNode The permission node to verify
     *
     * @return true if the player has permission, false otherwise
     */
    public boolean hasPermissionOffline(String name, PermissionNode permissionNode) {
        if (permissionNode == null) {
            return true;
        }
        if (!isEnabled()) {
            return permissionNode.getDefaultPermission().evaluate(null);
        }

        return handler.hasPermissionOffline(name, permissionNode);
    }

    /**
     * Check whether the current permissions system has group support.
     * If no permissions system is hooked, false will be returned.
     *
     * @return True if the current permissions system supports groups, false otherwise.
     */
    public boolean hasGroupSupport() {
        return isEnabled() && handler.hasGroupSupport();
    }

    /**
     * Get the permission groups of a player, if available.
     *
     * @param player The player.
     *
     * @return Permission groups, or an empty collection if this feature is not supported.
     */
    public Collection<String> getGroups(OfflinePlayer player) {
        return isEnabled() ? handler.getGroups(player) : Collections.emptyList();
    }

    /**
     * Get the primary group of a player, if available.
     *
     * @param player The player.
     *
     * @return The name of the primary permission group. Or null.
     */
    public String getPrimaryGroup(OfflinePlayer player) {
        return isEnabled() ? handler.getPrimaryGroup(player) : null;
    }

    /**
     * Check whether the player is in the specified group.
     *
     * @param player    The player.
     * @param groupName The group name.
     *
     * @return True if the player is in the specified group, false otherwise.
     * False is also returned if groups aren't supported by the used permissions system.
     */
    public boolean isInGroup(OfflinePlayer player, String groupName) {
        return isEnabled() && handler.isInGroup(player, groupName);
    }

    /**
     * Add the permission group of a player, if supported.
     *
     * @param player    The player
     * @param groupName The name of the group.
     *
     * @return True if succeed, false otherwise.
     * False is also returned if this feature isn't supported for the current permissions system.
     */
    public boolean addGroup(OfflinePlayer player, String groupName) {
        if (!isEnabled() || StringUtils.isEmpty(groupName)) {
            return false;
        }
        return handler.addToGroup(player, groupName);
    }

    /**
     * Add the permission groups of a player, if supported.
     *
     * @param player     The player
     * @param groupNames The name of the groups to add.
     *
     * @return True if at least one group was added, false otherwise.
     * False is also returned if this feature isn't supported for the current permissions system.
     */
    public boolean addGroups(OfflinePlayer player, Collection<String> groupNames) {
        // If no permissions system is used, return false
        if (!isEnabled()) {
            return false;
        }

        // Add each group to the user
        boolean result = false;
        for (String groupName : groupNames) {
            if (!groupName.isEmpty()) {
                result |= handler.addToGroup(player, groupName);
            }
        }

        // Return the result
        return result;
    }

    /**
     * Remove the permission group of a player, if supported.
     *
     * @param player    The player
     * @param groupName The name of the group.
     *
     * @return True if succeed, false otherwise.
     * False is also returned if this feature isn't supported for the current permissions system.
     */
    public boolean removeGroup(OfflinePlayer player, String groupName) {
        return isEnabled() && handler.removeFromGroup(player, groupName);
    }

    /**
     * Remove the permission groups of a player, if supported.
     *
     * @param player     The player
     * @param groupNames The name of the groups to remove.
     *
     * @return True if at least one group was removed, false otherwise.
     * False is also returned if this feature isn't supported for the current permissions system.
     */
    public boolean removeGroups(OfflinePlayer player, Collection<String> groupNames) {
        // If no permissions system is used, return false
        if (!isEnabled()) {
            return false;
        }

        // Add each group to the user
        boolean result = false;
        for (String groupName : groupNames) {
            if (!groupName.isEmpty()) {
                result |= handler.removeFromGroup(player, groupName);
            }
        }

        // Return the result
        return result;
    }

    /**
     * Set the permission group of a player, if supported.
     * This clears the current groups of the player.
     *
     * @param player    The player
     * @param groupName The name of the group.
     *
     * @return True if succeed, false otherwise.
     * False is also returned if this feature isn't supported for the current permissions system.
     */
    public boolean setGroup(OfflinePlayer player, String groupName) {
        return isEnabled() && handler.setGroup(player, groupName);
    }

    /**
     * Remove all groups of the specified player, if supported.
     * Systems like Essentials GroupManager don't allow all groups to be removed from a player, thus the user will stay
     * in its primary group. All the subgroups are removed just fine.
     *
     * @param player The player to remove all groups from.
     *
     * @return True if succeed, false otherwise.
     * False will also be returned if this feature isn't supported for the used permissions system.
     */
    public boolean removeAllGroups(OfflinePlayer player) {
        // If no permissions system is used, return false
        if (!isEnabled()) {
            return false;
        }

        // Get a list of current groups
        Collection<String> groupNames = getGroups(player);

        // Remove each group
        return removeGroups(player, groupNames);
    }

    /**
     * Loads the permission data of the given player.
     *
     * @param offlinePlayer the offline player.
     * @return true if the load was successful.
     */
    public boolean loadUserData(OfflinePlayer offlinePlayer) {
        try {
            try {
                loadUserData(offlinePlayer.getUniqueId());
            } catch (NoSuchMethodError e) {
                loadUserData(offlinePlayer.getName());
            }
        } catch (PermissionLoadUserException e) {
            ConsoleLogger.logException("Unable to load the permission data of user " + offlinePlayer.getName(), e);
            return false;
        }
        return true;
    }

    /**
     * Loads the permission data of the given player unique identifier.
     *
     * @param uuid the {@link UUID} of the player.
     * @throws PermissionLoadUserException if the action failed.
     */
    public void loadUserData(UUID uuid) throws PermissionLoadUserException {
        if (!isEnabled()) {
            return;
        }
        handler.loadUserData(uuid);
    }

    /**
     * Loads the permission data of the given player name.
     *
     * @param name the name of the player.
     * @throws PermissionLoadUserException if the action failed.
     */
    public void loadUserData(String name) throws PermissionLoadUserException {
        if (!isEnabled()) {
            return;
        }
        handler.loadUserData(name);
    }
}
