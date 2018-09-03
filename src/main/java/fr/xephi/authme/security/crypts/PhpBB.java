package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.HashUtils;
import fr.xephi.authme.security.MessageDigestAlgorithm;
import fr.xephi.authme.security.crypts.description.HasSalt;
import fr.xephi.authme.security.crypts.description.Recommendation;
import fr.xephi.authme.security.crypts.description.SaltType;
import fr.xephi.authme.security.crypts.description.Usage;

import java.io.UnsupportedEncodingException;
import java.security.MessageDigest;

import static fr.xephi.authme.security.HashUtils.isEqual;
import static fr.xephi.authme.security.crypts.BCryptHasher.SALT_LENGTH_ENCODED;

/**
 * Encryption method compatible with phpBB3.
 * <p>
 * As tested with phpBB 3.2.1, by default new passwords are encrypted with BCrypt $2y$.
 * For backwards compatibility, phpBB3 supports other hashes for comparison. This implementation
 * successfully checks against phpBB's salted MD5 hashing algorithm (adaptation of phpass),
 * as well as plain MD5.
 */
@Recommendation(Usage.ACCEPTABLE)
@HasSalt(value = SaltType.TEXT, length = SALT_LENGTH_ENCODED)
public class PhpBB implements EncryptionMethod {

    private final BCrypt2y bCrypt2y = new BCrypt2y();

    @Override
    public HashedPassword computeHash(String password, String name) {
        return bCrypt2y.computeHash(password, name);
    }

    @Override
    public String computeHash(String password, String salt, String name) {
        return bCrypt2y.computeHash(password, salt, name);
    }

    @Override
    public boolean comparePassword(String password, HashedPassword hashedPassword, String name) {
        final String hash = hashedPassword.getHash();
        if (HashUtils.isValidBcryptHash(hash)) {
            return bCrypt2y.comparePassword(password, hashedPassword, name);
        } else if (hash.length() == 34) {
            return PhpassSaltedMd5.phpbb_check_hash(password, hash);
        } else {
            return isEqual(hash, PhpassSaltedMd5.md5(password));
        }
    }

    @Override
    public String generateSalt() {
        // Salt length 22, as seen in https://github.com/phpbb/phpbb/blob/master/phpBB/phpbb/passwords/driver/bcrypt.php
        // Ours generates 16 chars because the salt must not yet be encoded.
        return BCryptHasher.generateSalt();
    }

    @Override
    public boolean hasSeparateSalt() {
        return false;
    }

    /**
     * Java implementation of the salted MD5 as used in phpBB (adapted from phpass).
     *
     * @see <a href="https://github.com/phpbb/phpbb/blob/master/phpBB/phpbb/passwords/driver/salted_md5.php">phpBB's salted_md5.php</a>
     * @see <a href="http://www.openwall.com/phpass/">phpass</a>
     */
    private static final class PhpassSaltedMd5 {

        private static final String itoa64 = "./0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";

        private static String md5(String data) {
            try {
                byte[] bytes = data.getBytes("ISO-8859-1");
                MessageDigest md5er = HashUtils.getDigest(MessageDigestAlgorithm.MD5);
                byte[] hash = md5er.digest(bytes);
                return bytes2hex(hash);
            } catch (UnsupportedEncodingException e) {
                throw new UnsupportedOperationException(e);
            }
        }

        private static int hexToInt(char ch) {
            if (ch >= '0' && ch <= '9')
                return ch - '0';
            ch = Character.toUpperCase(ch);
            if (ch >= 'A' && ch <= 'F')
                return ch - 'A' + 0xA;
            throw new IllegalArgumentException("Not a hex character: " + ch);
        }

        private static String bytes2hex(byte[] bytes) {
            StringBuilder r = new StringBuilder(32);
            for (byte b : bytes) {
                String x = Integer.toHexString(b & 0xff);
                if (x.length() < 2)
                    r.append('0');
                r.append(x);
            }
            return r.toString();
        }

        private static String pack(String hex) {
            StringBuilder buf = new StringBuilder();
            for (int i = 0; i < hex.length(); i += 2) {
                char c1 = hex.charAt(i);
                char c2 = hex.charAt(i + 1);
                char packed = (char) (hexToInt(c1) * 16 + hexToInt(c2));
                buf.append(packed);
            }
            return buf.toString();
        }

        private static String _hash_encode64(String input, int count) {
            StringBuilder output = new StringBuilder();
            int i = 0;
            do {
                int value = input.charAt(i++);
                output.append(itoa64.charAt(value & 0x3f));
                if (i < count)
                    value |= input.charAt(i) << 8;
                output.append(itoa64.charAt((value >> 6) & 0x3f));
                if (i++ >= count)
                    break;
                if (i < count)
                    value |= input.charAt(i) << 16;
                output.append(itoa64.charAt((value >> 12) & 0x3f));
                if (i++ >= count)
                    break;
                output.append(itoa64.charAt((value >> 18) & 0x3f));
            } while (i < count);
            return output.toString();
        }

        private static String _hash_crypt_private(String password, String setting) {
            String output = "*";
            if (!setting.substring(0, 3).equals("$H$"))
                return output;
            int count_log2 = itoa64.indexOf(setting.charAt(3));
            if (count_log2 < 7 || count_log2 > 30)
                return output;
            int count = 1 << count_log2;
            String salt = setting.substring(4, 12);
            if (salt.length() != 8)
                return output;
            String m1 = md5(salt + password);
            String hash = pack(m1);
            do {
                hash = pack(md5(hash + password));
            } while (--count > 0);
            output = setting.substring(0, 12);
            output += _hash_encode64(hash, 16);
            return output;
        }

        private static boolean phpbb_check_hash(String password, String hash) {
            return isEqual(hash, _hash_crypt_private(password, hash)); // #1561: fix timing issue
        }
    }
}
