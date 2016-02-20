package fr.xephi.authme.permission;

/**
 * Permission nodes that give a player a status (e.g. VIP)
 * or grant them more freedom (e.g. less restrictions).
 */
public enum PlayerStatePermission implements PermissionNode {

    /**
     * Permission node to bypass AntiBot protection.
     */
    BYPASS_ANTIBOT("authme.bypassantibot"),

    /**
     * Permission for users to bypass force-survival mode.
     */
    BYPASS_FORCE_SURVIVAL("authme.bypassforcesurvival"),

    /**
     * Permission node to identify VIP users.
     */
    IS_VIP("authme.vip"),

    /**
     * Permission to be able to register multiple accounts.
     */
    ALLOW_MULTIPLE_ACCOUNTS("authme.allowmultipleaccounts");

    /**
     * The permission node.
     */
    private String node;

    /**
     * Constructor.
     *
     * @param node Permission node.
     */
    PlayerStatePermission(String node) {
        this.node = node;
    }

    @Override
    public String getNode() {
        return node;
    }
}
