package fr.xephi.authme.command;

import fr.xephi.authme.permission.DefaultPermission;
import fr.xephi.authme.permission.PermissionNode;

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
     * Get the default permission if the permission nodes couldn't be used.
     *
     * @return The default permission.
     */
    public DefaultPermission getDefaultPermission() {
        return this.defaultPermission;
    }


}
