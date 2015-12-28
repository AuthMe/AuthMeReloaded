package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.RandomString;

import static fr.xephi.authme.security.HashUtils.md5;

public class MYBB implements EncryptionMethod {

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
