package fr.xephi.authme.permission;

import com.google.common.collect.ImmutableSet;
import fr.xephi.authme.util.StringUtils;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static fr.xephi.authme.TestHelper.getJarFile;
import static org.junit.Assert.fail;

/**
 * Tests that the permissions listed in plugin.yml correspond to the ones in the code.
 */
public class PermissionConsistencyTest {

    /** All classes defining permission nodes. */
    private static final Set<Class<? extends PermissionNode>> PERMISSION_CLASSES = ImmutableSet
        .<Class<? extends PermissionNode>>of(PlayerPermission.class, AdminPermission.class, PlayerStatePermission.class);

    /** Wildcard permissions (present in plugin.yml but not in the codebase). */
    private static final Set<String> PLUGIN_YML_PERMISSIONS_WILDCARDS =
        ImmutableSet.of("authme.admin.*", "authme.player.*", "authme.player.email");

    /** Name of the fields that make up a permission entry in plugin.yml. */
    private static final Set<String> PERMISSION_FIELDS = ImmutableSet.of("description", "default", "children");

    /** All known PermissionNode objects. */
    private static List<PermissionNode> permissionNodes;

    /** All permissions listed in plugin.yml. */
    private static Map<String, PermissionDefinition> pluginYmlPermissions;

    @BeforeClass
    public static void gatherPermissionNodes() {
        permissionNodes = getPermissionsFromClasses();
        pluginYmlPermissions = getPermissionsFromPluginYmlFile();
    }

    @Test
    public void shouldHaveAllPermissionsInPluginYml() {
        // given
        List<String> errors = new ArrayList<>();

        // when
        for (PermissionNode node : permissionNodes) {
            PermissionDefinition permDef = pluginYmlPermissions.get(node.getNode());
            if (permDef == null) {
                errors.add("Permission '" + node.getNode() + "' does not exist in plugin.yml");
            } else if (!node.getDefaultPermission().equals(permDef.expectedDefault)) {
                errors.add("Default value for '" + node.getNode() + "' has different default value");
            }
        }

        // then
        if (!errors.isEmpty()) {
            fail("Found consistency issues!\n" + StringUtils.join("\n", errors));
        }
    }

    @Test
    public void shouldNotHaveUnknownPermissionsInPluginYml() {
        // given
        List<String> errors = new ArrayList<>();

        // when
        for (String key : pluginYmlPermissions.keySet()) {
            if (!PLUGIN_YML_PERMISSIONS_WILDCARDS.contains(key)) {
                if (!doesPermissionExist(key, permissionNodes)) {
                    errors.add("Permission '" + key + "' in plugin.yml does not exist in the codebase");
                }
                // TODO #337: Add else if checking that non-wildcard permissions do not have children
            }
            // TODO #337: Add check that children of wildcard permissions make sense
        }

        // then
        if (!errors.isEmpty()) {
            fail("Found consistency issues!\n" + StringUtils.join("\n", errors));
        }
    }

    private static boolean doesPermissionExist(String key, List<PermissionNode> nodes) {
        for (PermissionNode node : nodes) {
            if (key.equals(node.getNode())) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns all {@link PermissionNode} fields from the permission node classes.
     *
     * @return collection of all permission nodes in the code
     */
    private static List<PermissionNode> getPermissionsFromClasses() {
        List<PermissionNode> nodes = new ArrayList<>();
        for (Class<? extends PermissionNode> clazz : PERMISSION_CLASSES) {
            nodes.addAll(Arrays.<PermissionNode>asList(clazz.getEnumConstants()));
        }
        return Collections.unmodifiableList(nodes);
    }

    /**
     * Returns all permission entries from the plugin.yml file.
     *
     * @return map with the permission entries by permission node
     */
    private static Map<String, PermissionDefinition> getPermissionsFromPluginYmlFile() {
        FileConfiguration pluginFile = YamlConfiguration.loadConfiguration(getJarFile("/plugin.yml"));
        MemorySection permsList = (MemorySection) pluginFile.get("permissions");

        Map<String, PermissionDefinition> permissions = new HashMap<>();
        addChildren(permsList, permissions);
        return permissions;
    }

    /**
     * Recursively visits every MemorySection and creates {@link PermissionDefinition} where applicable.
     *
     * @param node the node to visit
     * @param collection the collection to add constructed permission definitions to
     */
    private static void addChildren(MemorySection node, Map<String, PermissionDefinition> collection) {
        // A MemorySection may have a permission entry, as well as MemorySection children
        boolean hasPermissionEntry = false;
        for (String key : node.getKeys(false)) {
            if (node.get(key) instanceof MemorySection && !"children".equals(key)) {
                addChildren((MemorySection) node.get(key), collection);
            } else if (PERMISSION_FIELDS.contains(key)) {
                hasPermissionEntry = true;
            } else {
                throw new IllegalStateException("Unexpected key '" + key + "'");
            }
        }
        if (hasPermissionEntry) {
            PermissionDefinition permDef = new PermissionDefinition(node);
            collection.put(permDef.node, permDef);
        }
    }

    // TODO #337: Save children to PermissionDefinition objects
    private static final class PermissionDefinition {

        private final String node;
        private final DefaultPermission expectedDefault;

        PermissionDefinition(MemorySection memorySection) {
            this.node = removePermissionsPrefix(memorySection.getCurrentPath());
            this.expectedDefault = mapToDefaultPermission(memorySection.getString("default"));
        }

        private static DefaultPermission mapToDefaultPermission(String defaultDescription) {
            if ("true".equals(defaultDescription)) {
                return DefaultPermission.ALLOWED;
            } else if ("op".equals(defaultDescription)) {
                return DefaultPermission.OP_ONLY;
            } else if ("false".equals(defaultDescription)) {
                return DefaultPermission.NOT_ALLOWED;
            } else if (defaultDescription == null) {
                // Return null: comparison with PermissionNode will always fail
                return null;
            }
            throw new IllegalStateException("Unknown default description '" + defaultDescription + "'");
        }

        private static String removePermissionsPrefix(String path) {
            if (path.startsWith("permissions.")) {
                return path.substring("permissions.".length());
            }
            throw new IllegalStateException("Unexpected path '" + path + "': does not begin with 'permissions.'");
        }
    }

}
