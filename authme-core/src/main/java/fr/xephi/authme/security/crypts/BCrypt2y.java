package fr.xephi.authme.security.crypts;

import javax.inject.Inject;

/**
 * Hash for BCrypt in the $2y$ variant. Uses a fixed cost factor of 10.
 */
public class BCrypt2y extends BCryptBasedHash {

    @Inject
    public BCrypt2y() {
        this(10);
    }

    public BCrypt2y(int cost) {
        super(new BCryptHasher("2y", cost));
    }
}
