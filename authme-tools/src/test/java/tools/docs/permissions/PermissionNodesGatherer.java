package tools.docs.permissions;

import fr.xephi.authme.ClassCollector;
import fr.xephi.authme.permission.PermissionNode;
import tools.utils.FileIoUtils;
import tools.utils.ToolsConstants;

import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
        "/\\*\\*(\\s+\\*)?"    // Match starting '/**' and optional whitespace with a '*'
        + "(.*?)\\s+\\*/"      // Capture everything until we encounter '*/'
        + "\\s+([A-Z_]+)\\("); // Match the enum name (e.g. 'LOGIN'), until before the first '('

    /**
     * List of all enum classes that implement the {@link PermissionNode} interface.
     */
    private List<Class<? extends PermissionNode>> permissionClasses;

    /**
     * Return a sorted collection of all permission nodes, including its JavaDoc description.
     *
     * @param <T> permission node enum type
     * @return Ordered map whose keys are the permission nodes and the values the associated JavaDoc
     */
    @SuppressWarnings("unchecked")
    public <T extends Enum<T> & PermissionNode> Map<String, String> gatherNodesWithJavaDoc() {
        Map<String, String> result = new TreeMap<>();
        result.put("authme.admin.*", "Give access to all admin commands.");
        result.put("authme.player.*", "Permission to use all player (non-admin) commands.");
        result.put("authme.player.email", "Grants all email permissions.");

        getPermissionClasses().forEach(clz -> addDescriptionsForClass((Class<T>) clz, result));
        return result;
    }

    /**
     * Return all enum classes implementing the PermissionNode interface.
     *
     * @return all permission node enums
     */
    public List<Class<? extends PermissionNode>> getPermissionClasses() {
        if (permissionClasses == null) {
            ClassCollector classCollector = new ClassCollector(ToolsConstants.MAIN_SOURCE_ROOT, "");
            permissionClasses = classCollector
                .collectClasses(PermissionNode.class)
                .stream()
                .filter(Class::isEnum)
                .collect(Collectors.toList());
        }
        return permissionClasses;
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
            String description = matcher.group(2);
            String enumValue = matcher.group(3);
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
        return FileIoUtils.readFromFile(classFile);
    }

}
