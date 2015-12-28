package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.HashUtils;
import fr.xephi.authme.security.RandomString;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.Usage;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Recommendation(Usage.DO_NOT_USE)
public class PHPFUSION implements EncryptionMethod {

    @Override
    public String computeHash(String password, String salt, String name) {
        String algo = "HmacSHA256";
        String keyString = HashUtils.sha1(salt);
        try {
            SecretKeySpec key = new SecretKeySpec(keyString.getBytes("UTF-8"), algo);
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
            return hash.toString();
        } catch (UnsupportedEncodingException | InvalidKeyException | NoSuchAlgorithmException e) {
            throw new UnsupportedOperationException("Cannot create PHPFUSION hash for " + name, e);
        }
    }

    @Override
    public HashResult computeHash(String password, String name) {
        String salt = generateSalt();
        return new HashResult(computeHash(password, salt, name), salt);
    }

    @Override
    public boolean comparePassword(String hash, String password, String salt, String name) {
        return hash.equals(computeHash(password, salt, name));
    }

    @Override
    public String generateSalt() {
        return RandomString.generateHex(12);
    }

    @Override
    public boolean hasSeparateSalt() {
        return true;
    }

}
