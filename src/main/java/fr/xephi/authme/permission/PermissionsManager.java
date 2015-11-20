package fr.xephi.authme.permission;

import com.nijiko.permissions.PermissionHandler;
import com.nijikokun.bukkit.Permissions.Permissions;
import de.bananaco.bpermissions.api.ApiLayer;
import de.bananaco.bpermissions.api.CalculableType;
import net.milkbowl.vault.permission.Permission;
import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.permissions.AnjoPermissionsHandler;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.event.server.PluginDisableEvent;
import org.bukkit.event.server.PluginEnableEvent;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsService;
import ru.tehkode.permissions.PermissionManager;
import ru.tehkode.permissions.PermissionUser;
import ru.tehkode.permissions.bukkit.PermissionsEx;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * PermissionsManager.
 *
 * A permissions manager, to manage and use various permissions systems.
 * This manager supports dynamic plugin hooking and various other features.
 *
 * Written by Tim Visée.
 *
 * @author Tim Visée, http://timvisee.com
 * @version 0.2.1
 */
public class PermissionsManager {

    /**
     * Server instance.
     */
    private Server server;
    /**
     * Plugin instance.
     */
    private Plugin plugin;
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
     * Permissions manager instance for the legacy permissions system.
     */
    private PermissionHandler defaultPerms;
    /**
     * zPermissions service instance.
     */
    private ZPermissionsService zPermissionsService;
    /**
     * Vault instance.
     */
    public Permission vaultPerms = null;

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
     * @return Permissions system type.
     */
    public PermissionsSystemType getUsedPermissionsSystemType() {
        return this.permsType;
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
            if(pex != null) {
                PermissionManager pexPerms = PermissionsEx.getPermissionManager();
                if(pexPerms != null) {
                    permsType = PermissionsSystemType.PERMISSIONS_EX;

                    System.out.println("[" + plugin.getName() + "] Hooked into PermissionsEx!");
                    return permsType;
                }
            }
        } catch(Exception ex) {
            // An error occurred, show a warning message
            System.out.println("[" + plugin.getName() + "] Error while hooking into PermissionsEx!");
        }

        // PermissionsBukkit, check if it's available
        try {
            Plugin bukkitPerms = pm.getPlugin("PermissionsBukkit");
            if(bukkitPerms != null) {
                permsType = PermissionsSystemType.PERMISSIONS_BUKKIT;
                System.out.println("[" + plugin.getName() + "] Hooked into PermissionsBukkit!");
                return permsType;
            }
        } catch(Exception ex) {
            // An error occurred, show a warning message
            System.out.println("[" + plugin.getName() + "] Error while hooking into PermissionsBukkit!");
        }

        // bPermissions, check if it's available
        try {
            Plugin bPerms = pm.getPlugin("bPermissions");
            if(bPerms != null) {
                permsType = PermissionsSystemType.B_PERMISSIONS;
                System.out.println("[" + plugin.getName() + "] Hooked into bPermissions!");
                return permsType;
            }
        } catch(Exception ex) {
            // An error occurred, show a warning message
            System.out.println("[" + plugin.getName() + "] Error while hooking into bPermissions!");
        }

        // Essentials Group Manager, check if it's available
        try {
            final Plugin groupManagerPlugin = pm.getPlugin("GroupManager");
            if(groupManagerPlugin != null && groupManagerPlugin.isEnabled()) {
                permsType = PermissionsSystemType.ESSENTIALS_GROUP_MANAGER;
                groupManagerPerms = (GroupManager) groupManagerPlugin;
                System.out.println("[" + plugin.getName() + "] Hooked into Essentials Group Manager!");
                return permsType;
            }
        } catch(Exception ex) {
            // An error occurred, show a warning message
            System.out.println("[" + plugin.getName() + "] Error while hooking into Essentials Group Manager!");
        }

        // zPermissions, check if it's available
        try {
            Plugin zPerms = pm.getPlugin("zPermissions");
            if(zPerms != null) {
                zPermissionsService = Bukkit.getServicesManager().load(ZPermissionsService.class);
                if(zPermissionsService != null) {
                    permsType = PermissionsSystemType.Z_PERMISSIONS;
                    System.out.println("[" + plugin.getName() + "] Hooked into zPermissions!");
                    return permsType;
                }
            }
        } catch(Exception ex) {
            // An error occurred, show a warning message
            System.out.println("[" + plugin.getName() + "] Error while hooking into zPermissions!");
        }

        // Vault, check if it's available
        try {
            final Plugin vaultPlugin = pm.getPlugin("Vault");
            if(vaultPlugin != null && vaultPlugin.isEnabled()) {
                RegisteredServiceProvider<Permission> permissionProvider = this.server.getServicesManager().getRegistration(Permission.class);
                if(permissionProvider != null) {
                    vaultPerms = permissionProvider.getProvider();
                    if(vaultPerms.isEnabled()) {
                        permsType = PermissionsSystemType.VAULT;
                        System.out.println("[" + plugin.getName() + "] Hooked into Vault Permissions!");
                        return permsType;
                    } else {
                        System.out.println("[" + plugin.getName() + "] Not using Vault Permissions, Vault Permissions is disabled!");
                    }
                }
            }
        } catch(Exception ex) {
            // An error occurred, show a warning message
            System.out.println("[" + plugin.getName() + "] Error while hooking into Vault Permissions!");
        }

        // Permissions, check if it's available
        try {
            Plugin testPerms = pm.getPlugin("Permissions");
            if(testPerms != null) {
                permsType = PermissionsSystemType.PERMISSIONS;
                this.defaultPerms = ((Permissions) testPerms).getHandler();
                System.out.println("[" + plugin.getName() + "] Hooked into Permissions!");
                return PermissionsSystemType.PERMISSIONS;
            }
        } catch(Exception ex) {
            // An error occurred, show a warning message
            System.out.println("[" + plugin.getName() + "] Error while hooking into Permissions!");
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
        if(pluginName.equals("PermissionsEx") || pluginName.equals("PermissionsBukkit") ||
                pluginName.equals("bPermissions") || pluginName.equals("GroupManager") ||
                pluginName.equals("zPermissions") || pluginName.equals("Vault") ||
                pluginName.equals("Permissions")) {
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
        if(pluginName.equals("PermissionsEx") || pluginName.equals("PermissionsBukkit") ||
                pluginName.equals("bPermissions") || pluginName.equals("GroupManager") ||
                pluginName.equals("zPermissions") || pluginName.equals("Vault") ||
                pluginName.equals("Permissions")) {
            this.log.info(pluginName + " plugin disabled, updating hooks!");
            setup();
        }
    }

    /**
     * Get the logger instance.
     *
     * @return Logger instance.
     */
    public Logger getLogger() {
        return this.log;
    }

    /**
     * Set the logger instance.
     *
     * @param log Logger instance.
     */
    public void setLogger(Logger log) {
        this.log = log;
    }

    /**
     * Check if the player has permission. If no permissions system is used, the player has to be OP.
     *
     * @param player    The player.
     * @param permsNode Permissions node.
     *
     * @return True if the player has permission.
     */
    public boolean hasPermission(Player player, String permsNode) {
        return hasPermission(player, permsNode, player.isOp());
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
    public boolean hasPermission(Player player, String permsNode, boolean def) {
        if(!isEnabled())
            // No permissions system is used, return default
            return def;

        switch(this.permsType) {
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
                Map<String, Boolean> perms = zPermissionsService.getPlayerPermissions(player.getWorld().getName(), null, player.getName());
                if(perms.containsKey(permsNode))
                    return perms.get(permsNode);
                else
                    return def;

            case VAULT:
                // Vault
                return vaultPerms.has(player, permsNode);

            case PERMISSIONS:
                // Permissions
                return this.defaultPerms.has(player, permsNode);

            case NONE:
                // Not hooked into any permissions system, return default
                return def;

            default:
                // Something went wrong, return false to prevent problems
                return false;
        }
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    public List<String> getGroups(Player player) {
        if(!isEnabled())
            // No permissions system is used, return an empty list
            return new ArrayList<>();

        switch(this.permsType) {
            case PERMISSIONS_EX:
                // Permissions Ex
                PermissionUser user = PermissionsEx.getUser(player);
                return user.getParentIdentifiers(null);

            case PERMISSIONS_BUKKIT:
                // Permissions Bukkit
                // Permissions Bukkit doesn't support group, return an empty list
                return new ArrayList<>();

            case B_PERMISSIONS:
                // bPermissions
                return Arrays.asList(ApiLayer.getGroups(player.getName(), CalculableType.USER, player.getName()));

            case ESSENTIALS_GROUP_MANAGER:
                // Essentials Group Manager
                final AnjoPermissionsHandler handler = groupManagerPerms.getWorldsHolder().getWorldPermissions(player);
                if(handler == null)
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

    public enum PermissionsSystemType {
        NONE("None"),
        PERMISSIONS_EX("PermissionsEx"),
        PERMISSIONS_BUKKIT("Permissions Bukkit"),
        B_PERMISSIONS("bPermissions"),
        ESSENTIALS_GROUP_MANAGER("Essentials Group Manager"),
        Z_PERMISSIONS("zPermissions"),
        VAULT("Vault"),
        PERMISSIONS("Permissions");

        public String name;

        PermissionsSystemType(String name) {
            this.name = name;
        }

        public String getName() {
            return this.name;
        }
    }
}