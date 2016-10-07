package fr.xephi.authme.command.help;

/**
 * Common, non-generic keys for messages used when showing command help.
 * All keys are prefixed with {@code common}.
 */
public enum HelpMessageKey {

    SHORT_DESCRIPTION("description.short"),

    DETAILED_DESCRIPTION("description.detailed"),

    USAGE("usage"),

    ARGUMENTS("arguments"),

    OPTIONAL("optional"),

    HAS_PERMISSION("hasPermission"),

    NO_PERMISSION("noPermission"),

    ALTERNATIVES("alternatives"),

    DEFAULT("default"),

    RESULT("result"),

    PERMISSIONS("permissions"),

    COMMANDS("commands");


    private final String key;

    /**
     * Constructor.
     *
     * @param key the message key
     */
    HelpMessageKey(String key) {
        this.key = "common." + key;
    }

    /** @return the message key */
    public String getKey() {
        return key;
    }
}
