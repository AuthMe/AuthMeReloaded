package fr.xephi.authme.permission;

/**
 * Sample permission nodes for testing.
 */
public enum TestPermissions implements PermissionNode {

    LOGIN("authme.login", DefaultPermission.ALLOWED),

    DELETE_USER("authme.admin.delete", DefaultPermission.OP_ONLY),

    WORLD_DOMINATION("global.own", DefaultPermission.NOT_ALLOWED);


    private final String node;
    private final DefaultPermission defaultPermission;

    TestPermissions(String node, DefaultPermission defaultPermission) {
        this.node = node;
        this.defaultPermission = defaultPermission;
    }

    @Override
    public String getNode() {
        return node;
    }

    @Override
    public DefaultPermission getDefaultPermission() {
        return defaultPermission;
    }

}
