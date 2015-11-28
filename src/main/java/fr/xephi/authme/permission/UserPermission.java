package fr.xephi.authme.permission;

/**
 * AuthMe user permission nodes.
 */
public enum UserPermission implements PermissionsNode {

    BYPASS_ANTIBOT("authme.bypassantibot"),

    IS_VIP("authme.vip"),

    LOGIN("authme.login"),

    LOGOUT("authme.logout"),

    REGISTER("authme.register"),

    UNREGISTER("authme.unregister"),

    CHANGE_PASSWORD("authme.changepassword"),

    ADD_EMAIL("authme.email.add"),

    CHANGE_EMAIL("authme.email.change"),

    RECOVER_EMAIL("authme.email.recover"),

    CAPTCHA("authme.captcha"),

    CONVERTER("authme.converter"),

    CAN_LOGIN_BE_FORCED("authme.canbeforced"),

    BYPASS_FORCE_SURVIVAL("authme.bypassforcesurvival"),

    ALLOW_MULTIPLE_ACCOUNTS("authme.allow2accounts"),

    SEE_OTHER_ACCOUNTS("authme.seeOtherAccounts");

    private String node;

    @Override
    public String getNode() {
        return node;
    }

    UserPermission(String node) {
        this.node = node;
    }


}
