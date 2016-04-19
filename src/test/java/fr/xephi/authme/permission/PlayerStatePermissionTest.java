package fr.xephi.authme.permission;

import org.junit.Test;

import java.util.HashSet;
import java.util.Set;

import static com.google.common.collect.Sets.newHashSet;
import static org.junit.Assert.fail;

/**
 * Test for {@link PlayerStatePermission}.
 */
public class PlayerStatePermissionTest {

    @Test
    public void shouldStartWithAuthMeAdminPrefix() {
        // given
        String requiredPrefix = "authme.";
        Set<String> forbiddenPrefixes = newHashSet("authme.player", "authme.admin");

        // when/then
        for (PlayerStatePermission permission : PlayerStatePermission.values()) {
            if (!permission.getNode().startsWith(requiredPrefix)) {
                fail("The permission '" + permission + "' does not start with the required prefix '"
                    + requiredPrefix + "'");
            } else if (hasAnyPrefix(permission.getNode(), forbiddenPrefixes)) {
                fail("The permission '" + permission + "' should not start with any of " + forbiddenPrefixes);
            }
        }
    }

    @Test
    public void shouldHaveUniqueNodes() {
        // given
        Set<String> nodes = new HashSet<>();

        // when/then
        for (PlayerStatePermission permission : PlayerStatePermission.values()) {
            if (nodes.contains(permission.getNode())) {
                fail("More than one enum value defines the node '" + permission.getNode() + "'");
            }
            nodes.add(permission.getNode());
        }
    }

    private static boolean hasAnyPrefix(String node, Set<String> prefixes) {
        for (String prefix : prefixes) {
            if (node.startsWith(prefix)) {
                return true;
            }
        }
        return false;
    }

}
