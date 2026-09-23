package fr.xephi.authme.permission;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Sets;
import fr.xephi.authme.ClassCollector;
import fr.xephi.authme.TestHelper;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static fr.xephi.authme.TestHelper.getJarFile;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Tests that the permissions listed in plugin.yml correspond to the ones in the code.
 */
class PermissionConsistencyTest {

    /** Wildcard permissions (present in plugin.yml but not in the codebase). */
    private static final Set<String> PLUGIN_YML_PERMISSIONS_WILDCARDS =
        ImmutableSet.of("authme.admin.*", "authme.player.*", "authme.player.email", "authme.debug");

    /** Name of the fields that make up a permission entry in plugin.yml. */
    private static final Set<String> PERMISSION_FIELDS = ImmutableSet.of("description", "default", "children");

    /** All classes defining permission nodes. */
    private static List<Class<? extends PermissionNode>> permissionClasses;

    /** All known PermissionNode objects. */
    private static List<PermissionNode> permissionNodes;

    /** All permissions listed in plugin.yml. */
    private static Map<String, PermissionDefinition> pluginYmlPermissions;

    @BeforeAll
    static void gatherPermissionNodes() {
        permissionClasses = new ClassCollector(TestHelper.SOURCES_FOLDER, TestHelper.PROJECT_ROOT + "permission")
            .collectClasses(PermissionNode.class).stream()
            .filter(clz -> !clz.isInterface())
            .collect(Collectors.toList());
        permissionNodes = getPermissionsFromClasses();
        pluginYmlPermissions = getPermissionsFromPluginYmlFile();
    }

    @Test
    void shouldHaveAllPermissionsInPluginYml() {
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
            fail("Found consistency issues!\n" + String.join("\n", errors));
        }
    }

    @Test
    void shouldNotHaveUnknownPermissionsInPluginYml() {
        // given
        List<String> errors = new ArrayList<>();

        // when
        for (PermissionDefinition def : pluginYmlPermissions.values()) {
            if (PLUGIN_YML_PERMISSIONS_WILDCARDS.contains(def.node)) {
                validateChildren(def, errors);
            } else {
                if (!doesPermissionExist(def.node, permissionNodes)) {
                    errors.add("Permission '" + def.node + "' in plugin.yml does not exist in the codebase");
                } else if (!def.children.isEmpty()) {
                    errors.add("Permission '" + def.node + "' has children in plugin.yml "
                        + "but is not a wildcard permission");
                }
            }
        }

        // then
        if (!errors.isEmpty()) {
            fail("Found consistency issues!\n" + String.join("\n", errors));
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
        for (Class<? extends PermissionNode> clazz : permissionClasses) {
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
        return ImmutableMap.copyOf(permissions);
    }

    /**
     * Recursively visits every MemorySection and creates a {@link PermissionDefinition} when applicable.
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

    /**
     * Validates that the given permission definition's children, if present, are children of the wildcard permission.
     *
     * @param definition the permission definition to process
     * @param errorList list of errors to add an entry to in case of unsuccessful check
     */
    private static void validateChildren(PermissionDefinition definition, List<String> errorList) {
        // Replace ending .* in path if present, e.g. authme.player.* -> authme.player
        // Add ending '.' since we want all children to be children, i.e. authme.playertest would not be OK
        String root = definition.node.replaceAll("\\.\\*$", "") + ".";
        Set<String> expectedChildren = permissionNodes.stream().map(PermissionNode::getNode)
            .filter(n -> n.startsWith(root)).collect(Collectors.toSet());
        Set<String> missingChildren = Sets.difference(expectedChildren, definition.children);
        Set<String> unexpectedChildren = Sets.difference(definition.children, expectedChildren);

        if (!missingChildren.isEmpty()) {
            errorList.add("Node '" + definition.node + "' has missing children: " + missingChildren);
        }
        if (!unexpectedChildren.isEmpty()) {
            errorList.add("Node '" + definition.node + "' has unexpected children: " + unexpectedChildren);
        }
    }

    /**
     * Represents a permission entry in plugin.yml.
     */
    private static final class PermissionDefinition {

        private final String node;
        private final Set<String> children;
        private final DefaultPermission expectedDefault;

        PermissionDefinition(MemorySection memorySection) {
            this.node = removePermissionsPrefix(memorySection.getCurrentPath());
            this.expectedDefault = mapToDefaultPermission(memorySection.getString("default"));

            if (memorySection.get("children") instanceof MemorySection) {
                List<String> children = new ArrayList<>();
                collectChildren((MemorySection) memorySection.get("children"), children);
                this.children = removeStart(memorySection.getCurrentPath() + ".children.", children);
            } else {
                this.children = Collections.emptySet();
            }
        }

        /**
         * Returns the {@link DefaultPermission} corresponding to the {@code default} value in plugin.yml.
         *
         * @param defaultValue the value of the default
         * @return the according DefaultPermission object, or null
         */
        private static DefaultPermission mapToDefaultPermission(String defaultValue) {
            if ("true".equals(defaultValue)) {
                return DefaultPermission.ALLOWED;
            } else if ("op".equals(defaultValue)) {
                return DefaultPermission.OP_ONLY;
            } else if ("false".equals(defaultValue)) {
                return DefaultPermission.NOT_ALLOWED;
            } else if (defaultValue == null) {
                // Return null: comparison with the default of the PermissionNode will fail
                // -> force to set default in plugin.yml
                return null;
            }
            throw new IllegalStateException("Unknown default description '" + defaultValue + "'");
        }

        /**
         * Removes the starting "permission." node in the path.
         *
         * @param path the path to truncate
         * @return the shortened path
         */
        private static String removePermissionsPrefix(String path) {
            if (path.startsWith("permissions.")) {
                return path.substring("permissions.".length());
            }
            throw new IllegalStateException("Unexpected path '" + path + "': does not begin with 'permissions.'");
        }

        /**
         * Recursively walks through the given memory section to gather all keys.
         * Assumes that the ending value is a boolean and throws an exception otherwise.
         *
         * @param parentSection the memory section to traverse
         * @param children list to add all results to
         */
        private static void collectChildren(MemorySection parentSection, List<String> children) {
            for (Map.Entry<String, Object> entry : parentSection.getValues(false).entrySet()) {
                if (entry.getValue() instanceof MemorySection) {
                    collectChildren((MemorySection) entry.getValue(), children);
                } else if (entry.getValue() instanceof Boolean) {
                    if (!Boolean.TRUE.equals(entry.getValue())) {
                        throw new IllegalStateException("Child entry '" + entry.getKey()
                            + "' has unexpected value '" + entry.getValue() + "'");
                    }
                    children.add(parentSection.getCurrentPath() + "." + entry.getKey());
                } else {
                    throw new IllegalStateException("Found child entry at '" + entry.getKey() + "' with value "
                        + "of unexpected type: '" + parentSection.getCurrentPath() + "." + entry.getValue() + "'");
                }
            }
        }

        /**
         * Removes the given start from all entries in the list.
         *
         * @param start the start to remove
         * @param list the entries to modify
         * @return list with shortened entries
         */
        private static Set<String> removeStart(String start, List<String> list) {
            Set<String> result = new HashSet<>(list.size());
            for (String entry : list) {
                result.add(entry.substring(start.length()));
            }
            return result;
        }
    }

}
