package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.Usage;

import static fr.xephi.authme.security.HashUtils.md5;

@Deprecated
@Recommendation(Usage.DEPRECATED)
public class DoubleMd5 extends UnsaltedMethod {

    @Override
    public String computeHash(String password) {
        return md5(md5(password));
    }

}
