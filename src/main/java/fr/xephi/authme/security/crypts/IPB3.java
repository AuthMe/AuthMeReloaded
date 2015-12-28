package fr.xephi.authme.security.crypts;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.security.HashUtils;
import fr.xephi.authme.security.RandomString;
import fr.xephi.authme.security.crypts.description.HasSalt;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.SaltType;
import fr.xephi.authme.security.crypts.description.Usage;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static fr.xephi.authme.security.HashUtils.md5;

@Recommendation(Usage.DO_NOT_USE)
@HasSalt(value = SaltType.TEXT, length = 5)
public class IPB3 implements NewEncrMethod {

    @Override
    public String computeHash(String password, String salt, String name) {
        return md5(md5(salt) + md5(password));
    }

    @Override
    public HashResult computeHash(String password, String name) {
        String salt = generateSalt();
        return new HashResult(computeHash(password, salt, name), salt);
    }

    @Override
    public boolean comparePassword(String hash, String password, String playerName) {
        String salt = AuthMe.getInstance().database.getAuth(playerName).getSalt();
        return hash.equals(computeHash(password, salt, playerName));
    }

    @Override
    public boolean comparePassword(String hash, String password, String salt, String name) {
        return hash.equals(computeHash(password, salt, name));
    }

    @Override
    public String generateSalt() {
        return RandomString.generateHex(5);
    }

    @Override
    public boolean hasSeparateSalt() {
        return true;
    }
}
