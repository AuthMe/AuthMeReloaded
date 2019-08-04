package fr.xephi.authme.data.limbo;

/**
 * Holds states a limbo player can be in.
 */
public enum LimboPlayerState {

    /**
     * Initial state. A limbo player is always in this state before the player has entered the correct password.
     */
    PASSWORD_REQUIRED,

    /**
     * A TOTP code must be provided before the player has completed the login process.
     */
    TOTP_REQUIRED,

    /**
     * Migration of old TWO_FACTOR hash algorithm: player must set a new password.
     */
    NEW_PASSWORD_FOR_TWO_FACTOR_MIGRATION_REQUIRED

}
