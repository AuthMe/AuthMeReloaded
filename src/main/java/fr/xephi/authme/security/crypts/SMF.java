package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.HashUtils;

public class SMF extends UsernameSaltMethod {

    public HashResult computeHash(String password, String name) {
        return new HashResult(HashUtils.sha1(name.toLowerCase() + password));
    }

}
