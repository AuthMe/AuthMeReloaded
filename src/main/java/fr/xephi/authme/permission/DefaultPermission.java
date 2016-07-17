package fr.xephi.authme.permission;

import org.bukkit.permissions.ServerOperator;

/**
 * The default permission to fall back to if there is no support for permission nodes.
 */
public enum DefaultPermission {

    /** No one has permission. */
    NOT_ALLOWED("No permission") {
        @Override
        public boolean evaluate(ServerOperator sender) {
            return false;
        }
    },

    /** Only players with OP status have permission. */
    OP_ONLY("OP's only") {
        @Override
        public boolean evaluate(ServerOperator sender) {
            return sender != null && sender.isOp();
        }
    },

    /** Everyone is granted permission. */
    ALLOWED("Everyone allowed") {
        @Override
        public boolean evaluate(ServerOperator sender) {
            return true;
        }
    };

    /** Textual representation of the default permission. */
    private final String title;

    /**
     * Constructor.
     * @param title The textual representation
     */
    DefaultPermission(String title) {
        this.title = title;
    }

    /**
     * Evaluates whether permission is granted to the sender or not.
     *
     * @param sender the sender to process
     * @return true if the sender has permission, false otherwise
     */
    public abstract boolean evaluate(ServerOperator sender);

    /**
     * Return the textual representation.
     *
     * @return the textual representation
     */
    public String getTitle() {
        return title;
    }

}
