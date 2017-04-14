package fr.xephi.authme.security.crypts;

import de.mkammerer.argon2.Argon2Factory;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.Usage;

@Recommendation(Usage.RECOMMENDED)
public class Argon2 extends UnsaltedMethod {

    private de.mkammerer.argon2.Argon2 argon2;

    public Argon2() {
        argon2 = Argon2Factory.create();
    }

    @Override
    public String computeHash(String password) {
        return argon2.hash(2, 65536, 1, password);
    }

    @Override
    public boolean comparePassword(String password, HashedPassword hashedPassword, String name) {
        return argon2.verify(hashedPassword.getHash(), password);
    }
}
