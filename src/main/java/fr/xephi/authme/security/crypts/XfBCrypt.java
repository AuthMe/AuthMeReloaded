package fr.xephi.authme.security.crypts;

import at.favre.lib.crypto.bcrypt.BCrypt;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XfBCrypt extends BCryptBasedHash {

    public static final String SCHEME_CLASS = "XenForo_Authentication_Core12";
    private static final Pattern HASH_PATTERN = Pattern.compile("\"hash\";s.*\"(.*)?\"");

    XfBCrypt() {
        super(new BCryptHasher(BCrypt.Version.VERSION_2A, 10));
    }

    /**
     * Extracts the password hash from the given BLOB.
     *
     * @param blob the blob to process
     * @return the extracted hash
     */
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
