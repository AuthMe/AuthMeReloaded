package utils;

/**
 * Option required by a tool task.
 */
public class TaskOption {

    private final String name;
    private final String description;
    private final String defaultOption;
    private final String[] options;

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
