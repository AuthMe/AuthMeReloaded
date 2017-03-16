package fr.xephi.authme.security.crypts;

/**
 * Plaintext password storage.
 *
 * @deprecated Using this is no longer supported. AuthMe will migrate to SHA256 on startup.
 */
@Deprecated
public class PlainText extends UnsaltedMethod {

    @Override
    public String computeHash(String password) {
        return password;
    }

}
