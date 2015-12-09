package fr.xephi.authme.permission;

/**
 * Common interface for AuthMe permission nodes.
 */
public interface PermissionNode {

    /**
     * Return the node of the permission, e.g. "authme.player.unregister".
     *
     * @return The name of the permission node
     */
    String getNode();

    /**
     * Return the wildcard node that also grants the permission.
     *
     * @return The wildcard permission node (e.g. "authme.player.*")
     */
    PermissionNode getWildcardNode();

}
