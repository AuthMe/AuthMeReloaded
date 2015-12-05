package permissionstree;

import fr.xephi.authme.util.StringUtils;
import utils.CommentType;
import utils.GeneratedFileWriter;

import java.util.Map;
import java.util.Scanner;
import java.util.Set;

/**
 * Class responsible for formatting a permissions node list and
 * for writing it to a file if desired.
 */
public class PermissionsListWriter {

    private static final String PERMISSIONS_TREE_FILE = "gen_permtree.txt";

    public static void main(String[] args) {
        // Ask if result should be written to file
        Scanner scanner = new Scanner(System.in);
        System.out.println("Include description? [Enter 'n' for no]");
        boolean includeDescription = !matches("n", scanner);

        System.out.println("Write to file? [Enter 'y' for yes]");
        boolean writeToFile = matches("y", scanner);
        scanner.close();

        // Generate connections and output or write
        String output = generatedOutput(includeDescription);

        if (writeToFile) {
            GeneratedFileWriter.createGeneratedFile(PERMISSIONS_TREE_FILE, output, CommentType.YML);
        } else {
            System.out.println(output);
        }
    }

    private static String generatedOutput(boolean includeDescription) {
        PermissionNodesGatherer creater = new PermissionNodesGatherer();
        if (!includeDescription) {
            Set<String> nodes = creater.gatherNodes();
            return StringUtils.join("\n", nodes);
        }

        Map<String, String> permissions = creater.gatherNodesWithJavaDoc();
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, String> entry : permissions.entrySet()) {
            sb.append(entry.getKey())
                .append(": ")
                .append(entry.getValue())
                .append("\n");
        }
        return sb.toString();
    }

    private static boolean matches(String answer, Scanner sc) {
        String userInput = sc.nextLine();
        return answer.equalsIgnoreCase(userInput);
    }

}
