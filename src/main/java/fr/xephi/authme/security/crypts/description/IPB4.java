package fr.xephi.authme.security.crypts.description;

import fr.xephi.authme.security.crypts.EncryptionMethod;
import fr.xephi.authme.security.crypts.HashedPassword;


@Recommendation(Usage.DOES_NOT_WORK)
@HasSalt(value = SaltType.TEXT)
public class IPB4 implements EncryptionMethod {

    @Override
    public String computeHash(String password, String salt, String name) {
        return null;
    }

    @Override
    public HashedPassword computeHash(String password, String name) {
        return null;
    }

    @Override
    public boolean comparePassword(String password, HashedPassword hash, String name) {
        return false;
    }

    @Override
    public String generateSalt() {
        return null;
    }

    @Override
    public boolean hasSeparateSalt() {
        return false;
    }
}
