package fr.xephi.authme.permission;

import org.bukkit.command.CommandSender;

/**
 * The default permission to fall back to if there is no support for permission nodes.
 */
public enum DefaultPermission {

    /** No one has permission. */
    NOT_ALLOWED("No permission") {
        @Override
        public boolean evaluate(CommandSender sender) {
            return false;
        }

        @Override
        public boolean evaluateOffline(String name) {
            return false;
        }
    },

    /** Only players with OP status have permission. */
    OP_ONLY("OP's only") {
        @Override
        public boolean evaluate(CommandSender sender) {
            return sender.isOp();
        }

        @Override
        public boolean evaluateOffline(String name) {
            // TODO #784: Check if there is an elegant way to evaluate OP status
            return false;
        }
    },

    /** Everyone is granted permission. */
    ALLOWED("Everyone allowed") {
        @Override
        public boolean evaluate(CommandSender sender) {
            return true;
        }

        @Override
        public boolean evaluateOffline(String name) {
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
    public abstract boolean evaluate(CommandSender sender);

    /**
     * Evaluate whether permission is granted to an offline user.
     *
     * @param name The name to check
     * @return True if the user has permission, false otherwise
     */
    public abstract boolean evaluateOffline(String name);

    /**
     * Return the textual representation.
     *
     * @return the textual representation
     */
    public String getTitle() {
        return title;
    }

}
