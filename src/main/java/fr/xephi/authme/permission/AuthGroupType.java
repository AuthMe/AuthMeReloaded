package fr.xephi.authme.permission;

/**
 * Represents the group type based on the user's auth status.
 */
public enum AuthGroupType {

    /** Player does not have an account. */
    UNREGISTERED,

    /** Registered? */
    REGISTERED, // TODO #761: Remove this or the NOT_LOGGED_IN one

    /** Player is registered and not logged in. */
    NOT_LOGGED_IN,

    /** Player is logged in. */
    LOGGED_IN

}
