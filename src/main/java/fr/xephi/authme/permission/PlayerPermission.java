package fr.xephi.authme.permission;

/**
 * AuthMe player permission nodes, for regular players.
 */
public enum PlayerPermission implements PermissionNode {

    /**
     * Permission node to bypass AntiBot protection.
     */
    BYPASS_ANTIBOT("authme.player.bypassantibot"),

    /**
     * Permission node to identify VIP users.
     */
    IS_VIP("authme.player.vip"),

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
     * Command permission to add an email address.
     */
    ADD_EMAIL("authme.player.email.add"),

    /**
     * Command permission to change the email address.
     */
    CHANGE_EMAIL("authme.player.email.change"),

    /**
     * Command permission to recover an account using it's email address.
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
     * Permission for users to bypass force-survival mode.
     */
    BYPASS_FORCE_SURVIVAL("authme.player.bypassforcesurvival"),

    /**
     * Permission for users to allow two accounts.
     */
    ALLOW_MULTIPLE_ACCOUNTS("authme.player.allow2accounts"),

    /**
     * Permission for user to see other accounts.
     */
    SEE_OTHER_ACCOUNTS("authme.player.seeotheraccounts"),

	/**
	 * Permission to use all player (non-admin) commands.
	 */
	PLAYER_ALL("authme.player.*"),

	/**
	 * Permission to use to see own other accounts.
	 */
	SEE_OWN_ACCOUNTS("authme.player.showotheraccounts");

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
    public PermissionNode getWildcardNode() {
        return PLAYER_ALL;
    }
}
