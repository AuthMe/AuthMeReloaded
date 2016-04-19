package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.HashUtils;

public class ROYALAUTH extends UnsaltedMethod {

    @Override
    public String computeHash(String password) {
        for (int i = 0; i < 25; i++) {
            // TODO ljacqu 20151228: HashUtils#sha512 gets a new message digest each time...
            password = HashUtils.sha512(password);
        }
        return password;
    }

}
