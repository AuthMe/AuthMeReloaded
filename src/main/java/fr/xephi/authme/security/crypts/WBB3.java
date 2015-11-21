package fr.xephi.authme.security.crypts;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import fr.xephi.authme.AuthMe;

/**
 */
public class WBB3 implements EncryptionMethod {

    /**
     * Method getHash.
     * @param password String
     * @param salt String
     * @param name String
    
    
    
     * @return String * @throws NoSuchAlgorithmException * @see fr.xephi.authme.security.crypts.EncryptionMethod#getHash(String, String, String) */
    @Override
    public String getHash(String password, String salt, String name)
            throws NoSuchAlgorithmException {
        return getSHA1(salt.concat(getSHA1(salt.concat(getSHA1(password)))));
    }

    /**
     * Method comparePassword.
     * @param hash String
     * @param password String
     * @param playerName String
    
    
    
     * @return boolean * @throws NoSuchAlgorithmException * @see fr.xephi.authme.security.crypts.EncryptionMethod#comparePassword(String, String, String) */
    @Override
    public boolean comparePassword(String hash, String password,
            String playerName) throws NoSuchAlgorithmException {
        String salt = AuthMe.getInstance().database.getAuth(playerName).getSalt();
        return hash.equals(getHash(password, salt, ""));
    }

    /**
     * Method getSHA1.
     * @param message String
    
    
     * @return String * @throws NoSuchAlgorithmException */
    private static String getSHA1(String message)
            throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA1");
        sha1.reset();
        sha1.update(message.getBytes());
        byte[] digest = sha1.digest();
        return String.format("%0" + (digest.length << 1) + "x", new BigInteger(1, digest));
    }
}
