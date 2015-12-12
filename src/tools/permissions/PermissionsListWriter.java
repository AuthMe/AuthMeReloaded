package permissions;

import utils.ANewMap;
import utils.FileUtils;
import utils.TagReplacer;
import utils.TaskOption;
import utils.ToolTask;
import utils.ToolsConstants;

import java.util.Arrays;
import java.util.Map;
import java.util.Set;

/**
 * Task responsible for formatting a permissions node list and
 * for writing it to a file if desired.
 */
public class PermissionsListWriter implements ToolTask {

    private static final String PERMISSIONS_OUTPUT_FILE = ToolsConstants.DOCS_FOLDER + "permission_nodes.md";

    @Override
    public String getTaskName() {
        return "writePermissionsList";
    }

    @Override
    public void execute(Map<String, String> options) {
        // Ask if result should be written to file
        boolean includeDescription = options.get("include.description").equals("y");
        boolean writeToFile = options.get("write.to.file").equals("y");

        if (!includeDescription) {
            outputSimpleList();
        } else if (writeToFile) {
            generateAndWriteFile();
        } else {
            System.out.println(generatePermissionsList());
        }
    }

    @Override
    public Iterable<TaskOption> getOptions() {
        return Arrays.asList(
            new TaskOption("include.description", "Include description? [y/n]", "y", "y", "n"),
            new TaskOption("write.to.file", "Write to file? [y/n]", "n", "y", "n"));
    }

    private static void generateAndWriteFile() {
        final String permissionsTagValue = generatePermissionsList();

        Map<String, Object> tags = ANewMap.<String, Object>with("permissions", permissionsTagValue).build();
        FileUtils.generateFileFromTemplate(
            ToolsConstants.TOOLS_SOURCE_ROOT + "permissions/permission_nodes.tpl.md", PERMISSIONS_OUTPUT_FILE, tags);
        System.out.println("Wrote to '" + PERMISSIONS_OUTPUT_FILE + "'");
        System.out.println("Before committing, please verify the output!");
    }

    private static String generatePermissionsList() {
        PermissionNodesGatherer gatherer = new PermissionNodesGatherer();
        Map<String, String> permissions = gatherer.gatherNodesWithJavaDoc();

        final String template = FileUtils.readFromToolsFile("permissions/permission_node_entry.tpl.md");
        StringBuilder sb = new StringBuilder();

        for (Map.Entry<String, String> entry : permissions.entrySet()) {
            Map<String, Object> tags = ANewMap.<String, Object>
                with("node", entry.getKey())
                .and("description", entry.getValue())
                .build();
            sb.append(TagReplacer.applyReplacements(template, tags));
        }
        return sb.toString();
    }

    private static void outputSimpleList() {
        PermissionNodesGatherer gatherer = new PermissionNodesGatherer();
        Set<String> nodes = gatherer.gatherNodes();
        for (String node : nodes) {
            System.out.println(node);
        }
        System.out.println();
        System.out.println("Total: " + nodes.size());
    }

}
