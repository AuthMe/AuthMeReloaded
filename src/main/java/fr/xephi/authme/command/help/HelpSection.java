package fr.xephi.authme.command.help;

/**
 * Translatable sections. Message keys are prefixed by {@code section}.
 */
public enum HelpSection {

    COMMAND("command"),

    SHORT_DESCRIPTION("description.short"),

    DETAILED_DESCRIPTION("description.detailed"),

    ARGUMENTS("arguments"),

    ALTERNATIVES("alternatives"),

    PERMISSIONS("permissions"),

    CHILDREN("children");


    private final String key;

    /**
     * Constructor.
     *
     * @param key the message key
     */
    HelpSection(String key) {
        this.key = "section." + key;
    }

    /** @return the message key */
    public String getKey() {
        return key;
    }
}
