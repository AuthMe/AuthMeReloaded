package fr.xephi.authme.security.crypts;

import java.nio.charset.Charset;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class CRAZYCRYPT1 implements EncryptionMethod {

    protected final Charset charset = Charset.forName("UTF-8");
    private static final char[] CRYPTCHARS = new char[] { '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f' };

    @Override
    public String getHash(String password, String salt, String name)
            throws NoSuchAlgorithmException {
        final String text = "ÜÄaeut//&/=I " + password + "7421€547" + name + "__+IÄIH§%NK " + password;
        try {
            final MessageDigest md = MessageDigest.getInstance("SHA-512");
            md.update(text.getBytes(charset), 0, text.length());
            return byteArrayToHexString(md.digest());
        } catch (final NoSuchAlgorithmException e) {
            return null;
        }
    }

    @Override
    public boolean comparePassword(String hash, String password,
            String playerName) throws NoSuchAlgorithmException {
        return hash.equals(getHash(password, null, playerName));
    }

    public static String byteArrayToHexString(final byte... args) {
        final char[] chars = new char[args.length * 2];
        for (int i = 0; i < args.length; i++) {
            chars[i * 2] = CRYPTCHARS[(args[i] >> 4) & 0xF];
            chars[i * 2 + 1] = CRYPTCHARS[(args[i]) & 0xF];
        }
        return new String(chars);
    }
}
