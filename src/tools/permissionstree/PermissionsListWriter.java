package permissionstree;

import fr.xephi.authme.util.StringUtils;
import utils.CommentType;
import utils.GeneratedFileWriter;

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
        System.out.println("Write to file? [y = yes]");
        String answer = scanner.next();
        boolean writeToFile = "y".equalsIgnoreCase(answer);

        // Generate connections and output or write
        PermissionNodesListCreater creater = new PermissionNodesListCreater();
        Set<String> nodes = creater.gatherNodes();
        String output = StringUtils.join("\n", nodes);

        if (writeToFile) {
            GeneratedFileWriter.createGeneratedFile(PERMISSIONS_TREE_FILE, output, CommentType.YML);
        } else {
            System.out.println(output);
        }
    }

}
