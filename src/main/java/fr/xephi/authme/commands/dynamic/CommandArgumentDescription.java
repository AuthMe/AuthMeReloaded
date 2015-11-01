package fr.xephi.authme.commands.dynamic;

public class CommandArgumentDescription {

    // TODO: Allow argument to consist of infinite parts. <label ...>

    /** Argument label. */
    private String label;
    /** Argument description. */
    private String description;
    /** Defines whether the argument is optional. */
    private boolean optional = false;

    /**
     * Constructor.
     *
     * @param label The argument label.
     * @param description The argument description.
     */
    @SuppressWarnings("UnusedDeclaration")
    public CommandArgumentDescription(String label, String description) {
        this(label, description, false);
    }

    /**
     * Constructor.
     *
     * @param label The argument label.
     * @param description The argument description.
     * @param optional True if the argument is optional, false otherwise.
     */
    public CommandArgumentDescription(String label, String description, boolean optional) {
        setLabel(label);
        setDescription(description);
        setOptional(optional);
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
     * Set the argument label.
     *
     * @param label Argument label.
     */
    public void setLabel(String label) {
        this.label = label;
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
     * Set the argument description.
     *
     * @param description Argument description.
     */
    public void setDescription(String description) {
        this.description = description;
    }

    /**
     * Check whether the argument is optional.
     *
     * @return True if the argument is optional, false otherwise.
     */
    public boolean isOptional() {
        return optional;
    }

    /**
     * Set whether the argument is optional.
     *
     * @param optional True if the argument is optional, false otherwise.
     */
    public void setOptional(boolean optional) {
        this.optional = optional;
    }
}
