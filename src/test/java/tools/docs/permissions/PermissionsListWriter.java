package tools.docs.permissions;

import tools.utils.AutoToolTask;
import tools.utils.FileIoUtils;
import tools.utils.TagValue.NestedTagValue;
import tools.utils.TagValueHolder;

import java.util.Map;

import static tools.utils.ToolsConstants.DOCS_FOLDER;
import static tools.utils.ToolsConstants.TOOLS_SOURCE_ROOT;

/**
 * Task responsible for formatting a permissions node list and
 * for writing it to a file if desired.
 */
public class PermissionsListWriter implements AutoToolTask {

    private static final String TEMPLATE_FILE = TOOLS_SOURCE_ROOT + "docs/permissions/permission_nodes.tpl.md";
    private static final String PERMISSIONS_OUTPUT_FILE = DOCS_FOLDER + "permission_nodes.md";

    @Override
    public String getTaskName() {
        return "writePermissionsList";
    }

    @Override
    public void executeDefault() {
        final NestedTagValue permissionsTagValue = generatePermissionsList();

        TagValueHolder tags = TagValueHolder.create().put("nodes", permissionsTagValue);
        FileIoUtils.generateFileFromTemplate(TEMPLATE_FILE, PERMISSIONS_OUTPUT_FILE, tags);
        System.out.println("Wrote to '" + PERMISSIONS_OUTPUT_FILE + "'");
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
}
