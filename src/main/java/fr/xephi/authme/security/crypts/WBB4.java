package fr.xephi.authme.security.crypts;

import java.security.NoSuchAlgorithmException;

/**
 */
public class WBB4 implements EncryptionMethod {

    /**
     * Method getHash.
     *
     * @param password String
     * @param salt     String
     * @param name     String
     * @return String * @throws NoSuchAlgorithmException * @see fr.xephi.authme.security.crypts.EncryptionMethod#getHash(String, String, String)
     */
    @Override
    public String getHash(String password, String salt, String name)
        throws NoSuchAlgorithmException {
        return BCRYPT.getDoubleHash(password, salt);
    }

    /**
     * Method comparePassword.
     *
     * @param hash       String
     * @param password   String
     * @param playerName String
     * @return boolean * @throws NoSuchAlgorithmException * @see fr.xephi.authme.security.crypts.EncryptionMethod#comparePassword(String, String, String)
     */
    @Override
    public boolean comparePassword(String hash, String password,
                                   String playerName) throws NoSuchAlgorithmException {
        return BCRYPT.checkpw(password, hash, 2);
    }

}
