package utils;

/**
 * Option required by a tool task.
 */
public class TaskOption {

    private final String name;
    private final String description;
    private final String defaultOption;
    private final String[] options;

    /**
     * Constructor.
     *
     * @param name Name of the option (to refer to the option)
     * @param description Description shown to the user when asked to set the option
     * @param defaultOption The default option. Can be null to force a value from options.
     * @param options Collection of possible options. Can be null to allow any input.
     */
    public TaskOption(String name, String description, String defaultOption, String... options) {
        this.name = name;
        this.description = description;
        this.defaultOption = defaultOption;
        this.options = options;
    }

    public String getName() {
        return name;
    }

    public String getDescription() {
        return description;
    }

    public String getDefaultOption() {
        return defaultOption;
    }

    public String[] getOptions() {
        return options;
    }
}
