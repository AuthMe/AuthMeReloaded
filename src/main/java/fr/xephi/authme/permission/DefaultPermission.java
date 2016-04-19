package fr.xephi.authme.permission;

/**
 * The default permission for a command if there is no support for permission nodes.
 */
public enum DefaultPermission {

    /** No one can execute the command. */
    NOT_ALLOWED("No permission"),

    /** Only players with the OP status may execute the command. */
    OP_ONLY("OP's only"),

    /** The command can be executed by anyone. */
    ALLOWED("Everyone allowed");

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
     *  Return the textual representation.
     *  
     *  @return The textual representation
     */
    public String getTitle() {
        return title;
    }

}
