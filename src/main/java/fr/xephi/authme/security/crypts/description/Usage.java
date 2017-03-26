package fr.xephi.authme.security.crypts.description;

/**
 * Usage recommendation that can be provided for a hash algorithm.
 * <p>
 * Use the following rules of thumb:
 * <ul>
 *     <li>Hashes using MD5 may be {@link #ACCEPTABLE} but never {@link #RECOMMENDED}.</li>
 *     <li>Hashes using no salt or one based on the username should be {@link #DO_NOT_USE}.</li>
 * </ul>
 */
public enum Usage {

    /** The hash algorithm appears to be cryptographically secure and is one of the algorithms recommended by AuthMe. */
    RECOMMENDED,

    /** There are safer algorithms that can be chosen but using the algorithm is generally OK. */
    ACCEPTABLE,

    /** Hash algorithm is not recommended to be used. Use only if required by another system. */
    DO_NOT_USE,

    /** Algorithm that is or will be no longer supported actively. */
    DEPRECATED,

    /** The algorithm does not work properly; do not use. */
    DOES_NOT_WORK

}
