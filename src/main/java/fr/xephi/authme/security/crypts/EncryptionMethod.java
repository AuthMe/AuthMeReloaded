package fr.xephi.authme.security.crypts;

import java.security.NoSuchAlgorithmException;

/**
 * <p>
 * Public interface for Custom Password encryption method
 * </p>
 * <p>
 * The getHash function is called when we need to crypt the password (/register
 * usually)
 * </p>
 * <p>
 * The comparePassword is called when we need to match password (/login usually)
 * </p>
 */
public interface EncryptionMethod {

    /**
     * @param password
     * @param salt
     *            (can be an other data like playerName;salt , playerName,
     *            etc... for customs methods)
     * @return Hashing password
     * @throws NoSuchAlgorithmException
     */
    String getHash(String password, String salt, String name)
            throws NoSuchAlgorithmException;

    /**
     * @param hash
     * @param password
     * @param playerName
     * @return true if password match, false else
     * @throws NoSuchAlgorithmException
     */
    boolean comparePassword(String hash, String password, String playerName)
            throws NoSuchAlgorithmException;

}
