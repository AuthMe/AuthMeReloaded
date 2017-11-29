package fr.xephi.authme.permission;

/**
 * AuthMe admin command permissions.
 */
public enum AdminPermission implements PermissionNode {

    /**
     * Administrator command to register a new user.
     */
    REGISTER("authme.admin.register"),

    /**
     * Administrator command to unregister an existing user.
     */
    UNREGISTER("authme.admin.unregister"),

    /**
     * Administrator command to force-login an existing user.
     */
    FORCE_LOGIN("authme.admin.forcelogin"),

    /**
     * Administrator command to change the password of a user.
     */
    CHANGE_PASSWORD("authme.admin.changepassword"),

    /**
     * Administrator command to see the last login date and time of a user.
     */
    LAST_LOGIN("authme.admin.lastlogin"),

    /**
     * Administrator command to see all accounts associated with a user.
     */
    ACCOUNTS("authme.admin.accounts"),

    /**
     * Administrator command to get the email address of a user, if set.
     */
    GET_EMAIL("authme.admin.getemail"),

    /**
     * Administrator command to set or change the email address of a user.
     */
    CHANGE_EMAIL("authme.admin.changemail"),

    /**
     * Administrator command to get the last known IP of a user.
     */
    GET_IP("authme.admin.getip"),

    /**
     * Administrator command to see the last recently logged in players.
     */
    SEE_RECENT_PLAYERS("authme.admin.seerecent"),

    /**
     * Administrator command to teleport to the AuthMe spawn.
     */
    SPAWN("authme.admin.spawn"),

    /**
     * Administrator command to set the AuthMe spawn.
     */
    SET_SPAWN("authme.admin.setspawn"),

    /**
     * Administrator command to teleport to the first AuthMe spawn.
     */
    FIRST_SPAWN("authme.admin.firstspawn"),

    /**
     * Administrator command to set the first AuthMe spawn.
     */
    SET_FIRST_SPAWN("authme.admin.setfirstspawn"),

    /**
     * Administrator command to purge old user data.
     */
    PURGE("authme.admin.purge"),

    /**
     * Administrator command to purge the last position of a user.
     */
    PURGE_LAST_POSITION("authme.admin.purgelastpos"),

    /**
     * Administrator command to purge all data associated with banned players.
     */
    PURGE_BANNED_PLAYERS("authme.admin.purgebannedplayers"),

    /**
     * Administrator command to purge a given player.
     */
    PURGE_PLAYER("authme.admin.purgeplayer"),

    /**
     * Administrator command to toggle the AntiBot protection status.
     */
    SWITCH_ANTIBOT("authme.admin.switchantibot"),

    /**
     * Administrator command to convert old or other data to AuthMe data.
     */
    CONVERTER("authme.admin.converter"),

    /**
     * Administrator command to reload the plugin configuration.
     */
    RELOAD("authme.admin.reload"),

    /**
     * Permission to see Antibot messages.
     */
    ANTIBOT_MESSAGES("authme.admin.antibotmessages"),

    /**
     * Permission to use the update messages command.
     */
    UPDATE_MESSAGES("authme.admin.updatemessages"),

    /**
     * Permission to see the other accounts of the players that log in.
     */
    SEE_OTHER_ACCOUNTS("authme.admin.seeotheraccounts"),

    /**
     * Allows to use the backup command.
     */
    BACKUP("authme.admin.backup");

    /**
     * The permission node.
     */
    private String node;

    /**
     * Constructor.
     *
     * @param node Permission node.
     */
    AdminPermission(String node) {
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
