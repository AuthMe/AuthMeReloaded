package fr.xephi.authme.permission;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.Assert.fail;

/**
 * Test for {@link AdminPermission}.
 */
public class AdminPermissionTest {

    @Test
    public void shouldStartWithAuthMePrefix() {
        // given
        String requiredPrefix = "authme.";

        // when/then
        for (AdminPermission perm : AdminPermission.values()) {
            if (!perm.getNode().startsWith(requiredPrefix)) {
                fail("The permission '" + perm + "' does not start with the required prefix '" + requiredPrefix + "'");
            }
        }
    }

    @Test
    public void shouldContainAdminBranch() {
        // given
        String requiredBranch = ".admin.";

        // when/then
        for (AdminPermission perm : AdminPermission.values()) {
            if (!perm.getNode().contains(requiredBranch)) {
                fail("The permission '" + perm + "' does not contain with the required branch '" + requiredBranch + "'");
            }
        }
    }

    @Test
    public void shouldHaveUniqueNodes() {
        // given
        Set<String> nodes = new HashSet<>();

        // when/then
        for (AdminPermission perm : AdminPermission.values()) {
            if (nodes.contains(perm.getNode())) {
                fail("More than one enum value defines the node '" + perm.getNode() + "'");
            }
            nodes.add(perm.getNode());
        }
    }

}
