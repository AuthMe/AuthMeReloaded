package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.HashUtils;

public class Md5InSha1 extends UnsaltedMethod {

@Override
public String computeHash(String password) {
return HashUtils.md5(HashUtils.sha1(password));
}
}
