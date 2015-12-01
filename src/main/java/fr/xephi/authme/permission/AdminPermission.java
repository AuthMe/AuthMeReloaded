package fr.xephi.authme.permission;

/**
 * AuthMe admin command permissions.
 */
public enum AdminPermission implements PermissionNode {

    /**
     * Administrator command to register a new user.
     */
    REGISTER("authme.command.admin.register"),

    /**
     * Administrator command to unregister an existing user.
     */
    UNREGISTER("authme.command.admin.unregister"),

    /**
     * Administrator command to force-login an existing user.
     */
    FORCE_LOGIN("authme.command.admin.forcelogin"),

    /**
     * Administrator command to change the password of a user.
     */
    CHANGE_PASSWORD("authme.command.admin.changepassword"),

    /**
     * Administrator command to see the last login date and time of an user.
     */
    LAST_LOGIN("authme.command.admin.lastlogin"),

    /**
     * Administrator command to see all accounts associated with an user.
     */
    ACCOUNTS("authme.command.admin.accounts"),

    /**
     * Administrator command to get the email address of an user, if set.
     */
    GET_EMAIL("authme.command.admin.getemail"),

    /**
     * Administrator command to set or change the email adress of an user.
     */
    CHANGE_EMAIL("authme.command.admin.changemail"),

    /**
     * Administrator command to get the last known IP of an user.
     */
    GET_IP("authme.command.admin.getip"),

    /**
     * Administrator command to teleport to the AuthMe spawn.
     */
    SPAWN("authme.command.admin.spawn"),

    /**
     * Administrator command to set the AuthMe spawn.
     */
    SET_SPAWN("authme.command.admin.setspawn"),

    /**
     * Administrator command to teleport to the first AuthMe spawn.
     */
    FIRST_SPAWN("authme.command.admin.firstspawn"),

    /**
     * Administrator command to set the first AuthMe spawn.
     */
    SET_FIRST_SPAWN("authme.command.admin.setfirstspawn"),

    /**
     * Administrator command to purge old user data.
     */
    PURGE("authme.command.admin.purge"),

    /**
     * Administrator command to purge the last position of an user.
     */
    PURGE_LAST_POSITION("authme.command.admin.purgelastpos"),

    /**
     * Administrator command to purge all data associated with banned players.
     */
    PURGE_BANNED_PLAYERS("authme.command.admin.purgebannedplayers"),

    /**
     * Administrator command to toggle the AntiBot protection status.
     */
    SWITCH_ANTIBOT("authme.command.admin.switchantibot"),

    /**
     * Administrator command to reload the plugin configuration.
     */
    RELOAD("authme.command.admin.reload");

    /**
     * Permission node.
     */
    private String node;

    /**
     * Get the permission node.
     * @return
     */
    @Override
    public String getNode() {
        return node;
    }

    /**
     * Constructor.
     *
     * @param node Permission node.
     */
    AdminPermission(String node) {
        this.node = node;
    }
}
