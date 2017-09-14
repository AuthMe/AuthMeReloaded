package fr.xephi.authme.permission;

/**
 * Enum representing the permissions systems AuthMe supports.
 */
public enum PermissionsSystemType {

    /**
     *  LuckPerms.
     */
    LUCK_PERMS("LuckPerms", "LuckPerms"),

    /**
     * Permissions Ex.
     */
    PERMISSIONS_EX("PermissionsEx", "PermissionsEx"),

    /**
     * bPermissions.
     */
    B_PERMISSIONS("bPermissions", "bPermissions"),

    /**
     * zPermissions.
     */
    Z_PERMISSIONS("zPermissions", "zPermissions"),

    /**
     * Vault.
     */
    VAULT("Vault", "Vault");

    /**
     * The display name of the permissions system.
     */
    private String displayName;

    /**
     * The name of the permissions system plugin.
     */
    private String pluginName;

    /**
     * Constructor for PermissionsSystemType.
     *
     * @param displayName Display name of the permissions system.
     * @param pluginName Name of the plugin.
     */
    PermissionsSystemType(String displayName, String pluginName) {
        this.displayName = displayName;
        this.pluginName = pluginName;
    }

    /**
     * Get the display name of the permissions system.
     *
     * @return Display name.
     */
    public String getDisplayName() {
        return this.displayName;
    }

    /**
     * Return the plugin name.
     *
     * @return Plugin name.
     */
    public String getPluginName() {
        return this.pluginName;
    }

    /**
     * Cast the permissions system type to a string.
     *
     * @return The display name of the permissions system.
     */
    @Override
    public String toString() {
        return getDisplayName();
    }

    /**
     * Check if a given plugin is a permissions system.
     *
     * @param name The name of the plugin to check.
     * @return If the plugin is a valid permissions system.
     */
    public static boolean isPermissionSystem(String name) {
        for (PermissionsSystemType permissionsSystemType : values()) {
            if (permissionsSystemType.pluginName.equals(name)) {
                return true;
            }
        }
        return false;
    }
}
