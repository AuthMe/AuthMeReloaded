package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.HashUtils;
import fr.xephi.authme.security.MessageDigestAlgorithm;

import java.security.MessageDigest;

public class RoyalAuth extends UnsaltedMethod {

    @Override
    public String computeHash(String password) {
        MessageDigest algorithm = HashUtils.getDigest(MessageDigestAlgorithm.SHA512);
        for (int i = 0; i < 25; ++i) {
            password = HashUtils.hash(password, algorithm);
        }
        return password;
    }

}
