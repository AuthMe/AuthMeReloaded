package permissions;

import fr.xephi.authme.permission.AdminPermission;
import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.permission.PlayerPermission;
import fr.xephi.authme.permission.PlayerStatePermission;
import utils.FileUtils;
import utils.ToolsConstants;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Gatherer to generate up-to-date lists of the AuthMe permission nodes.
 */
public class PermissionNodesGatherer {

    /**
     * Regular expression that should match the JavaDoc comment above an enum, <i>including</i>
     * the name of the enum value. The first group (i.e. {@code \\1}) should be the JavaDoc description;
     * the second group should contain the enum value.
     */
    private static final Pattern JAVADOC_WITH_ENUM_PATTERN = Pattern.compile(
        "/\\*\\*\\s+\\*"       // Match starting '/**' and the '*' on the next line
        + "(.*?)\\s+\\*/"      // Capture everything until we encounter '*/'
        + "\\s+([A-Z_]+)\\("); // Match the enum name (e.g. 'LOGIN'), until before the first '('

    /**
     * Return a sorted collection of all permission nodes.
     *
     * @return AuthMe permission nodes sorted alphabetically
     */
    public Set<String> gatherNodes() {
        Set<String> nodes = new TreeSet<>();
        for (PermissionNode perm : PlayerPermission.values()) {
            nodes.add(perm.getNode());
        }
        for (PermissionNode perm : AdminPermission.values()) {
            nodes.add(perm.getNode());
        }
        return nodes;
    }

    /**
     * Return a sorted collection of all permission nodes, including its JavaDoc description.
     *
     * @return Ordered map whose keys are the permission nodes and the values the associated JavaDoc
     */
    public Map<String, String> gatherNodesWithJavaDoc() {
        Map<String, String> result = new TreeMap<>();
        result.put("authme.admin.*", "Give access to all admin commands.");
        result.put("authme.player.*", "Permission to use all player (non-admin) commands.");
        // TODO ljacqu 20160109: Add authme.player.email manual description?

        addDescriptionsForClass(PlayerPermission.class, result);
        addDescriptionsForClass(AdminPermission.class, result);
        addDescriptionsForClass(PlayerStatePermission.class, result);
        return result;
    }

    private <T extends Enum<T> & PermissionNode> void addDescriptionsForClass(Class<T> clazz,
                                                                              Map<String, String> descriptions) {
        String classSource = getSourceForClass(clazz);
        Map<String, String> sourceDescriptions = extractJavaDocFromSource(classSource);

        for (T perm : EnumSet.allOf(clazz)) {
            String description = sourceDescriptions.get(perm.name());
            if (description == null) {
                System.out.println("Note: Could not retrieve description for "
                    + clazz.getSimpleName() + "#" + perm.name());
                description = "";
            }
            descriptions.put(perm.getNode(), description.trim());
        }
    }

    private static Map<String, String> extractJavaDocFromSource(String source) {
        Map<String, String> allMatches = new HashMap<>();
        Matcher matcher = JAVADOC_WITH_ENUM_PATTERN.matcher(source);
        while (matcher.find()) {
            String description = matcher.group(1);
            String enumValue = matcher.group(2);
            allMatches.put(enumValue, description);
        }
        return allMatches;
    }

    /**
     * Return the Java source code for the given implementation of {@link PermissionNode}.
     *
     * @param clazz The clazz to the get the source for
     * @param <T> The concrete type
     * @return Source code of the file
     */
    private static <T extends Enum<T> & PermissionNode> String getSourceForClass(Class<T> clazz) {
        String classFile = ToolsConstants.MAIN_SOURCE_ROOT + clazz.getName().replace(".", "/") + ".java";
        return FileUtils.readFromFile(classFile);
    }

}
