package fr.xephi.authme.permission;

/**
 * Permissions for the debug sections (/authme debug).
 */
public enum DebugSectionPermissions implements PermissionNode {

    /** General permission to use the /authme debug command. */
    DEBUG_COMMAND("authme.debug.command"),

    /** Permission to use the country lookup section. */
    COUNTRY_LOOKUP("authme.debug.country"),

    /** Permission to use the stats section. */
    DATA_STATISTICS("authme.debug.stats"),

    /** Permission to use the permission checker. */
    HAS_PERMISSION_CHECK("authme.debug.perm"),

    /** Permission to use sample validation. */
    INPUT_VALIDATOR("authme.debug.valid"),

    /** Permission to use the limbo data viewer. */
    LIMBO_PLAYER_VIEWER("authme.debug.limbo"),

    /** Permission to view permission groups. */
    PERM_GROUPS("authme.debug.group"),

    /** Permission to view data from the database. */
    PLAYER_AUTH_VIEWER("authme.debug.db"),

    /** Permission to change nullable status of MySQL columns. */
    MYSQL_DEFAULT_CHANGER("authme.debug.mysqldef"),

    /** Permission to view spawn information. */
    SPAWN_LOCATION("authme.debug.spawn"),

    /** Permission to use the test email sender. */
    TEST_EMAIL("authme.debug.mail");

    private final String node;

    /**
     * Constructor.
     *
     * @param node the permission node
     */
    DebugSectionPermissions(String node) {
        this.node = node;
    }

    @Override
    public String getNode() {
        return node;
    }

    @Override
    public DefaultPermission getDefaultPermission() {
        return DefaultPermission.OP_ONLY;
    }
}
