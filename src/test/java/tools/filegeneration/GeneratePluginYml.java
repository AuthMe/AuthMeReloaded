package tools.filegeneration;

import com.google.common.collect.ImmutableMap;
import fr.xephi.authme.command.CommandDescription;
import fr.xephi.authme.command.CommandInitializer;
import fr.xephi.authme.command.CommandUtils;
import fr.xephi.authme.permission.DefaultPermission;
import fr.xephi.authme.permission.PermissionNode;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import tools.docs.permissions.PermissionNodesGatherer;
import tools.utils.AutoToolTask;
import tools.utils.FileIoUtils;
import tools.utils.ToolsConstants;

import java.io.StringReader;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

/**
 * Generates the command and permission section of plugin.yml.
 */
public class GeneratePluginYml implements AutoToolTask {

    private static final String PLUGIN_YML_FILE = ToolsConstants.MAIN_RESOURCES_ROOT + "plugin.yml";

    private static final Map<String, String> WILDCARD_PERMISSIONS = ImmutableMap.of(
        "authme.player.*", "Gives access to all player commands",
        "authme.player.email", "Gives access to all email commands",
        "authme.admin.*", "Gives access to all admin commands",
        "authme.debug", "Gives access to /authme debug and all its sections");

    private List<PermissionNode> permissionNodes;

    private String pluginYmlStart;

    @Override
    public void executeDefault() {
        FileConfiguration configuration = loadPartialPluginYmlFile();

        configuration.set("commands", generateCommands());
        configuration.set("permissions", generatePermissions());

        FileIoUtils.writeToFile(PLUGIN_YML_FILE,
        pluginYmlStart + "\n" + configuration.saveToString());
    }

    @Override
    public String getTaskName() {
        return "generatePluginYml";
    }

    /**
     * Because some parts above the commands section have placeholders that aren't valid YAML, we need
     * to split the contents into an upper part that we ignore and a lower part we load as YAML. When
     * saving we prepend the YAML export with the stripped off part of the file again.
     *
     * @return file configuration with the lower part of the plugin.yml file
     */
    private FileConfiguration loadPartialPluginYmlFile() {
        List<String> pluginYmlLines = FileIoUtils.readLinesFromFile(Paths.get(PLUGIN_YML_FILE));
        int lineNr = 0;
        for (String line : pluginYmlLines) {
            if ("commands:".equals(line)) {
                break;
            }
            ++lineNr;
        }
        if (lineNr == pluginYmlLines.size()) {
            throw new IllegalStateException("Could not find line starting 'commands:' section");
        }
        pluginYmlStart = String.join("\n", pluginYmlLines.subList(0, lineNr));
        String yamlContents = String.join("\n", pluginYmlLines.subList(lineNr, pluginYmlLines.size()));
        return YamlConfiguration.loadConfiguration(new StringReader(yamlContents));
    }

    private static Map<String, Object> generateCommands() {
        Collection<CommandDescription> commands = new CommandInitializer().getCommands();
        Map<String, Object> entries = new LinkedHashMap<>();
        for (CommandDescription command : commands) {
            entries.put(command.getLabels().get(0), buildCommandEntry(command));
        }
        return entries;
    }

    private Map<String, Object> generatePermissions() {
        PermissionNodesGatherer gatherer = new PermissionNodesGatherer();
        Map<String, String> permissionDescriptions = gatherer.gatherNodesWithJavaDoc();

        permissionNodes = gatherer.getPermissionClasses().stream()
            // Note ljacqu 20161023: The compiler fails if we use method references below
            .map(clz -> clz.getEnumConstants())
            .flatMap((PermissionNode[] nodes) -> Arrays.stream(nodes))
            .collect(Collectors.toList());

        Map<String, Object> descriptions = new TreeMap<>();
        for (PermissionNode node : permissionNodes) {
            descriptions.put(node.getNode(), buildPermissionEntry(node, permissionDescriptions.get(node.getNode())));
        }
        addWildcardPermissions(descriptions);
        return descriptions;
    }

    private void addWildcardPermissions(Map<String, Object> permissions) {
        for (Map.Entry<String, String> entry : WILDCARD_PERMISSIONS.entrySet()) {
            permissions.put(entry.getKey(),
                buildWildcardPermissionEntry(entry.getValue(), gatherChildren(entry.getKey())));
        }
    }

    private Map<String, Boolean> gatherChildren(String parentNode) {
        String parentPath = parentNode.replaceAll("\\.\\*$", "");

        Map<String, Boolean> children = new TreeMap<>();
        for (PermissionNode node : permissionNodes) {
            if (node.getNode().startsWith(parentPath)) {
                children.put(node.getNode(), Boolean.TRUE);
            }
        }
        return children;
    }

    private static Map<String, Object> buildCommandEntry(CommandDescription command) {
        if (command.getLabels().size() > 1) {
            return ImmutableMap.of(
                "description", command.getDescription(),
                "usage", buildUsage(command),
                "aliases", command.getLabels().subList(1, command.getLabels().size()));
        } else {
            return ImmutableMap.of(
                "description", command.getDescription(),
                "usage", buildUsage(command));
        }
    }

    private static String buildUsage(CommandDescription command) {
        final String commandStart = "/" + command.getLabels().get(0);
        if (!command.getArguments().isEmpty()) {
            // Command has arguments, so generate something like /authme register <password> <confirmPass>
            final String arguments = command.getArguments().stream()
                .map(CommandUtils::formatArgument)
                .collect(Collectors.joining(" "));
            return commandStart + " " + arguments;
        }
        // Argument-less command, list all children: /authme register|login|firstspawn|spawn|...
        String usage = commandStart + " " + command.getChildren()
            .stream()
            .filter(cmd -> !cmd.getLabels().contains("help"))
            .map(cmd -> cmd.getLabels().get(0))
            .collect(Collectors.joining("|"));
        return usage.trim();
    }

    private static Map<String, Object> buildPermissionEntry(PermissionNode permissionNode, String description) {
        return ImmutableMap.of(
            "description", description,
            "default", convertDefaultPermission(permissionNode.getDefaultPermission()));
    }

    private static Map<String, Object> buildWildcardPermissionEntry(String description, Map<String, Boolean> children) {
        return ImmutableMap.of(
            "description", description,
            "children", children);
    }

    private static Object convertDefaultPermission(DefaultPermission defaultPermission) {
        switch (defaultPermission) {
            // Returning true/false as booleans will make SnakeYAML avoid using quotes
            case ALLOWED: return true;
            case NOT_ALLOWED: return false;
            case OP_ONLY: return "op";
            default:
                throw new IllegalArgumentException("Unknown default permission '" + defaultPermission + "'");
        }
    }
}
