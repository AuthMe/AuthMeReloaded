package fr.xephi.authme.security.crypts;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.security.HashUtils;
import fr.xephi.authme.security.RandomString;
import fr.xephi.authme.security.crypts.description.HasSalt;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.SaltType;
import fr.xephi.authme.security.crypts.description.Usage;
import fr.xephi.authme.settings.Settings;

import java.math.BigInteger;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import static fr.xephi.authme.security.HashUtils.md5;

@Recommendation(Usage.ACCEPTABLE) // presuming that length is something sensible (>= 8)
@HasSalt(value = SaltType.TEXT)   // length defined by Settings.saltLength
public class SALTED2MD5 extends SeparateSaltMethod {

    @Override
    public String computeHash(String password, String salt, String name) {
        return md5(md5(password) + salt);
    }

    @Override
    public String generateSalt() {
        return RandomString.generateHex(Settings.saltLength);
    }

}
