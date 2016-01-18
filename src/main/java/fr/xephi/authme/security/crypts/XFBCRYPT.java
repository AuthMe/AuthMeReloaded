package fr.xephi.authme.security.crypts;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.util.StringUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XFBCRYPT implements EncryptionMethod {
    public static final String SCHEME_CLASS = "XenForo_Authentication_Core12";
    private static final Pattern HASH_PATTERN = Pattern.compile("\"hash\";s.*\"(.*)?\"");

    @Override
    public String generateSalt() {
        return BCryptService.gensalt();
    }

    @Override
    public String computeHash(String password, String salt, String name) {
        return BCryptService.hashpw(password, salt);
    }

    @Override
    public HashedPassword computeHash(String password, String name) {
        String salt = generateSalt();
        return new HashedPassword(BCryptService.hashpw(password, salt), null);
    }

    @Override
    public boolean comparePassword(String password, HashedPassword hash, String salt) {
        try {
            return hash.getHash().length() > 3 && BCryptService.checkpw(password, hash.getHash());
        } catch (IllegalArgumentException e) {
            ConsoleLogger.showError("XfBCrypt checkpw() returned " + StringUtils.formatException(e));
        }
        return false;
    }

    @Override
    public boolean hasSeparateSalt() {
        return false;
    }

    public static String getHashFromBlob(byte[] blob) {
        String line = new String(blob);
        Matcher m = HASH_PATTERN.matcher(line);
        if (m.find()) {
            return m.group(1);
        }
        return "*"; // what?
    }

    public static String serializeHash(String hash) {
        return "a:1:{s:4:\"hash\";s:" + hash.length() + ":\""+hash+"\";}";
    }
}
