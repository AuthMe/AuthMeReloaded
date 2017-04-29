package fr.xephi.authme.data.limbo;

/**
 * Represents the group type based on the user's auth status.
 */
enum AuthGroupType {

    /** Player does not have an account. */
    UNREGISTERED,

    /** Player is registered but not logged in. */
    REGISTERED_UNAUTHENTICATED,

    /** Player is logged in. */
    LOGGED_IN

}
