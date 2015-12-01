package fr.xephi.authme.permission;

/**
 * AuthMe player permission nodes, for regular players.
 */
public enum PlayerPermission implements PermissionNode {

    BYPASS_ANTIBOT("authme.command.player.bypassantibot"),

    IS_VIP("authme.command.player.vip"),

    LOGIN("authme.command.player.login"),

    LOGOUT("authme.command.player.logout"),

    REGISTER("authme.command.player.register"),

    UNREGISTER("authme.command.player.unregister"),

    CHANGE_PASSWORD("authme.command.player.changepassword"),

    ADD_EMAIL("authme.command.player.email.add"),

    CHANGE_EMAIL("authme.command.player.email.change"),

    RECOVER_EMAIL("authme.command.player.email.recover"),

    CAPTCHA("authme.command.player.captcha"),

    CONVERTER("authme.command.player.converter"),

    CAN_LOGIN_BE_FORCED("authme.command.player.canbeforced"),

    BYPASS_FORCE_SURVIVAL("authme.command.player.bypassforcesurvival"),

    ALLOW_MULTIPLE_ACCOUNTS("authme.command.player.allow2accounts"),

    SEE_OTHER_ACCOUNTS("authme.command.player.seeotheraccounts");

    private String node;

    @Override
    public String getNode() {
        return node;
    }

    PlayerPermission(String node) {
        this.node = node;
    }
}
