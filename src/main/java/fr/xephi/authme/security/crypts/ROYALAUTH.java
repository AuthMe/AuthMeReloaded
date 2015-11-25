package fr.xephi.authme.security.crypts;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 */
public class ROYALAUTH implements EncryptionMethod {

    /**
     * Method getHash.
     *
     * @param password String
     * @param salt     String
     * @param name     String
     *
     * @return String * @throws NoSuchAlgorithmException * @see fr.xephi.authme.security.crypts.EncryptionMethod#getHash(String, String, String)
     */
    @Override
    public String getHash(String password, String salt, String name)
        throws NoSuchAlgorithmException {
        for (int i = 0; i < 25; i++)
            password = hash(password, salt);
        return password;
    }

    /**
     * Method hash.
     *
     * @param password String
     * @param salt     String
     *
     * @return String * @throws NoSuchAlgorithmException
     */
    public String hash(String password, String salt)
        throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-512");
        md.update(password.getBytes());
        byte byteData[] = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte aByteData : byteData)
            sb.append(Integer.toString((aByteData & 0xff) + 0x100, 16).substring(1));
        return sb.toString();
    }

    /**
     * Method comparePassword.
     *
     * @param hash       String
     * @param password   String
     * @param playerName String
     *
     * @return boolean * @throws NoSuchAlgorithmException * @see fr.xephi.authme.security.crypts.EncryptionMethod#comparePassword(String, String, String)
     */
    @Override
    public boolean comparePassword(String hash, String password,
                                   String playerName) throws NoSuchAlgorithmException {
        return hash.equalsIgnoreCase(getHash(password, "", ""));
    }

}
