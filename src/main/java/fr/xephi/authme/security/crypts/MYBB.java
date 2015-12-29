package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.RandomString;

import static fr.xephi.authme.security.HashUtils.md5;

public class MYBB extends SeparateSaltMethod {

    @Override
    public String computeHash(String password, String salt, String name) {
        return md5(md5(salt) + md5(password));
    }

    @Override
    public String generateSalt() {
        return RandomString.generateHex(8);
    }

}
