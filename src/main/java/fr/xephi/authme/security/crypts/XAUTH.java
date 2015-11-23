package fr.xephi.authme.security.crypts;

import java.security.NoSuchAlgorithmException;

/**
 */
public class XAUTH implements EncryptionMethod {

    /**
     * Method getWhirlpool.
     *
     * @param message String
     * @return String
     */
    public static String getWhirlpool(String message) {
        WHIRLPOOL w = new WHIRLPOOL();
        byte[] digest = new byte[WHIRLPOOL.DIGESTBYTES];
        w.NESSIEinit();
        w.NESSIEadd(message);
        w.NESSIEfinalize(digest);
        return WHIRLPOOL.display(digest);
    }

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
        String hash = getWhirlpool(salt + password).toLowerCase();
        int saltPos = (password.length() >= hash.length() ? hash.length() - 1 : password.length());
        return hash.substring(0, saltPos) + salt + hash.substring(saltPos);
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
        int saltPos = (password.length() >= hash.length() ? hash.length() - 1 : password.length());
        String salt = hash.substring(saltPos, saltPos + 12);
        return hash.equals(getHash(password, salt, ""));
    }

}
