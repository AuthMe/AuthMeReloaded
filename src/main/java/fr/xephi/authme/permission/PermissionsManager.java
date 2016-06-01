package fr.xephi.authme.permission;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.command.CommandDescription;
import fr.xephi.authme.permission.handlers.BPermissionsHandler;
import fr.xephi.authme.permission.handlers.GroupManagerHandler;
import fr.xephi.authme.permission.handlers.PermissionHandler;
import fr.xephi.authme.permission.handlers.PermissionsBukkitHandler;
import fr.xephi.authme.permission.handlers.PermissionsExHandler;
import fr.xephi.authme.permission.handlers.VaultHandler;
import fr.xephi.authme.permission.handlers.ZPermissionsHandler;
import net.milkbowl.vault.permission.Permission;
import org.anjocaido.groupmanager.GroupManager;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsService;
import ru.tehkode.permissions.bukkit.PermissionsEx;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>
 * PermissionsManager.
 * </p><p>
 * A permissions manager, to manage and use various permissions systems.
 * This manager supports dynamic plugin hooking and various other features.
 * </p><p>
 * Written by Tim Visée.
 * </p>
 * @author Tim Visée, http://timvisee.com
 * @version 0.3
 */
public class PermissionsManager {

    /**
     * Server instance.
     */
    private final Server server;
    private final PluginManager pluginManager;
    /**
     * Type of permissions system that is currently used.
     * Null if no permissions system is hooked and/or used.
     */
    private PermissionsSystemType permsType = null;
    /**
     * The permission handler that is currently in use.
     * Null if no permission system is hooked.
     */
    private PermissionHandler handler = null;

    /**
     * Constructor.
     *
     * @param server Server instance
     * @param pluginManager Bukkit plugin manager
     */
    @Inject
    public PermissionsManager(Server server, PluginManager pluginManager) {
        this.server = server;
        this.pluginManager = pluginManager;
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
     * Return the permissions system where the permissions manager is currently hooked into.
     *
     * @return The name of the permissions system used.
     */
    public PermissionsSystemType getSystem() {
        return permsType;
    }

    /**
     * Setup and hook into the permissions systems.
     */
    @PostConstruct
    public void setup() {
        // Force-unhook from current hooked permissions systems
        unhook();

        // Reset used permissions system type flag
        permsType = null;

        // Loop through all the available permissions system types
        for (PermissionsSystemType type : PermissionsSystemType.values()) {
            // Try to find and hook the current plugin if available, print an error if failed
            try {
                // Try to find the plugin for the current permissions system
                Plugin plugin = pluginManager.getPlugin(type.getPluginName());

                // Make sure a plugin with this name was found
                if (plugin == null)
                    continue;

                // Make sure the plugin is enabled before hooking
                if (!plugin.isEnabled()) {
                    ConsoleLogger.info("Not hooking into " + type.getName() + " because it's disabled!");
                    continue;
                }

                // Use the proper method to hook this plugin
                switch (type) {
                    case PERMISSIONS_EX:
                        // Get the permissions manager for PermissionsEx and make sure it isn't null
                        if (PermissionsEx.getPermissionManager() == null) {
                            ConsoleLogger.info("Failed to hook into " + type.getName() + "!");
                            continue;
                        }

                        handler = new PermissionsExHandler(PermissionsEx.getPermissionManager());
                        break;

                    case ESSENTIALS_GROUP_MANAGER:
                        // Set the plugin instance
                        handler = new GroupManagerHandler((GroupManager) plugin);
                        break;

                    case Z_PERMISSIONS:
                        // Set the zPermissions service and make sure it's valid
                        ZPermissionsService zPermissionsService = Bukkit.getServicesManager().load(ZPermissionsService.class);
                        if (zPermissionsService == null) {
                            ConsoleLogger.info("Failed to hook into " + type.getName() + "!");
                            continue;
                        }

                        handler = new ZPermissionsHandler(zPermissionsService);
                        break;

                    case VAULT:
                        // Get the permissions provider service
                        RegisteredServiceProvider<Permission> permissionProvider = this.server.getServicesManager().getRegistration(Permission.class);
                        if (permissionProvider == null) {
                            ConsoleLogger.info("Failed to hook into " + type.getName() + "!");
                            continue;
                        }

                        // Get the Vault provider and make sure it's valid
                        Permission vaultPerms = permissionProvider.getProvider();
                        if (vaultPerms == null) {
                            ConsoleLogger.info("Not using " + type.getName() + " because it's disabled!");
                            continue;
                        }

                        handler = new VaultHandler(vaultPerms);
                        break;

                    case B_PERMISSIONS:
                        handler = new BPermissionsHandler();
                        break;

                    case PERMISSIONS_BUKKIT:
                        handler = new PermissionsBukkitHandler();
                        break;

                    default:
                }

                // Set the hooked permissions system type
                this.permsType = type;

                // Show a success message
                ConsoleLogger.info("Hooked into " + type.getName() + "!");

                // Return the used permissions system type
                return;

            } catch (Exception ex) {
                // An error occurred, show a warning message
                ConsoleLogger.logException("Error while hooking into " + type.getName(), ex);
            }
        }

        // No recognized permissions system found, show a message and return
        ConsoleLogger.info("No supported permissions system found! Permissions are disabled!");
    }

    /**
     * Break the hook with all permission systems.
     */
    public void unhook() {
        // Reset the current used permissions system
        this.permsType = null;

        // Print a status message to the console
        ConsoleLogger.info("Unhooked from Permissions!");
    }

    /**
     * Reload the permissions manager, and re-hook all permission plugins.
     *
     * @return True on success, false on failure.
     */
    public boolean reload() {
        // Unhook all permission plugins
        unhook();

        // Set up the permissions manager again, return the result
        setup();
        return true;
    }

    /**
     * Method called when a plugin is being enabled.
     *
     * @param event Event instance.
     */
    public void onPluginEnable(PluginEnableEvent event) {
        // Get the plugin and it's name
        Plugin plugin = event.getPlugin();
        String pluginName = plugin.getName();

        // Check if any known permissions system is enabling
        for (PermissionsSystemType permissionsSystemType : PermissionsSystemType.values()) {
            if (permissionsSystemType.isPermissionSystem(pluginName)) {
                ConsoleLogger.info(pluginName + " plugin enabled, dynamically updating permissions hooks!");
                setup();
                break;
            }
        }
    }

    /**
     * Method called when a plugin is being disabled.
     *
     * @param event Event instance.
     */
    public void onPluginDisable(PluginDisableEvent event) {
        // Get the plugin instance and name
        Plugin plugin = event.getPlugin();
        String pluginName = plugin.getName();

        // Is the WorldGuard plugin disabled
        for (PermissionsSystemType permissionsSystemType : PermissionsSystemType.values()) {
            if (permissionsSystemType.isPermissionSystem(pluginName)) {
                ConsoleLogger.info(pluginName + " plugin disabled, updating hooks!");
                setup();
                break;
            }
        }
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
        return hasPermission(sender, permissionNode, sender.isOp());
    }

    public boolean hasPermission(CommandSender sender, PermissionNode permissionNode, boolean def) {
        if (!(sender instanceof Player)) {
            return def;
        }

        Player player = (Player) sender;
        return hasPermission(player, permissionNode, def);
    }

    public boolean hasPermission(Player player, Iterable<PermissionNode> nodes, boolean def) {
        for (PermissionNode node : nodes) {
            if (!hasPermission(player, node, def)) {
                return false;
            }
        }
        return true;
    }

    public boolean hasPermission(CommandSender sender, CommandDescription command) {
        if (command.getPermission() == null) {
            return true;
        }

        DefaultPermission defaultPermission = command.getPermission().getDefaultPermission();
        boolean def = evaluateDefaultPermission(defaultPermission, sender);
        return (sender instanceof Player)
            ? hasPermission((Player) sender, command.getPermission(), def)
            : def;
    }

    public static boolean evaluateDefaultPermission(DefaultPermission defaultPermission, CommandSender sender) {
        switch (defaultPermission) {
            case ALLOWED:
                return true;

            case OP_ONLY:
                return sender.isOp();

            case NOT_ALLOWED:
            default:
                return false;
        }
    }

    /**
     * Check if a player has permission.
     *
     * @param player    The player.
     * @param node      The permission node.
     * @param def       Default returned if no permissions system is used.
     *
     * @return True if the player has permission.
     */
    private boolean hasPermission(Player player, PermissionNode node, boolean def) {
        // If no permissions system is used, return the default value
        if (!isEnabled())
            return def;

        return handler.hasPermission(player, node, def);
    }

    /**
     * Check whether the current permissions system has group support.
     * If no permissions system is hooked, false will be returned.
     *
     * @return True if the current permissions system supports groups, false otherwise.
     */
    public boolean hasGroupSupport() {
        // If no permissions system is used, return false
        if (!isEnabled())
            return false;

        return handler.hasGroupSupport();
    }

    /**
     * Get the permission groups of a player, if available.
     *
     * @param player The player.
     *
     * @return Permission groups, or an empty list if this feature is not supported.
     */
    public List<String> getGroups(Player player) {
        // If no permissions system is used, return an empty list
        if (!isEnabled())
            return new ArrayList<>();

        return handler.getGroups(player);
    }

    /**
     * Get the primary group of a player, if available.
     *
     * @param player The player.
     *
     * @return The name of the primary permission group. Or null.
     */
    public String getPrimaryGroup(Player player) {
        // If no permissions system is used, return an empty list
        if (!isEnabled())
            return null;

        return handler.getPrimaryGroup(player);
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
    public boolean inGroup(Player player, String groupName) {
        // If no permissions system is used, return false
        if (!isEnabled())
            return false;

        return handler.isInGroup(player, groupName);
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
    public boolean addGroup(Player player, String groupName) {
        // If no permissions system is used, return false
        if (!isEnabled())
            return false;

        return handler.addToGroup(player, groupName);
    }

    /**
     * Add the permission groups of a player, if supported.
     *
     * @param player     The player
     * @param groupNames The name of the groups to add.
     *
     * @return True if succeed, false otherwise.
     * False is also returned if this feature isn't supported for the current permissions system.
     */
    public boolean addGroups(Player player, List<String> groupNames) {
        // If no permissions system is used, return false
        if (!isEnabled())
            return false;

        // Add each group to the user
        boolean result = true;
        for (String groupName : groupNames)
            if (!addGroup(player, groupName))
                result = false;

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
    public boolean removeGroup(Player player, String groupName) {
        // If no permissions system is used, return false
        if (!isEnabled())
            return false;

        return handler.removeFromGroup(player, groupName);
    }

    /**
     * Remove the permission groups of a player, if supported.
     *
     * @param player     The player
     * @param groupNames The name of the groups to add.
     *
     * @return True if succeed, false otherwise.
     * False is also returned if this feature isn't supported for the current permissions system.
     */
    public boolean removeGroups(Player player, List<String> groupNames) {
        // If no permissions system is used, return false
        if (!isEnabled())
            return false;

        // Add each group to the user
        boolean result = true;
        for (String groupName : groupNames)
            if (!removeGroup(player, groupName))
                result = false;

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
    public boolean setGroup(Player player, String groupName) {
        // If no permissions system is used, return false
        if (!isEnabled())
            return false;

        return handler.setGroup(player, groupName);
    }

    /**
     * Set the permission groups of a player, if supported.
     * This clears the current groups of the player.
     *
     * @param player     The player
     * @param groupNames The name of the groups to set.
     *
     * @return True if succeed, false otherwise.
     * False is also returned if this feature isn't supported for the current permissions system.
     */
    public boolean setGroups(Player player, List<String> groupNames) {
        // If no permissions system is used or if there's no group supplied, return false
        if (!isEnabled() || groupNames.size() <= 0)
            return false;

        // Set the main group
        if (!setGroup(player, groupNames.get(0)))
            return false;

        // Add the rest of the groups
        boolean result = true;
        for (int i = 1; i < groupNames.size(); i++) {
            // Get the group name
            String groupName = groupNames.get(0);

            // Add this group
            if (!addGroup(player, groupName))
                result = false;
        }

        // Return the result
        return result;
    }

    /**
     * Remove all groups of the specified player, if supported.
     * Systems like Essentials GroupManager don't allow all groups to be removed from a player, thus the user will stay
     * in it's primary group. All the subgroups are removed just fine.
     *
     * @param player The player to remove all groups from.
     *
     * @return True if succeed, false otherwise.
     * False will also be returned if this feature isn't supported for the used permissions system.
     */
    public boolean removeAllGroups(Player player) {
        // If no permissions system is used, return false
        if (!isEnabled())
            return false;

        // Get a list of current groups
        List<String> groupNames = getGroups(player);

        // Remove each group
        return removeGroups(player, groupNames);
    }
}
