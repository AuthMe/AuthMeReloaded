package fr.xephi.authme.command;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.permission.DefaultPermission;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PermissionNode;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

/**
 */
public class CommandPermissions {

    /**
     * Defines the permission nodes required to have permission to execute this command.
     */
    private List<PermissionNode> permissionNodes;
    /**
     * Defines the default permission if the permission nodes couldn't be used.
     */
    private DefaultPermission defaultPermission;

    /**
     * Constructor.
     *
     * @param permissionNodes   The permission nodes required to execute a command.
     * @param defaultPermission The default permission if the permission nodes couldn't be used.
     */
    public CommandPermissions(List<PermissionNode> permissionNodes, DefaultPermission defaultPermission) {
        this.permissionNodes = permissionNodes;
        this.defaultPermission = defaultPermission;
    }

    /**
     * Get the permission nodes required to execute this command.
     *
     * @return The permission nodes required to execute this command.
     */
    public List<PermissionNode> getPermissionNodes() {
        return this.permissionNodes;
    }

    /**
     * Check whether this command requires any permission to be executed. This is based on the getPermission() method.
     *
     * @param sender CommandSender
     *
     * @return True if this command requires any permission to be executed by a player.
     */
    public boolean hasPermission(CommandSender sender) {
        // Make sure any permission node is set
        if (permissionNodes.isEmpty()) {
            return true;
        }

        PermissionsManager permissionsManager = AuthMe.getInstance().getPermissionsManager();
        final boolean defaultPermission = getDefaultPermissionCommandSender(sender);

        // Make sure the command sender is a player, if not use the default
        if (!(sender instanceof Player)) {
            return defaultPermission;
        }

        // Get the player instance
        Player player = (Player) sender;

        // Get the permissions manager, and make sure it's instance is valid

        if (permissionsManager == null)
            return false;

        // Check whether the player has permission, return the result
        return permissionsManager.hasPermission(player, this.permissionNodes, defaultPermission);
    }

    /**
     * Get the default permission if the permission nodes couldn't be used.
     *
     * @return The default permission.
     */
    public DefaultPermission getDefaultPermission() {
        return this.defaultPermission;
    }


    /**
     * Get the default permission for a specified command sender.
     *
     * @param sender The command sender to get the default permission for.
     *
     * @return True if the command sender has permission by default, false otherwise.
     */
    public boolean getDefaultPermissionCommandSender(CommandSender sender) {
        switch (getDefaultPermission()) {
            case ALLOWED:
                return true;

            case OP_ONLY:
                return sender.isOp();

            case NOT_ALLOWED:
            default:
                return false;
        }
    }
}
