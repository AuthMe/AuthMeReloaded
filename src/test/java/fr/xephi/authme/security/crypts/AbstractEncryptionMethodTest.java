package fr.xephi.authme.security.crypts;

import fr.xephi.authme.security.PasswordSecurity;
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
                String hash = method.getHash(password, getSalt(method), USERNAME);
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

    static void generateHashes(EncryptionMethod method) {
        System.out.println("AbstractEncryptionMethodTest.testGivenPasswords(method,");
        for (String password : GIVEN_PASSWORDS) {
            try {
                System.out.println("\t\"" + method.getHash(password, getSalt(method), "USERNAME")
                    + "\", // " + password);
            } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException("Could not generate hash", e);
            }
        }
        System.out.println(");");
    }

    // TODO #358: Remove this method and use the new salt method on the interface
    private static String getSalt(EncryptionMethod method) {
        try {
            if (method instanceof BCRYPT) {
                return BCRYPT.gensalt();
            } else if (method instanceof MD5) {
                return "";
            } else if (method instanceof JOOMLA) {
                return PasswordSecurity.createSalt(32);
            } else if (method instanceof SHA256) {
                return PasswordSecurity.createSalt(16);
            }
        } catch (NoSuchAlgorithmException e) {
                throw new RuntimeException(e);
            }
        throw new RuntimeException("Unknown EncryptionMethod for salt generation");
    }

}
