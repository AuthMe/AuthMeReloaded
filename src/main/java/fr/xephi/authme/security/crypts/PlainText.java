package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.Usage;

/**
 * Plaintext password storage.
 *
 * @deprecated Using this is no longer supported. AuthMe will migrate to SHA256 on startup.
 */
@Deprecated
@Recommendation(Usage.DEPRECATED)
public class PlainText extends UnsaltedMethod {

    @Override
    public String computeHash(String password) {
        return password;
    }

}
