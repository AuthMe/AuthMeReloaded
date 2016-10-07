package fr.xephi.authme.permission;

/**
 * AuthMe admin command permissions.
 */
public enum AdminPermission implements PermissionNode {

    /**
     * Administrator command to register a new user.
     */
    REGISTER("authme.admin.register", DefaultPermission.OP_ONLY),

    /**
     * Administrator command to unregister an existing user.
     */
    UNREGISTER("authme.admin.unregister", DefaultPermission.OP_ONLY),

    /**
     * Administrator command to force-login an existing user.
     */
    FORCE_LOGIN("authme.admin.forcelogin", DefaultPermission.OP_ONLY),

    /**
     * Administrator command to change the password of a user.
     */
    CHANGE_PASSWORD("authme.admin.changepassword", DefaultPermission.OP_ONLY),

    /**
     * Administrator command to see the last login date and time of a user.
     */
    LAST_LOGIN("authme.admin.lastlogin", DefaultPermission.OP_ONLY),

    /**
     * Administrator command to see all accounts associated with a user.
     */
    ACCOUNTS("authme.admin.accounts", DefaultPermission.OP_ONLY),

    /**
     * Administrator command to get the email address of a user, if set.
     */
    GET_EMAIL("authme.admin.getemail", DefaultPermission.OP_ONLY),

    /**
     * Administrator command to set or change the email address of a user.
     */
    CHANGE_EMAIL("authme.admin.changemail", DefaultPermission.OP_ONLY),

    /**
     * Administrator command to get the last known IP of a user.
     */
    GET_IP("authme.admin.getip", DefaultPermission.OP_ONLY),

    /**
     * Administrator command to teleport to the AuthMe spawn.
     */
    SPAWN("authme.admin.spawn", DefaultPermission.OP_ONLY),

    /**
     * Administrator command to set the AuthMe spawn.
     */
    SET_SPAWN("authme.admin.setspawn", DefaultPermission.OP_ONLY),

    /**
     * Administrator command to teleport to the first AuthMe spawn.
     */
    FIRST_SPAWN("authme.admin.firstspawn", DefaultPermission.OP_ONLY),

    /**
     * Administrator command to set the first AuthMe spawn.
     */
    SET_FIRST_SPAWN("authme.admin.setfirstspawn", DefaultPermission.OP_ONLY),

    /**
     * Administrator command to purge old user data.
     */
    PURGE("authme.admin.purge", DefaultPermission.OP_ONLY),

    /**
     * Administrator command to purge the last position of a user.
     */
    PURGE_LAST_POSITION("authme.admin.purgelastpos", DefaultPermission.OP_ONLY),

    /**
     * Administrator command to purge all data associated with banned players.
     */
    PURGE_BANNED_PLAYERS("authme.admin.purgebannedplayers", DefaultPermission.OP_ONLY),

    /**
     * Administrator command to toggle the AntiBot protection status.
     */
    SWITCH_ANTIBOT("authme.admin.switchantibot", DefaultPermission.OP_ONLY),

    /**
     * Administrator command to convert old or other data to AuthMe data.
     */
    CONVERTER("authme.admin.converter", DefaultPermission.OP_ONLY),

    /**
     * Administrator command to reload the plugin configuration.
     */
    RELOAD("authme.admin.reload", DefaultPermission.OP_ONLY),

    /**
     * Permission to see Antibot messages.
     */
    ANTIBOT_MESSAGES("authme.admin.antibotmessages", DefaultPermission.OP_ONLY),

    /**
     * Permission to see the other accounts of the players that log in.
     */
    SEE_OTHER_ACCOUNTS("authme.admin.seeotheraccounts", DefaultPermission.OP_ONLY);

    /**
     * The permission node.
     */
    private String node;

    /**
     * The default permission level
     */
    private DefaultPermission defaultPermission;

    /**
     * Constructor.
     *
     * @param node Permission node.
     */
    AdminPermission(String node, DefaultPermission defaultPermission) {
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
