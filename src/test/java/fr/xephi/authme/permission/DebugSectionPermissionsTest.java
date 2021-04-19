package fr.xephi.authme.permission;

/**
 * Test for {@link DebugSectionPermissions}.
 */
class DebugSectionPermissionsTest extends AbstractPermissionsEnumTest {

    @Override
    protected PermissionNode[] getPermissionNodes() {
        return DebugSectionPermissions.values();
    }

    @Override
    protected String getRequiredPrefix() {
        return "authme.debug.";
    }
}
