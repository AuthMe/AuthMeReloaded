package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.HashUtils;

public class MD5 extends UnsaltedMethod {

    @Override
    public String computeHash(String password) {
        return HashUtils.md5(password);
    }

}
