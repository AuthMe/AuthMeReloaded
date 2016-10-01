package fr.xephi.authme.command.help;

/**
 * Common, non-generic keys for messages used when showing command help.
 * All keys are prefixed with {@code common}.
 */
public enum HelpMessageKey {

    SHORT_DESCRIPTION("description.short", "Short description"),

    DETAILED_DESCRIPTION("description.detailed", "Detailed description"),

    USAGE("usage", "Usage"),

    ARGUMENTS("arguments", "Arguments"),

    OPTIONAL("optional", "(Optional)"),

    HAS_PERMISSION("hasPermission", "You have permission"),

    NO_PERMISSION("noPermission", "No permission"),

    ALTERNATIVES("alternatives", "Alternatives"),

    DEFAULT("default", "Default"),

    RESULT("result", "Result"),

    PERMISSIONS("permissions", "Permissions"),

    COMMANDS("commands", "Commands");


    private final String key;
    private final String fallback;

    HelpMessageKey(String key, String fallback) {
        this.key = "common." + key;
        this.fallback = fallback;
    }

    public String getKey() {
        return key;
    }

    public String getFallback() {
        return fallback;
    }
}
