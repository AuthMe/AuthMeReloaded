package fr.xephi.authme.security.crypts;

import java.security.NoSuchAlgorithmException;

/**
 */
public class BCRYPT2Y implements EncryptionMethod {

    /**
     * Method getHash.
     * @param password String
     * @param salt String
     * @param name String
     * @return String
     * @throws NoSuchAlgorithmException
     * @see fr.xephi.authme.security.crypts.EncryptionMethod#getHash(String, String, String)
     */
    @Override
    public String getHash(String password, String salt, String name)
            throws NoSuchAlgorithmException {
        if (salt.length() == 22)
            salt = "$2y$10$" + salt;
        return (BCRYPT.hashpw(password, salt));
    }

    /**
     * Method comparePassword.
     * @param hash String
     * @param password String
     * @param playerName String
     * @return boolean
     * @throws NoSuchAlgorithmException
     * @see fr.xephi.authme.security.crypts.EncryptionMethod#comparePassword(String, String, String)
     */
    @Override
    public boolean comparePassword(String hash, String password,
            String playerName) throws NoSuchAlgorithmException {
        String ok = hash.substring(0, 29);
        if (ok.length() != 29)
            return false;
        return hash.equals(getHash(password, ok, playerName));
    }

}
