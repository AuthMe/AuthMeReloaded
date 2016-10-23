package fr.xephi.authme.permission;

import org.bukkit.permissions.ServerOperator;

/**
 * The default permission to fall back to if there is no support for permission nodes.
 */
public enum DefaultPermission {

    /** No one has permission. */
    NOT_ALLOWED {
        @Override
        public boolean evaluate(ServerOperator sender) {
            return false;
        }
    },

    /** Only players with OP status have permission. */
    OP_ONLY {
        @Override
        public boolean evaluate(ServerOperator sender) {
            return sender != null && sender.isOp();
        }
    },

    /** Everyone is granted permission. */
    ALLOWED {
        @Override
        public boolean evaluate(ServerOperator sender) {
            return true;
        }
    };

    /**
     * Evaluates whether permission is granted to the sender or not.
     *
     * @param sender the sender to process
     * @return true if the sender has permission, false otherwise
     */
    public abstract boolean evaluate(ServerOperator sender);

}
