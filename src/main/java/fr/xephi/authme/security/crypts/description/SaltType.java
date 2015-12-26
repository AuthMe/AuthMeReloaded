package fr.xephi.authme.security.crypts.description;

/**
 * The type of salt used by an encryption algorithms.
 */
public enum SaltType {

    /** Random, newly generated text. */
    TEXT,

    /** The username, including variations or repetitions. */
    USERNAME,

    /** No salt. */
    NONE

}
