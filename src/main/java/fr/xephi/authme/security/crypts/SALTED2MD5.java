package fr.xephi.authme.security.crypts;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.security.RandomString;
import fr.xephi.authme.security.crypts.description.HasSalt;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.SaltType;
import fr.xephi.authme.security.crypts.description.Usage;
import fr.xephi.authme.settings.Settings;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Recommendation(Usage.ACCEPTABLE) // presuming that length is something sensible (>= 8)
@HasSalt(value = SaltType.TEXT)   // length defined by Settings.saltLength
public class SALTED2MD5 implements NewEncrMethod {

    private static String getMD5(String message)
        throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        md5.reset();
        md5.update(message.getBytes());
        byte[] digest = md5.digest();
        return String.format("%0" + (digest.length << 1) + "x", new BigInteger(1, digest));
    }

    @Override
    public String computeHash(String password, String salt, String name)
        throws NoSuchAlgorithmException {
        return getMD5(getMD5(password) + salt);
    }

    @Override
    public HashResult computeHash(String password, String name) {
        try {
            String salt = generateSalt();
            return new HashResult(computeHash(password, salt, name), salt);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e); // TODO #358: Remove try-catch clause
        }
    }

    @Override
    public boolean comparePassword(String hash, String password,
                                   String playerName) throws NoSuchAlgorithmException {
        String salt = AuthMe.getInstance().database.getAuth(playerName).getSalt();
        return hash.equals(getMD5(getMD5(password) + salt));
    }

    @Override
    public boolean comparePassword(String hash, String password, String salt, String name) {
        try {
            return hash.equals(computeHash(password, salt, name));
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
            // TODO #358: Remove try-catch
        }
    }

    @Override
    public String generateSalt() {
        return RandomString.generateHex(Settings.saltLength);
    }

    @Override
    public boolean hasSeparateSalt() {
        return true;
    }

}
