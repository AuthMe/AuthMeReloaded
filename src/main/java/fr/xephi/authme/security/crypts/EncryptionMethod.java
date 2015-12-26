package fr.xephi.authme.security.crypts;

import java.security.NoSuchAlgorithmException;

/**
 * Public interface for custom password encryption methods.
 */
public interface EncryptionMethod {

    /**
     * Hash the given password with the given salt for the given player.
     *
     * @param password The clear-text password to hash
     * @param salt     The salt to add to the hash
     * @param name     The player's name (sometimes required for storing the salt separately in the database)
     *
     * @return The hashed password
     */
    String computeHash(String password, String salt, String name)
        throws NoSuchAlgorithmException;

    /**
     * Check whether a given hash matches the clear-text password.
     *
     * @param hash       The hash to verify
     * @param password   The clear-text password to verify the hash against
     * @param playerName The player name to do the check for (sometimes required for retrieving
     *                   the salt from the database)
     *
     * @return True if the password matches, false otherwise
     */
    boolean comparePassword(String hash, String password, String playerName)
        throws NoSuchAlgorithmException;

    // String generateSalt();

    // String computeHash(String password, String name);

}
