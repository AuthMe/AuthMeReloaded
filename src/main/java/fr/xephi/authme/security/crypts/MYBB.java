package fr.xephi.authme.security.crypts;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.security.HashUtils;
import fr.xephi.authme.security.RandomString;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static fr.xephi.authme.security.HashUtils.md5;

public class MYBB implements NewEncrMethod {

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
        return RandomString.generateHex(8);
    }

    @Override
    public boolean hasSeparateSalt() {
        return true;
    }
}
