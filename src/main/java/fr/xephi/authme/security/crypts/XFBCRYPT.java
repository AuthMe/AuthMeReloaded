package fr.xephi.authme.security.crypts;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XFBCRYPT extends BCRYPT {
    private static final Pattern HASH_PATTERN = Pattern.compile("\"hash\";s.*\"(.*)?\"");

    @Override
    public String generateSalt() {
        return BCRYPT.gensalt();
    }

    public static String getHashFromBlob(byte[] blob) {
        String line = new String(blob);
        Matcher m = HASH_PATTERN.matcher(line);
        if (m.find()) {
            return m.group(1);
        }
        return "*"; // what?
    }
}
