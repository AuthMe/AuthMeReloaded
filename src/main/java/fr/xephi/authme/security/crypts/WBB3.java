package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.RandomString;

import static fr.xephi.authme.security.HashUtils.sha1;

public class WBB3 extends SeparateSaltMethod {

    @Override
    public String computeHash(String password, String salt, String name) {
        return sha1(salt.concat(sha1(salt.concat(sha1(password)))));
    }

    @Override
    public String generateSalt() {
        return RandomString.generateHex(40);
    }

}
