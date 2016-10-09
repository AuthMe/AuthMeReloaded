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


    private final String key;

    /**
     * Constructor.
     *
     * @param key the message key
     */
    HelpMessage(String key) {
        this.key = "common." + key;
    }

    /** @return the message key */
    public String getKey() {
        return key;
    }
}
