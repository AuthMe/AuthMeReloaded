package fr.xephi.authme.permission;

/**
 * Common interface for AuthMe permission nodes.
 */
public interface PermissionNode {

    /** Return the node of the permission, e.g. "authme.unregister". */
    String getNode();

}
