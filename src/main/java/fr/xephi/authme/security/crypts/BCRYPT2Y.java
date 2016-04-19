package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.Usage;

@Recommendation(Usage.RECOMMENDED)
public class BCRYPT2Y extends HexSaltedMethod {

    @Override
    public String computeHash(String password, String salt, String name) {
        if (salt.length() == 22) {
            salt = "$2y$10$" + salt;
        }
        return BCryptService.hashpw(password, salt);
    }

    @Override
    public boolean comparePassword(String password, HashedPassword encrypted, String unusedName) {
        String hash = encrypted.getHash();
        if (hash.length() != 60) {
            return false;
        }
        // The salt is the first 29 characters of the hash

        String salt = hash.substring(0, 29);
        return hash.equals(computeHash(password, salt, null));
    }

    @Override
    public int getSaltLength() {
        return 22;
    }

}
