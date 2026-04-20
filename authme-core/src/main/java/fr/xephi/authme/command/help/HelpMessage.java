package fr.xephi.authme.command.help;

/**
 * Common, non-generic keys for messages used when showing command help.
 * All keys are prefixed with {@code common}.
 */
public enum HelpMessage {

    HEADER("header"),

    OPTIONAL("optional"),

    HAS_PERMISSION("hasPermission"),

    NO_PERMISSION("noPermission"),

    DEFAULT("default"),

    RESULT("result");

    private static final String PREFIX = "common.";
    private final String key;

    /**
     * Constructor.
     *
     * @param key the message key
     */
    HelpMessage(String key) {
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
