package fr.xephi.authme.security.crypts;

import fr.xephi.authme.AuthMe;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 */
public class PHPFUSION implements EncryptionMethod {

    private static String getSHA1(String message)
        throws NoSuchAlgorithmException {
        MessageDigest sha1 = MessageDigest.getInstance("SHA1");
        sha1.reset();
        sha1.update(message.getBytes());
        byte[] digest = sha1.digest();
        return String.format("%0" + (digest.length << 1) + "x", new BigInteger(1, digest));
    }

    @Override
    public String getHash(String password, String salt, String name)
        throws NoSuchAlgorithmException {
        String digest = null;
        String algo = "HmacSHA256";
        String keyString = getSHA1(salt);
        try {
            SecretKeySpec key = new SecretKeySpec((keyString).getBytes("UTF-8"), algo);
            Mac mac = Mac.getInstance(algo);
            mac.init(key);
            byte[] bytes = mac.doFinal(password.getBytes("ASCII"));
            StringBuilder hash = new StringBuilder();
            for (byte aByte : bytes) {
                String hex = Integer.toHexString(0xFF & aByte);
                if (hex.length() == 1) {
                    hash.append('0');
                }
                hash.append(hex);
            }
            digest = hash.toString();
        } catch (UnsupportedEncodingException | InvalidKeyException | NoSuchAlgorithmException e) {
            //ingore
        }

        return digest;
    }

    @Override
    public boolean comparePassword(String hash, String password,
                                   String playerName) throws NoSuchAlgorithmException {
        String salt = AuthMe.getInstance().database.getAuth(playerName).getSalt();
        return hash.equals(getHash(password, salt, ""));
    }

}
