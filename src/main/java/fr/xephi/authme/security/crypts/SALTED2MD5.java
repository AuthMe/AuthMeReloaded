package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.RandomString;
import fr.xephi.authme.security.crypts.description.HasSalt;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.SaltType;
import fr.xephi.authme.security.crypts.description.Usage;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.SecuritySettings;

import javax.inject.Inject;

import static fr.xephi.authme.security.HashUtils.md5;

@Recommendation(Usage.ACCEPTABLE) // presuming that length is something sensible (>= 8)
@HasSalt(value = SaltType.TEXT)   // length defined by the doubleMd5SaltLength setting
public class SALTED2MD5 extends SeparateSaltMethod {

    private final int saltLength;

    @Inject
    public SALTED2MD5(Settings settings) {
        saltLength = settings.getProperty(SecuritySettings.DOUBLE_MD5_SALT_LENGTH);
    }

    @Override
    public String computeHash(String password, String salt, String name) {
        return md5(md5(password) + salt);
    }

    @Override
    public String generateSalt() {
        return RandomString.generateHex(saltLength);
    }


}
