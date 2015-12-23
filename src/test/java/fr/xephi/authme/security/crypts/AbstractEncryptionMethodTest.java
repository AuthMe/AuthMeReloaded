package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.PasswordSecurity;
import org.junit.Ignore;
import org.junit.Test;

import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test for implementations of {@link EncryptionMethod}.
 */
// TODO #358: Remove NoSuchAlgorithm try-catch-es when no longer necessary
public abstract class AbstractEncryptionMethodTest {

    public static final String USERNAME = "Test_Player00";
    public static final String[] GIVEN_PASSWORDS = {"password", "PassWord1", "&^%te$t?Pw@_", "âË_3(íù*"};
    private static final String[] INTERNAL_PASSWORDS = {"test1234", "Ab_C73", "(!#&$~`_-Aa0", "Ûïé1&?+A"};

    private EncryptionMethod method;
    private Map<String, String> hashes;

    public AbstractEncryptionMethodTest(EncryptionMethod method, String hash0, String hash1,
                                        String hash2, String hash3) {
        this.method = method;
        hashes = new HashMap<>();
        hashes.put(GIVEN_PASSWORDS[0], hash0);
        hashes.put(GIVEN_PASSWORDS[1], hash1);
        hashes.put(GIVEN_PASSWORDS[2], hash2);
        hashes.put(GIVEN_PASSWORDS[3], hash3);
    }

    @Test
    public void testGivenPasswords() {
        for (String password : GIVEN_PASSWORDS) {
            try {
                assertTrue("Hash for password '" + password + "' should match",
                    method.comparePassword(hashes.get(password), password, USERNAME));
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("EncryptionMethod '" + method + "' threw exception", e);
            }
        }
    }

    @Test
    public void testPasswordEquality() {
        for (String password : INTERNAL_PASSWORDS) {
            try {
                String hash = method.computeHash(password, getSalt(method), USERNAME);
                assertTrue("Generated hash for '" + password + "' should match password (hash = '" + hash + "')",
                    method.comparePassword(hash, password, USERNAME));
                if (!password.equals(password.toLowerCase())) {
                    assertFalse("Lower-case of '" + password + "' should not match generated hash '" + hash + "'",
                        method.comparePassword(hash, password.toLowerCase(), USERNAME));
                }
                if (!password.equals(password.toUpperCase())) {
                    assertFalse("Upper-case of '" + password + "' should not match generated hash '" + hash + "'",
                        method.comparePassword(hash, password.toUpperCase(), USERNAME));
                }
            } catch (NoSuchAlgorithmException e) {
                throw new IllegalStateException("EncryptionMethod '" + method + "' threw exception", e);
            }
        }
    }

    // @org.junit.Test public void a() { AbstractEncryptionMethodTest.generateTest(); }
    // TODO #364: Remove this method
    static void generateTest(EncryptionMethod method) {
        String className = method.getClass().getSimpleName();
        System.out.println("/**\n * Test for {@link " + className + "}.\n */");
        System.out.println("public class " + className + "Test extends AbstractEncryptionMethodTest {");
        System.out.println("\n\tpublic " + className + "Test() {");
        System.out.println("\t\tsuper(new " + className + "(),");

        String delim = ",  ";
        for (String password : GIVEN_PASSWORDS) {
            if (password.equals(GIVEN_PASSWORDS[GIVEN_PASSWORDS.length - 1])) {
                delim = "); ";
            }
            try {
                System.out.println("\t\t\"" + method.computeHash(password, getSalt(method), USERNAME)
                    + "\"" + delim + "// " + password);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Could not generate hash", e);
            }
        }
        System.out.println("\t}");
        System.out.println("\n}");
    }

    // TODO #358: Remove this method and use the new salt method on the interface
    private static String getSalt(EncryptionMethod method) {
        try {
            if (method instanceof BCRYPT) {
                return BCRYPT.gensalt();
            } else if (method instanceof MD5 || method instanceof WORDPRESS || method instanceof SMF
                || method instanceof SHA512 || method instanceof SHA1 || method instanceof ROYALAUTH
                || method instanceof DOUBLEMD5) {
                return "";
            } else if (method instanceof JOOMLA || method instanceof SALTEDSHA512) {
                return PasswordSecurity.createSalt(32);
            } else if (method instanceof SHA256 || method instanceof PHPBB || method instanceof WHIRLPOOL
                || method instanceof MD5VB || method instanceof BCRYPT2Y) {
                return PasswordSecurity.createSalt(16);
            } else if (method instanceof WBB3) {
                return PasswordSecurity.createSalt(40);
            } else if (method instanceof XAUTH || method instanceof CryptPBKDF2Django
                || method instanceof CryptPBKDF2) {
                return PasswordSecurity.createSalt(12);
            } else if (method instanceof WBB4) {
                return BCRYPT.gensalt(8);
            }
        } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        throw new RuntimeException("Unknown EncryptionMethod for salt generation");
    }

}
