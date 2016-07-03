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
    public void shouldStartWithPlayerPrefix() {
        // given
        String playerBranch = "authme.player.";

        // when/then
        for (PlayerPermission permission : PlayerPermission.values()) {
            if (!permission.getNode().startsWith(playerBranch)) {
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
            if (!nodes.add(permission.getNode())) {
                fail("More than one enum value defines the node '" + permission.getNode() + "'");
            }
        }
    }
}
