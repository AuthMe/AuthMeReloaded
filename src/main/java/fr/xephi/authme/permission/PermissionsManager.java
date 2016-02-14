package fr.xephi.authme.permission;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.permissions.AnjoPermissionsHandler;
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

import de.bananaco.bpermissions.api.ApiLayer;
import de.bananaco.bpermissions.api.CalculableType;
import fr.xephi.authme.command.CommandDescription;
import fr.xephi.authme.util.CollectionUtils;
import net.milkbowl.vault.permission.Permission;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.PermissionsEx;

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
 * @version 0.2.1
 */
public class PermissionsManager implements PermissionsService {

    /**
     * Vault instance.
     */
    public Permission vaultPerms = null;
    /**
     * Server instance.
     */
    private final Server server;
    /**
     * Plugin instance.
     */
    private final Plugin plugin;
    /**
     * Logger instance.
     */
    private Logger log;
    /**
     * Type of permissions system that is currently used.
     */
    private PermissionsSystemType permsType = PermissionsSystemType.NONE;
    /**
     * Essentials group manager instance.
     */
    private GroupManager groupManagerPerms;
    /**
     * zPermissions service instance.
     */
    private ZPermissionsService zPermissionsService;

    /**
     * Constructor.
     *
     * @param server Server instance
     * @param plugin Plugin instance
     * @param log    Logger
     */
    public PermissionsManager(Server server, Plugin plugin, Logger log) {
        this.server = server;
        this.plugin = plugin;
        this.log = log;
    }

    /**
     * Check if the permissions manager is currently hooked into any of the supported permissions systems.
     *
     * @return False if there isn't any permissions system used.
     */
    public boolean isEnabled() {
        return !permsType.equals(PermissionsSystemType.NONE);
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
     *
     * @return The detected permissions system.
     */
    public PermissionsSystemType setup() {
        // Define the plugin manager
        final PluginManager pm = this.server.getPluginManager();

        // Reset used permissions system type
        permsType = PermissionsSystemType.NONE;

        // PermissionsEx, check if it's available
        try {
            Plugin pex = pm.getPlugin("PermissionsEx");
            if (pex != null) {
                PermissionManager pexPerms = PermissionsEx.getPermissionManager();
                if (pexPerms != null) {
                    permsType = PermissionsSystemType.PERMISSIONS_EX;

                    System.out.println("[" + plugin.getName() + "] Hooked into PermissionsEx!");
                    return permsType;
                }
            }
        } catch (Exception ex) {
            // An error occurred, show a warning message
            System.out.println("[" + plugin.getName() + "] Error while hooking into PermissionsEx!");
        }

        // PermissionsBukkit, check if it's available
        try {
            Plugin bukkitPerms = pm.getPlugin("PermissionsBukkit");
            if (bukkitPerms != null) {
                permsType = PermissionsSystemType.PERMISSIONS_BUKKIT;
                System.out.println("[" + plugin.getName() + "] Hooked into PermissionsBukkit!");
                return permsType;
            }
        } catch (Exception ex) {
            // An error occurred, show a warning message
            System.out.println("[" + plugin.getName() + "] Error while hooking into PermissionsBukkit!");
        }

        // bPermissions, check if it's available
        try {
            Plugin bPerms = pm.getPlugin("bPermissions");
            if (bPerms != null) {
                permsType = PermissionsSystemType.B_PERMISSIONS;
                System.out.println("[" + plugin.getName() + "] Hooked into bPermissions!");
                return permsType;
            }
        } catch (Exception ex) {
            // An error occurred, show a warning message
            System.out.println("[" + plugin.getName() + "] Error while hooking into bPermissions!");
        }

        // Essentials Group Manager, check if it's available
        try {
            final Plugin groupManagerPlugin = pm.getPlugin("GroupManager");
            if (groupManagerPlugin != null && groupManagerPlugin.isEnabled()) {
                permsType = PermissionsSystemType.ESSENTIALS_GROUP_MANAGER;
                groupManagerPerms = (GroupManager) groupManagerPlugin;
                System.out.println("[" + plugin.getName() + "] Hooked into Essentials Group Manager!");
                return permsType;
            }
        } catch (Exception ex) {
            // An error occurred, show a warning message
            System.out.println("[" + plugin.getName() + "] Error while hooking into Essentials Group Manager!");
        }

        // zPermissions, check if it's available
        try {
            Plugin zPerms = pm.getPlugin("zPermissions");
            if (zPerms != null) {
                zPermissionsService = Bukkit.getServicesManager().load(ZPermissionsService.class);
                if (zPermissionsService != null) {
                    permsType = PermissionsSystemType.Z_PERMISSIONS;
                    System.out.println("[" + plugin.getName() + "] Hooked into zPermissions!");
                    return permsType;
                }
            }
        } catch (Exception ex) {
            // An error occurred, show a warning message
            System.out.println("[" + plugin.getName() + "] Error while hooking into zPermissions!");
        }

        // Vault, check if it's available
        try {
            final Plugin vaultPlugin = pm.getPlugin("Vault");
            if (vaultPlugin != null && vaultPlugin.isEnabled()) {
                RegisteredServiceProvider<Permission> permissionProvider = this.server.getServicesManager().getRegistration(Permission.class);
                if (permissionProvider != null) {
                    vaultPerms = permissionProvider.getProvider();
                    if (vaultPerms.isEnabled()) {
                        permsType = PermissionsSystemType.VAULT;
                        System.out.println("[" + plugin.getName() + "] Hooked into Vault Permissions!");
                        return permsType;
                    } else {
                        System.out.println("[" + plugin.getName() + "] Not using Vault Permissions, Vault Permissions is disabled!");
                    }
                }
            }
        } catch (Exception ex) {
            // An error occurred, show a warning message
            System.out.println("[" + plugin.getName() + "] Error while hooking into Vault Permissions!");
        }

        // No recognized permissions system found
        permsType = PermissionsSystemType.NONE;
        System.out.println("[" + plugin.getName() + "] No supported permissions system found! Permissions disabled!");
        return PermissionsSystemType.NONE;
    }

    /**
     * Break the hook with all permission systems.
     */
    public void unhook() {
        // Reset the current used permissions system
        this.permsType = PermissionsSystemType.NONE;

        // Print a status message to the console
        this.log.info("Unhooked from Permissions!");
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
        if (pluginName.equals("PermissionsEx") || pluginName.equals("PermissionsBukkit") ||
            pluginName.equals("bPermissions") || pluginName.equals("GroupManager") ||
            pluginName.equals("zPermissions") || pluginName.equals("Vault")) {
            this.log.info(pluginName + " plugin enabled, dynamically updating permissions hooks!");
            setup();
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
        if (pluginName.equals("PermissionsEx") || pluginName.equals("PermissionsBukkit") ||
            pluginName.equals("bPermissions") || pluginName.equals("GroupManager") ||
            pluginName.equals("zPermissions") || pluginName.equals("Vault")) {
            this.log.info(pluginName + " plugin disabled, updating hooks!");
            setup();
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
        return hasPermission(player, permissionNode.getNode(), def);
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
        if (command.getCommandPermissions() == null
            || CollectionUtils.isEmpty(command.getCommandPermissions().getPermissionNodes())) {
            return true;
        }

        DefaultPermission defaultPermission = command.getCommandPermissions().getDefaultPermission();
        boolean def = evaluateDefaultPermission(defaultPermission, sender);
        return (sender instanceof Player)
            ? hasPermission((Player) sender, command.getCommandPermissions().getPermissionNodes(), def)
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
     * @param permsNode The permission node.
     * @param def       Default returned if no permissions system is used.
     *
     * @return True if the player has permission.
     */
    private boolean hasPermission(Player player, String permsNode, boolean def) {
        // If no permissions system is used, return the default value
        if (!isEnabled())
            return def;

        switch (this.permsType) {
            case PERMISSIONS_EX:
                // Permissions Ex
                PermissionUser user = PermissionsEx.getUser(player);
                return user.has(permsNode);

            case PERMISSIONS_BUKKIT:
                // Permissions Bukkit
                return player.hasPermission(permsNode);

            case B_PERMISSIONS:
                // bPermissions
                return ApiLayer.hasPermission(player.getWorld().getName(), CalculableType.USER, player.getName(), permsNode);

            case ESSENTIALS_GROUP_MANAGER:
                // Essentials Group Manager
                final AnjoPermissionsHandler handler = groupManagerPerms.getWorldsHolder().getWorldPermissions(player);
                return handler != null && handler.has(player, permsNode);

            case Z_PERMISSIONS:
                // zPermissions
                @SuppressWarnings("deprecation")
                Map<String, Boolean> perms = zPermissionsService.getPlayerPermissions(player.getWorld().getName(), null, player.getName());
                if (perms.containsKey(permsNode))
                    return perms.get(permsNode);
                else
                    return def;

            case VAULT:
                // Vault
                return vaultPerms.has(player, permsNode);

            case NONE:
                // Not hooked into any permissions system, return default
                return def;

            default:
                // Something went wrong, return false to prevent problems
                return false;
        }
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

        switch (this.permsType) {
            case PERMISSIONS_EX:
            case PERMISSIONS_BUKKIT:
            case B_PERMISSIONS:
            case ESSENTIALS_GROUP_MANAGER:
            case Z_PERMISSIONS:
                return true;

            case VAULT:
                // Vault
                return vaultPerms.hasGroupSupport();

            case NONE:
                // Not hooked into any permissions system, return false
                return false;

            default:
                // Something went wrong, return false to prevent problems
                return false;
        }
    }

    /**
     * Get the permission groups of a player, if available.
     *
     * @param player The player.
     *
     * @return Permission groups, or an empty list if this feature is not supported.
     */
    @SuppressWarnings({"unchecked", "rawtypes", "deprecation"})
    public List<String> getGroups(Player player) {
        // If no permissions system is used, return an empty list
        if (!isEnabled())
            return new ArrayList<>();

        switch (this.permsType) {
            case PERMISSIONS_EX:
                // Permissions Ex
                PermissionUser user = PermissionsEx.getUser(player);
                return user.getParentIdentifiers(null);

            case PERMISSIONS_BUKKIT:
                // Permissions Bukkit
                // FIXME: Add support for this!
                return new ArrayList<>();

            case B_PERMISSIONS:
                // bPermissions
                return Arrays.asList(ApiLayer.getGroups(player.getWorld().getName(), CalculableType.USER, player.getName()));

            case ESSENTIALS_GROUP_MANAGER:
                // Essentials Group Manager
                final AnjoPermissionsHandler handler = groupManagerPerms.getWorldsHolder().getWorldPermissions(player);
                if (handler == null)
                    return new ArrayList<>();
                return Arrays.asList(handler.getGroups(player.getName()));

            case Z_PERMISSIONS:
                //zPermissions
                return new ArrayList(zPermissionsService.getPlayerGroups(player.getName()));

            case VAULT:
                // Vault
                return Arrays.asList(vaultPerms.getPlayerGroups(player));

            case NONE:
                // Not hooked into any permissions system, return an empty list
                return new ArrayList<>();

            default:
                // Something went wrong, return an empty list to prevent problems
                return new ArrayList<>();
        }
    }

    /**
     * Get the primary group of a player, if available.
     *
     * @param player The player.
     *
     * @return The name of the primary permission group. Or null.
     */
    @SuppressWarnings("deprecation")
    public String getPrimaryGroup(Player player) {
        // If no permissions system is used, return an empty list
        if (!isEnabled())
            return null;

        switch (this.permsType) {
            case PERMISSIONS_EX:
            case PERMISSIONS_BUKKIT:
            case B_PERMISSIONS:
                // Get the groups of the player
                List<String> groups = getGroups(player);

                // Make sure there is any group available, or return null
                if (groups.size() == 0)
                    return null;

                // Return the first group
                return groups.get(0);

            case ESSENTIALS_GROUP_MANAGER:
                // Essentials Group Manager
                final AnjoPermissionsHandler handler = groupManagerPerms.getWorldsHolder().getWorldPermissions(player);
                if (handler == null)
                    return null;
                return handler.getGroup(player.getName());

            case Z_PERMISSIONS:
                //zPermissions
                return zPermissionsService.getPlayerPrimaryGroup(player.getName());

            case VAULT:
                // Vault
                return vaultPerms.getPrimaryGroup(player);

            case NONE:
                // Not hooked into any permissions system, return null
                return null;

            default:
                // Something went wrong, return null to prevent problems
                return null;
        }
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

        switch (this.permsType) {
            case PERMISSIONS_EX:
                // Permissions Ex
                PermissionUser user = PermissionsEx.getUser(player);
                return user.inGroup(groupName);

            case PERMISSIONS_BUKKIT:
            case Z_PERMISSIONS:
                // Get the current list of groups
                List<String> groupNames = getGroups(player);

                // Check whether the list contains the group name, return the result
                for (String entry : groupNames)
                    if (entry.equals(groupName))
                        return true;
                return false;

            case B_PERMISSIONS:
                // bPermissions
                return ApiLayer.hasGroup(player.getWorld().getName(), CalculableType.USER, player.getName(), groupName);

            case ESSENTIALS_GROUP_MANAGER:
                // Essentials Group Manager
                final AnjoPermissionsHandler handler = groupManagerPerms.getWorldsHolder().getWorldPermissions(player);
                return handler != null && handler.inGroup(player.getName(), groupName);

            case VAULT:
                // Vault
                return vaultPerms.playerInGroup(player, groupName);

            case NONE:
                // Not hooked into any permissions system, return an empty list
                return false;

            default:
                // Something went wrong, return an empty list to prevent problems
                return false;
        }
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

        // Set the group the proper way
        switch (this.permsType) {
            case PERMISSIONS_EX:
                // Permissions Ex
                PermissionUser user = PermissionsEx.getUser(player);
                user.addGroup(groupName);
                return true;

            case PERMISSIONS_BUKKIT:
                // Permissions Bukkit
                // Add the group to the user using a command
                return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "permissions player addgroup " + player.getName() + " " + groupName);

            case B_PERMISSIONS:
                // bPermissions
                ApiLayer.addGroup(player.getWorld().getName(), CalculableType.USER, player.getName(), groupName);
                return true;

            case ESSENTIALS_GROUP_MANAGER:
                // Essentials Group Manager
                // Add the group to the user using a command
                return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "manuaddsub " + player.getName() + " " + groupName);

            case Z_PERMISSIONS:
                // zPermissions
                // Add the group to the user using a command
                return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "permissions player " + player.getName() + " addgroup " + groupName);

            case VAULT:
                // Vault
                vaultPerms.playerAddGroup(player, groupName);
                return true;

            case NONE:
                // Not hooked into any permissions system, return false
                return false;

            default:
                // Something went wrong, return false
                return false;
        }
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

        // Set the group the proper way
        switch (this.permsType) {
            case PERMISSIONS_EX:
                // Permissions Ex
                PermissionUser user = PermissionsEx.getUser(player);
                user.removeGroup(groupName);
                return true;

            case PERMISSIONS_BUKKIT:
                // Permissions Bukkit
                // Remove the group to the user using a command
                return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "permissions player removegroup " + player.getName() + " " + groupName);

            case B_PERMISSIONS:
                // bPermissions
                ApiLayer.removeGroup(player.getWorld().getName(), CalculableType.USER, player.getName(), groupName);
                return true;

            case ESSENTIALS_GROUP_MANAGER:
                // Essentials Group Manager
                // Remove the group to the user using a command
                return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "manudelsub " + player.getName() + " " + groupName);

            case Z_PERMISSIONS:
                // zPermissions
                // Remove the group to the user using a command
                return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "permissions player " + player.getName() + " removegroup " + groupName);

            case VAULT:
                // Vault
                vaultPerms.playerRemoveGroup(player, groupName);
                return true;

            case NONE:
                // Not hooked into any permissions system, return false
                return false;

            default:
                // Something went wrong, return false
                return false;
        }
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

        // Create a list of group names
        List<String> groupNames = new ArrayList<>();
        groupNames.add(groupName);

        // Set the group the proper way
        switch (this.permsType) {
            case PERMISSIONS_EX:
                // Permissions Ex
                PermissionUser user = PermissionsEx.getUser(player);
                user.setParentsIdentifier(groupNames);
                return true;

            case PERMISSIONS_BUKKIT:
                // Permissions Bukkit
                // Set the user's group using a command
                return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "permissions player setgroup " + player.getName() + " " + groupName);

            case B_PERMISSIONS:
                // bPermissions
                ApiLayer.setGroup(player.getWorld().getName(), CalculableType.USER, player.getName(), groupName);
                return true;

            case ESSENTIALS_GROUP_MANAGER:
                // Essentials Group Manager
                // Clear the list of groups, add the player to the specified group afterwards using a command
                removeAllGroups(player);
                return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "manuadd " + player.getName() + " " + groupName);

            case Z_PERMISSIONS:
                //zPermissions
                // Set the players group through the plugin commands
                return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "permissions player " + player.getName() + " setgroup " + groupName);

            case VAULT:
                // Vault
                // Remove all current groups, add the player to the specified group afterwards
                removeAllGroups(player);
                vaultPerms.playerAddGroup(player, groupName);
                return true;

            case NONE:
                // Not hooked into any permissions system, return false
                return false;

            default:
                // Something went wrong, return false
                return false;
        }
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
