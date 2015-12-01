package fr.xephi.authme.permission;

/**
 * AuthMe admin command permissions.
 */
public enum AdminPermission implements PermissionNode {

    REGISTER("authme.command.admin.register"),

    UNREGISTER("authme.command.admin.unregister"),

    FORCE_LOGIN("authme.command.admin.forcelogin"),

    CHANGE_PASSWORD("authme.command.admin.changepassword"),

    LAST_LOGIN("authme.command.admin.lastlogin"),

    ACCOUNTS("authme.command.admin.accounts"),

    GET_EMAIL("authme.command.admin.getemail"),

    CHANGE_EMAIL("authme.command.admin.changemail"),

    GET_IP("authme.command.admin.getip"),

    SPAWN("authme.command.admin.spawn"),

    SET_SPAWN("authme.command.admin.setspawn"),

    FIRST_SPAWN("authme.command.admin.firstspawn"),

    SET_FIRST_SPAWN("authme.command.admin.setfirstspawn"),

    PURGE("authme.command.admin.purge"),

    PURGE_LAST_POSITION("authme.command.admin.purgelastpos"),

    PURGE_BANNED_PLAYERS("authme.command.admin.purgebannedplayers"),

    SWITCH_ANTIBOT("authme.command.admin.switchantibot"),

    RELOAD("authme.command.admin.reload");

    private String node;

    @Override
    public String getNode() {
        return node;
    }

    AdminPermission(String node) {
        this.node = node;
    }
}
