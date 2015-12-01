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
    public void shouldStartWithRegularAuthMePrefix() {
        // given
        String requiredPrefix = "authme.";
        String adminPrefix = "authme.admin";

        // when/then
        for (PlayerPermission perm : PlayerPermission.values()) {
            if (!perm.getNode().startsWith(requiredPrefix)) {
                fail("The permission '" + perm + "' does not start with the required prefix '" + requiredPrefix + "'");
            } else if (perm.getNode().startsWith(adminPrefix)) {
                fail("The permission '" + perm + "' should not use a node with the admin-specific prefix '"
                    + adminPrefix + "'");
            }
        }
    }

    @Test
    public void shouldHaveUniqueNodes() {
        // given
        Set<String> nodes = new HashSet<>();

        // when/then
        for (PlayerPermission perm : PlayerPermission.values()) {
            if (nodes.contains(perm.getNode())) {
                fail("More than one enum value defines the node '" + perm.getNode() + "'");
            }
            nodes.add(perm.getNode());
        }
    }
}
