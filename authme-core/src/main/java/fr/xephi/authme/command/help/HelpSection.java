package fr.xephi.authme.command.help;

/**
 * Translatable sections. Message keys are prefixed by {@code section}.
 */
public enum HelpSection {

    COMMAND("command"),

    SHORT_DESCRIPTION("description"),

    DETAILED_DESCRIPTION("detailedDescription"),

    ARGUMENTS("arguments"),

    ALTERNATIVES("alternatives"),

    PERMISSIONS("permissions"),

    CHILDREN("children");

    private static final String PREFIX = "section.";
    private final String key;

    /**
     * Constructor.
     *
     * @param key the message key
     */
    HelpSection(String key) {
        this.key = PREFIX + key;
    }

    /** @return the message key */
    public String getKey() {
        return key;
    }

    /** @return the key without the common prefix */
    public String getEntryKey() {
        // Note ljacqu 20171008: #getKey is called more often than this method, so we optimize for the former method
        return key.substring(PREFIX.length());
    }
}
