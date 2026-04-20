package fr.xephi.authme.command;

/**
 * Wrapper for the description of a command argument.
 */
public class CommandArgumentDescription {

    /**
     * Argument name (one-word description of the argument).
     */
    private final String name;
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
     * @param name        The argument name.
     * @param description The argument description.
     * @param isOptional  True if the argument is optional, false otherwise.
     */
    public CommandArgumentDescription(String name, String description, boolean isOptional) {
        this.name = name;
        this.description = description;
        this.isOptional = isOptional;
    }

    /**
     * Get the argument name.
     *
     * @return Argument name.
     */
    public String getName() {
        return this.name;
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
