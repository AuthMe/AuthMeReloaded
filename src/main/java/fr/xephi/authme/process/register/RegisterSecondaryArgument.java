package fr.xephi.authme.process.register;

/**
 * Type of the second argument of the {@code /register} command.
 */
public enum RegisterSecondaryArgument {

    /** No second argument. */
    NONE,

    /** Confirmation of the first argument. */
    CONFIRMATION,

    /** For password registration, mandatory secondary argument is email. */
    EMAIL_MANDATORY,

    /** For password registration, optional secondary argument is email. */
    EMAIL_OPTIONAL

}
