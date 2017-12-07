package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.HashUtils;

/**
 * Hash algorithm to hook into the CMS <a href="http://craftmywebsite.fr/">Craft My Website</a>.
 */
public class CmwCrypt extends UnsaltedMethod {

    @Override
    public String computeHash(String password) {
        return HashUtils.md5(HashUtils.sha1(password));
    }
}
