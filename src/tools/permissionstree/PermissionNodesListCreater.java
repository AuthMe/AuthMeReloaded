package permissionstree;

import fr.xephi.authme.permission.AdminPermission;
import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.permission.PlayerPermission;

import java.util.Set;
import java.util.TreeSet;

/**
 * Generate all permission nodes like a tree.
 */
public class PermissionNodesListCreater {

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

}
