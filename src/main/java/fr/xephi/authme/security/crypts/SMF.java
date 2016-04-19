package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.HashUtils;

public class SMF extends UsernameSaltMethod {

    public HashedPassword computeHash(String password, String name) {
        return new HashedPassword(HashUtils.sha1(name.toLowerCase() + password));
    }

}
