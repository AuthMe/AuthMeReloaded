package fr.xephi.authme.security.crypts.description;

/**
 * The type of salt used by an encryption algorithm.
 */
public enum SaltType {

    /** Random, newly generated text. */
    TEXT,

    /** Salt is based on the username, including variations and repetitions thereof. */
    USERNAME,

    /** No salt. */
    NONE

}
