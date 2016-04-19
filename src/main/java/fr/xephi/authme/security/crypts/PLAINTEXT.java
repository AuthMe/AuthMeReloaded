package fr.xephi.authme.security.crypts;

@Deprecated
public class PLAINTEXT extends UnsaltedMethod {

    @Override
    public String computeHash(String password) {
        return password;
    }

}
