package fr.xephi.authme.command;

/**
 * Result status for mapping command parts. See {@link FoundCommandResult} for a detailed description of the states.
 */
public enum FoundResultStatus {

    SUCCESS,

    INCORRECT_ARGUMENTS,

    UNKNOWN_LABEL,

    NO_PERMISSION,

    MISSING_BASE_COMMAND

}
