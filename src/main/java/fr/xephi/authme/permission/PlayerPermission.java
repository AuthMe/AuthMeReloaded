package fr.xephi.authme.permission;

/**
 * AuthMe player permission nodes, for regular players.
 */
public enum PlayerPermission implements PermissionNode {

    /**
     * Command permission to login.
     */
    LOGIN("authme.player.login", DefaultPermission.ALLOWED),

    /**
     * Command permission to logout.
     */
    LOGOUT("authme.player.logout", DefaultPermission.ALLOWED),

    /**
     * Command permission to register.
     */
    REGISTER("authme.player.register", DefaultPermission.ALLOWED),

    /**
     * Command permission to unregister.
     */
    UNREGISTER("authme.player.unregister", DefaultPermission.ALLOWED),

    /**
     * Command permission to change the password.
     */
    CHANGE_PASSWORD("authme.player.changepassword", DefaultPermission.ALLOWED),

    /**
     * Command permission to add an email address.
     */
    ADD_EMAIL("authme.player.email.add", DefaultPermission.ALLOWED),

    /**
     * Command permission to change the email address.
     */
    CHANGE_EMAIL("authme.player.email.change", DefaultPermission.ALLOWED),

    /**
     * Command permission to recover an account using it's email address.
     */
    RECOVER_EMAIL("authme.player.email.recover", DefaultPermission.ALLOWED),

    /**
     * Command permission to use captcha.
     */
    CAPTCHA("authme.player.captcha", DefaultPermission.ALLOWED),

    /**
     * Permission for users a login can be forced to.
     */
    CAN_LOGIN_BE_FORCED("authme.player.canbeforced", DefaultPermission.ALLOWED),

    /**
     * Permission to use to see own other accounts.
     */
    SEE_OWN_ACCOUNTS("authme.player.seeownaccounts", DefaultPermission.ALLOWED),

    /**
     * Permission to bypass the purging process
     */
    BYPASS_PURGE("authme.player.bypasspurge", DefaultPermission.NOT_ALLOWED);

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
    PlayerPermission(String node, DefaultPermission defaultPermission) {
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
