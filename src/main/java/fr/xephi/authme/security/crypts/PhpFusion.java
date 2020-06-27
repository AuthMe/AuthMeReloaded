package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.HashUtils;
import fr.xephi.authme.security.crypts.description.AsciiRestricted;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.Usage;
import fr.xephi.authme.util.RandomStringUtils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

@Recommendation(Usage.DO_NOT_USE)
@AsciiRestricted
public class PhpFusion extends SeparateSaltMethod {

    @Override
    public String computeHash(String password, String salt, String name) {
        String algo = "HmacSHA256";
        String keyString = HashUtils.sha1(salt);
        try {
            SecretKeySpec key = new SecretKeySpec(keyString.getBytes(StandardCharsets.UTF_8), algo);
            Mac mac = Mac.getInstance(algo);
            mac.init(key);
            byte[] bytes = mac.doFinal(password.getBytes(StandardCharsets.US_ASCII));
            StringBuilder hash = new StringBuilder();
            for (byte aByte : bytes) {
                String hex = Integer.toHexString(0xFF & aByte);
                if (hex.length() == 1) {
                    hash.append('0');
                }
                hash.append(hex);
            }
            return hash.toString();
        } catch (InvalidKeyException | NoSuchAlgorithmException e) {
            throw new UnsupportedOperationException("Cannot create PHPFUSION hash for " + name, e);
        }
    }

    @Override
    public String generateSalt() {
        return RandomStringUtils.generateHex(12);
    }


}
