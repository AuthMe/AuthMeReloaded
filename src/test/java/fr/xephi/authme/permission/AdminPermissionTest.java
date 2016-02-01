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
    public void shouldStartWithAuthMeAdminPrefix() {
        // given
        String requiredPrefix = "authme.admin.";

        // when/then
        for (AdminPermission permission : AdminPermission.values()) {
            if (!permission.getNode().startsWith(requiredPrefix)) {
                fail("The permission '" + permission + "' does not start with the required prefix '"
                    + requiredPrefix + "'");
            }
        }
    }

    @Test
    public void shouldHaveUniqueNodes() {
        // given
        Set<String> nodes = new HashSet<>();

        // when/then
        for (AdminPermission permission : AdminPermission.values()) {
            if (nodes.contains(permission.getNode())) {
                fail("More than one enum value defines the node '" + permission.getNode() + "'");
            }
            nodes.add(permission.getNode());
        }
    }

}
