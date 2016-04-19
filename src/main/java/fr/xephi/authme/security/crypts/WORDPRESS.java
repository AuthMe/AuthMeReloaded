package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.HashUtils;
import fr.xephi.authme.security.MessageDigestAlgorithm;
import fr.xephi.authme.security.crypts.description.HasSalt;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.SaltType;
import fr.xephi.authme.security.crypts.description.Usage;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;

@Recommendation(Usage.ACCEPTABLE)
@HasSalt(value = SaltType.TEXT, length = 9)
// Note ljacqu 20151228: Wordpress is actually a salted algorithm but salt generation is handled internally
// and isn't exposed to the outside, so we treat it as an unsalted implementation
public class WORDPRESS extends UnsaltedMethod {

    private static final String itoa64 = "./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private final SecureRandom randomGen = new SecureRandom();

    private String encode64(byte[] src, int count) {
        int i, value;
        StringBuilder output = new StringBuilder();
        i = 0;

        if (src.length < count) {
            byte[] t = new byte[count];
            System.arraycopy(src, 0, t, 0, src.length);
            Arrays.fill(t, src.length, count - 1, (byte) 0);
            src = t;
        }

        do {
            value = src[i] + (src[i] < 0 ? 256 : 0);
            ++i;
            output.append(itoa64.charAt(value & 63));
            if (i < count) {
                value |= (src[i] + (src[i] < 0 ? 256 : 0)) << 8;
            }
            output.append(itoa64.charAt((value >> 6) & 63));
            if (i++ >= count) {
                break;
            }
            if (i < count) {
                value |= (src[i] + (src[i] < 0 ? 256 : 0)) << 16;
            }
            output.append(itoa64.charAt((value >> 12) & 63));
            if (i++ >= count) {
                break;
            }
            output.append(itoa64.charAt((value >> 18) & 63));
        } while (i < count);
        return output.toString();
    }

    private String crypt(String password, String setting) {
        String output = "*0";
        if (((setting.length() < 2) ? setting : setting.substring(0, 2)).equalsIgnoreCase(output)) {
            output = "*1";
        }
        String id = (setting.length() < 3) ? setting : setting.substring(0, 3);
        if (!(id.equals("$P$") || id.equals("$H$"))) {
            return output;
        }
        int countLog2 = itoa64.indexOf(setting.charAt(3));
        if (countLog2 < 7 || countLog2 > 30) {
            return output;
        }
        int count = 1 << countLog2;
        String salt = setting.substring(4, 4 + 8);
        if (salt.length() != 8) {
            return output;
        }
        MessageDigest md = HashUtils.getDigest(MessageDigestAlgorithm.MD5);
        byte[] pass = stringToUtf8(password);
        byte[] hash = md.digest(stringToUtf8(salt + password));
        do {
            byte[] t = new byte[hash.length + pass.length];
            System.arraycopy(hash, 0, t, 0, hash.length);
            System.arraycopy(pass, 0, t, hash.length, pass.length);
            hash = md.digest(t);
        } while (--count > 0);
        output = setting.substring(0, 12);
        output += encode64(hash, 16);
        return output;
    }

    private String gensaltPrivate(byte[] input) {
        String output = "$P$";
        int iterationCountLog2 = 8;
        output += itoa64.charAt(Math.min(iterationCountLog2 + 5, 30));
        output += encode64(input, 6);
        return output;
    }

    private byte[] stringToUtf8(String string) {
        try {
            return string.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new UnsupportedOperationException("This system doesn't support UTF-8!", e);
        }
    }

    @Override
    public String computeHash(String password) {
        byte random[] = new byte[6];
        randomGen.nextBytes(random);
        return crypt(password, gensaltPrivate(stringToUtf8(new String(random))));
    }

    @Override
    public boolean comparePassword(String password, HashedPassword hashedPassword, String name) {
        String hash = hashedPassword.getHash();
        String comparedHash = crypt(password, hash);
        return comparedHash.equals(hash);
    }

}
