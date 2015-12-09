package fr.xephi.authme.permission;

/**
 * The default permission for a command if there is no support for permission nodes.
 */
public enum DefaultPermission {

    /** No one can execute the command. */
    NOT_ALLOWED,

    /** Only players with the OP status may execute the command. */
    OP_ONLY,

    /** The command can be executed by anyone. */
    ALLOWED
}
