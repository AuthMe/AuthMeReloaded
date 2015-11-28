package fr.xephi.authme.command;

/**
 */
public class CommandArgumentDescription {

    // TODO: Allow argument to consist of infinite parts. <label ...>

    /**
     * Argument label (one-word description of the argument).
     */
    private String label;
    /**
     * Argument description.
     */
    private String description;
    /**
     * Defines whether the argument is isOptional.
     */
    private boolean isOptional = false;

    /**
     * Constructor.
     *
     * @param label       The argument label.
     * @param description The argument description.
     * @param isOptional  True if the argument is isOptional, false otherwise.
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
     * Check whether the argument is optional.
     *
     * @return True if the argument is optional, false otherwise.
     */
    public boolean isOptional() {
        return isOptional;
    }

}
