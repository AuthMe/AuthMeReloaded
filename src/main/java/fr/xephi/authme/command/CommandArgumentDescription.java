package fr.xephi.authme.command;

/**
 * Wrapper for the description of a command argument.
 */
public class CommandArgumentDescription {

    /**
     * Argument label (one-word description of the argument).
     */
    private final String label;
    /**
     * Argument description.
     */
    private final String description;
    /**
     * Defines whether the argument is optional.
     */
    private final boolean isOptional;

    /**
     * Constructor.
     *
     * @param label       The argument label.
     * @param description The argument description.
     * @param isOptional  True if the argument is optional, false otherwise.
     */
    public CommandArgumentDescription(String label, String description, boolean isOptional) {
        this.label = label;
        this.description = description;
        this.isOptional = isOptional;
    }

    /**
     * Get the argument label.
     *
     * @return Argument label.
     */
    public String getLabel() {
        return this.label;
    }

    /**
     * Get the argument description.
     *
     * @return Argument description.
     */
    public String getDescription() {
        return description;
    }

    /**
     * Return whether the argument is optional.
     *
     * @return True if the argument is optional, false otherwise.
     */
    public boolean isOptional() {
        return isOptional;
    }

}
