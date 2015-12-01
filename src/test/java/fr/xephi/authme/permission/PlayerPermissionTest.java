package fr.xephi.authme.permission;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.fail;

/**
 * Test for {@link PlayerPermission}.
 */
public class PlayerPermissionTest {

    @Test
    public void shouldStartWithAuthMePrefix() {
        // given
        String requiredPrefix = "authme.";

        // when/then
        for (PlayerPermission permission : PlayerPermission.values()) {
            if (!permission.getNode().startsWith(requiredPrefix)) {
                fail("The permission '" + permission + "' does not start with the required prefix '" + requiredPrefix + "'");
            }
        }
    }

    @Test
    public void shouldContainPlayerBranch() {
        // given
        String playerBranch = ".player.";
        String adminBranch = ".admin.";

        // when/then
        for (PlayerPermission permission : PlayerPermission.values()) {
            if (permission.getNode().contains(adminBranch)) {
                fail("The permission '" + permission + "' should not use a node with the admin-specific branch '"
                    + adminBranch + "'");

            } else if (!permission.getNode().contains(playerBranch)) {
                fail("The permission '" + permission + "' should use a node with the player-specific branch '"
                    + playerBranch + "'");
            }
        }
    }

    @Test
    public void shouldHaveUniqueNodes() {
        // given
        Set<String> nodes = new HashSet<>();

        // when/then
        for (PlayerPermission permission : PlayerPermission.values()) {
            if (nodes.contains(permission.getNode())) {
                fail("More than one enum value defines the node '" + permission.getNode() + "'");
            }
            nodes.add(permission.getNode());
        }
    }
}
