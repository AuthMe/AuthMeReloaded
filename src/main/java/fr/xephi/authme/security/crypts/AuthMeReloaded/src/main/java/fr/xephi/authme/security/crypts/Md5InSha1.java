package fr.xephi.authme.security.crypts;

import static fr.xephi.authme.security.HashUtils.*;

public class Md5InSha1 extends UnsaltedMethod {

@Override
public String computeHash(String password) {
return md5(sha1(password));
}
}
