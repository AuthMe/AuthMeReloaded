package fr.xephi.authme.permission;

/**
 * AuthMe player permission nodes, for regular players.
 */
public enum PlayerPermission implements PermissionNode {

    /**
     * Command permission to login.
     */
    LOGIN("authme.player.login"),

    /**
     * Command permission to logout.
     */
    LOGOUT("authme.player.logout"),

    /**
     * Command permission to register.
     */
    REGISTER("authme.player.register"),

    /**
     * Command permission to unregister.
     */
    UNREGISTER("authme.player.unregister"),

    /**
     * Command permission to change the password.
     */
    CHANGE_PASSWORD("authme.player.changepassword"),

    /**
     * Command permission to see the own email address.
     */
    SEE_EMAIL("authme.player.email.see"),

    /**
     * Command permission to add an email address.
     */
    ADD_EMAIL("authme.player.email.add"),

    /**
     * Command permission to change the email address.
     */
    CHANGE_EMAIL("authme.player.email.change"),

    /**
     * Command permission to recover an account using its email address.
     */
    RECOVER_EMAIL("authme.player.email.recover"),

    /**
     * Command permission to use captcha.
     */
    CAPTCHA("authme.player.captcha"),

    /**
     * Permission for users a login can be forced to.
     */
    CAN_LOGIN_BE_FORCED("authme.player.canbeforced"),

    /**
     * Permission to use to see own other accounts.
     */
    SEE_OWN_ACCOUNTS("authme.player.seeownaccounts"),

    /**
     * Permission to use the email verification codes feature.
     */
    VERIFICATION_CODE("authme.player.security.verificationcode"),

    /**
     * Permission that enables on join quick commands checks for the player.
     */
    QUICK_COMMANDS_PROTECTION("authme.player.protection.quickcommandsprotection"),

    /**
     * Permission to enable two-factor authentication.
     */
    ENABLE_TWO_FACTOR_AUTH("authme.player.totpadd"),

    /**
     * Permission to disable two-factor authentication.
     */
    DISABLE_TWO_FACTOR_AUTH("authme.player.totpremove");

    /**
     * The permission node.
     */
    private String node;

    /**
     * Constructor.
     *
     * @param node Permission node.
     */
    PlayerPermission(String node) {
        this.node = node;
    }

    @Override
    public String getNode() {
        return node;
    }

    @Override
    public DefaultPermission getDefaultPermission() {
        return DefaultPermission.ALLOWED;
    }

}
