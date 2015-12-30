package fr.xephi.authme.security.crypts;

import static fr.xephi.authme.security.HashUtils.md5;

public class DOUBLEMD5 extends UnsaltedMethod {

    @Override
    public String computeHash(String password) {
        return md5(md5(password));
    }

}
