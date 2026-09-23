package fr.xephi.authme.permission;

/**
 * Test for {@link PlayerPermission}.
 */
class PlayerPermissionTest extends AbstractPermissionsEnumTest {

    @Override
    protected PermissionNode[] getPermissionNodes() {
        return PlayerPermission.values();
    }

    @Override
    protected String getRequiredPrefix() {
        return "authme.player.";
    }
}
