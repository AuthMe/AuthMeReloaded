package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.HashUtils;

public class SHA512 extends UnsaltedMethod {

    @Override
    public String computeHash(String password) {
        return HashUtils.sha512(password);
    }

}
