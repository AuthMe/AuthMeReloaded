package fr.xephi.authme.permission;

/**
 * AuthMe admin permissions.
 */
public enum AdminPermission implements PermissionNode {

    REGISTER("authme.admin.register"),

    UNREGISTER("authme.admin.unregister"),

    FORCE_LOGIN("authme.admin.forcelogin"),

    CHANGE_PASSWORD("authme.admin.changepassword"),

    LAST_LOGIN("authme.admin.lastlogin"),

    ACCOUNTS("authme.admin.accounts"),

    GET_EMAIL("authme.admin.getemail"),

    CHANGE_EMAIL("authme.admin.chgemail"),

    GET_IP("authme.admin.getip"),

    SPAWN("authme.admin.spawn"),

    SET_SPAWN("authme.admin.setspawn"),

    FIRST_SPAWN("authme.admin.firstspawn"),

    SET_FIRST_SPAWN("authme.admin.setfirstspawn"),

    PURGE("authme.admin.purge"),

    PURGE_LAST_POSITION("authme.admin.purgelastpos"),

    PURGE_BANNED_PLAYERS("authme.admin.purgebannedplayers"),

    SWITCH_ANTIBOT("authme.admin.switchantibot"),

    RELOAD("authme.admin.reload");

    private String node;

    @Override
    public String getNode() {
        return node;
    }

    AdminPermission(String node) {
        this.node = node;
    }

}
