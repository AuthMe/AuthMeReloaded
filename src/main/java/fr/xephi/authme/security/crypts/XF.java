package fr.xephi.authme.security.crypts;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class XF implements EncryptionMethod {

    @Override
    public String computeHash(String password, String salt, String name) {
        return getSha256(getSha256(password) + regmatch("\"salt\";.:..:\"(.*)\";.:.:\"hashFunc\"", salt));
    }

    @Override
    public HashedPassword computeHash(String password, String name) {
        String salt = generateSalt();
        return new HashedPassword(computeHash(password, salt, null), salt);
    }

    @Override
    public boolean comparePassword(String password, HashedPassword hashedPassword, String name) {
        // TODO #137: Write the comparePassword method. See commit 121d323 for what was here previously; it was
        // utter non-sense
        return false;
    }

    // TODO #137: If this method corresponds to HashUtils.sha256(), use it instead of this
    private String getSha256(String password) {
        MessageDigest md = null;
        try {
            md = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        md.update(password.getBytes());
        byte byteData[] = md.digest();
        StringBuilder sb = new StringBuilder();
        for (byte element : byteData) {
            sb.append(Integer.toString((element & 0xff) + 0x100, 16).substring(1));
        }
        StringBuilder hexString = new StringBuilder();
        for (byte element : byteData) {
            String hex = Integer.toHexString(0xff & element);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    @Override
    public String generateSalt() {
        // TODO #137: Find out what kind of salt format XF uses to generate new passwords
        return "";
    }

    @Override
    public boolean hasSeparateSalt() {
        return true;
    }

    private String regmatch(String pattern, String line) {
        List<String> allMatches = new ArrayList<>();
        Matcher m = Pattern.compile(pattern).matcher(line);
        while (m.find()) {
            allMatches.add(m.group(1));
        }
        return allMatches.get(0);
    }
}
