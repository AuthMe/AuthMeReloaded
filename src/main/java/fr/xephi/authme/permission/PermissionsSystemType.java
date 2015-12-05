package fr.xephi.authme.permission;

/**
 * Enum representing the permissions systems AuthMe supports.
 */
public enum PermissionsSystemType {

    NONE("None"),

    PERMISSIONS_EX("PermissionsEx"),

    PERMISSIONS_BUKKIT("Permissions Bukkit"),

    B_PERMISSIONS("bPermissions"),

    ESSENTIALS_GROUP_MANAGER("Essentials Group Manager"),

    Z_PERMISSIONS("zPermissions"),

    VAULT("Vault"),

    PERMISSIONS("Permissions");

    public final String name;

    /**
     * Constructor for PermissionsSystemType.
     *
     * @param name The name the permissions manager goes by
     */
    PermissionsSystemType(String name) {
        this.name = name;
    }

    public String getName() {
        return this.name;
    }
}
