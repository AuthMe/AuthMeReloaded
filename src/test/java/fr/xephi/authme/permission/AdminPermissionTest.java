package fr.xephi.authme.permission;

/**
 * Test for {@link AdminPermission}.
 */
class AdminPermissionTest extends AbstractPermissionsEnumTest {

    @Override
    protected PermissionNode[] getPermissionNodes() {
        return AdminPermission.values();
    }

    @Override
    protected String getRequiredPrefix() {
        return "authme.admin.";
    }

}
