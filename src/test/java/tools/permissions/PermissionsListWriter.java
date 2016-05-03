package tools.permissions;

import tools.utils.FileUtils;
import tools.utils.TagValue.NestedTagValue;
import tools.utils.TagValueHolder;
import tools.utils.ToolTask;
import tools.utils.ToolsConstants;

import java.util.Map;
import java.util.Scanner;
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
    public void execute(Scanner scanner) {
        // Ask if result should be written to file
        System.out.println("Include description? [Enter 'n' for no]");
        boolean includeDescription = !matches("n", scanner);

        boolean writeToFile = false;
        if (includeDescription) {
            System.out.println("Write to file? [Enter 'n' for no]");
            writeToFile = !matches("n", scanner);
        }

        if (!includeDescription) {
            outputSimpleList();
        } else if (writeToFile) {
            generateAndWriteFile();
        } else {
            System.out.println(generatePermissionsList());
        }
    }

    private static void generateAndWriteFile() {
        final NestedTagValue permissionsTagValue = generatePermissionsList();

        TagValueHolder tags = TagValueHolder.create().put("nodes", permissionsTagValue);
        FileUtils.generateFileFromTemplate(
            ToolsConstants.TOOLS_SOURCE_ROOT + "permissions/permission_nodes.tpl.md", PERMISSIONS_OUTPUT_FILE, tags);
        System.out.println("Wrote to '" + PERMISSIONS_OUTPUT_FILE + "'");
        System.out.println("Before committing, please verify the output!");
    }

    private static NestedTagValue generatePermissionsList() {
        PermissionNodesGatherer gatherer = new PermissionNodesGatherer();
        Map<String, String> permissions = gatherer.gatherNodesWithJavaDoc();
        NestedTagValue permissionTags = new NestedTagValue();
        for (Map.Entry<String, String> entry : permissions.entrySet()) {
            permissionTags.add(TagValueHolder.create()
                .put("node", entry.getKey())
                .put("description", entry.getValue()));
        }
        return permissionTags;
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

    private static boolean matches(String answer, Scanner sc) {
        String userInput = sc.nextLine();
        return answer.equalsIgnoreCase(userInput);
    }

}
