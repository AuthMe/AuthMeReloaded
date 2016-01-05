package fr.xephi.authme.settings.propertymap;

import java.util.ArrayList;
import java.util.List;

/**
 * Node class for building a tree from supplied String paths, ordered by insertion.
 * <p>
 * For instance, consider a tree to which the following paths are inserted (in the given order):
 * "animal.bird.duck", "color.yellow", "animal.rodent.rat", "animal.rodent.rabbit", "color.red".
 * For such a tree:<ul>
 *  <li>"animal" (or any of its children) is sorted before "color" (or any of its children)</li>
 *  <li>"animal.bird" or any child thereof is sorted before "animal.rodent"</li>
 *  <li>"animal.rodent.rat" comes before "animal.rodent.rabbit"</li>
 * </ul>
 *
 * @see PropertyMapComparator
 */
final class Node {

    private final String name;
    private final List<Node> children;

    private Node(String name) {
        this.name = name;
        this.children = new ArrayList<>();
    }

    /**
     * Create a root node, i.e. the starting node for a new tree. Call this method to create
     * a new tree and always pass this root to other methods.
     *
     * @return The generated root node.
     */
    public static Node createRoot() {
        return new Node(null);
    }

    /**
     * Add a node to the root, creating any intermediary children that don't exist.
     *
     * @param root The root to add the path to
     * @param fullPath The entire path of the node to add, separate by periods
     */
    public static void addNode(Node root, String fullPath) {
        String[] pathParts = fullPath.split("\\.");
        Node parent = root;
        for (String part : pathParts) {
            Node child = parent.getChild(part);
            if (child == null) {
                child = new Node(part);
                parent.children.add(child);
            }
            parent = child;
        }
    }

    /**
     * Compare two nodes by this class' sorting behavior (insertion order).
     * Note that this method assumes that both supplied paths exist in the tree.
     *
     * @param root The root of the tree
     * @param fullPath1 The full path to the first node
     * @param fullPath2 The full path to the second node
     * @return The comparison result, in the same format as {@link Comparable#compareTo}
     */
    public static int compare(Node root, String fullPath1, String fullPath2) {
        String[] path1 = fullPath1.split("\\.");
        String[] path2 = fullPath2.split("\\.");

        int commonCount = 0;
        Node commonNode = root;
        while (commonCount < path1.length && commonCount < path2.length
               && path1[commonCount].equals(path2[commonCount]) && commonNode != null) {
            commonNode = commonNode.getChild(path1[commonCount]);
            ++commonCount;
        }

        if (commonNode == null) {
            System.err.println("Could not find common node for '" + fullPath1 + "' at index " + commonCount);
            return fullPath1.compareTo(fullPath2); // fallback
        } else if (commonCount >= path1.length || commonCount >= path2.length) {
            return Integer.compare(path1.length, path2.length);
        }
        int child1Index = commonNode.getChildIndex(path1[commonCount]);
        int child2Index = commonNode.getChildIndex(path2[commonCount]);
        return Integer.compare(child1Index, child2Index);
    }

    private Node getChild(String name) {
        for (Node child : children) {
            if (child.name.equals(name)) {
                return child;
            }
        }
        return null;
    }

    /**
     * Return the child's index, i.e. the position at which it was inserted to its parent.
     *
     * @param name The name of the node
     * @return The insertion index
     */
    private int getChildIndex(String name) {
        int i = 0;
        for (Node child : children) {
            if (child.name.equals(name)) {
                return i;
            }
            ++i;
        }
        return -1;
    }

    @Override
    public String toString() {
        return "Node '" + name + "'";
    }

}
