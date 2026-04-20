package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.HashUtils;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.Usage;

@Deprecated
@Recommendation(Usage.DEPRECATED)
public class Sha1 extends UnsaltedMethod {

    @Override
    public String computeHash(String password) {
        return HashUtils.sha1(password);
    }

}
