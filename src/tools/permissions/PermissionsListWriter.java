package permissions;

import utils.ANewMap;
import utils.GeneratedFileWriter;
import utils.TagReplacer;
import utils.ToolsConstants;

import java.util.Map;
import java.util.Scanner;
import java.util.Set;

/**
 * Class responsible for formatting a permissions node list and
 * for writing it to a file if desired.
 */
public class PermissionsListWriter {

    private static final String PERMISSIONS_OUTPUT_FILE = ToolsConstants.DOCS_FOLDER + "permission_nodes.md";

    public static void main(String[] args) {
        // Ask if result should be written to file
        Scanner scanner = new Scanner(System.in);
        System.out.println("Include description? [Enter 'n' for no]");
        boolean includeDescription = !matches("n", scanner);

        if (!includeDescription) {
            outputSimpleList();
            return;
        }

        System.out.println("Write to file? [Enter 'n' for console output]");
        boolean writeToFile = !matches("n", scanner);
        scanner.close();


        if (writeToFile) {
            generateAndWriteFile();
        } else {
            System.out.println(generatePermissionsList());
        }
    }


    private static void generateAndWriteFile() {
        final String permissionsTagValue = generatePermissionsList();

        Map<String, Object> tags = ANewMap.<String, Object>with("permissions", permissionsTagValue).build();
        GeneratedFileWriter.generateFileFromTemplate(
            ToolsConstants.TOOLS_SOURCE_ROOT + "permissions/permission_nodes.tpl.md", PERMISSIONS_OUTPUT_FILE, tags);
        System.out.println("Wrote to '" + PERMISSIONS_OUTPUT_FILE + "'");
        System.out.println("Before committing, please verify the output!");
    }

    private static String generatePermissionsList() {
        PermissionNodesGatherer gatherer = new PermissionNodesGatherer();
        Map<String, String> permissions = gatherer.gatherNodesWithJavaDoc();

        final String template = GeneratedFileWriter.readFromToolsFile("permissions/permission_node_entry.tpl.md");
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

    private static boolean matches(String answer, Scanner sc) {
        String userInput = sc.nextLine();
        return answer.equalsIgnoreCase(userInput);
    }

}
