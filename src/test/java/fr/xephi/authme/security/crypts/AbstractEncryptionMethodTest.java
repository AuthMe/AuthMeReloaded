package fr.xephi.authme.security.crypts;

import com.google.common.collect.ImmutableList;
import fr.xephi.authme.security.crypts.description.AsciiRestricted;
import org.junit.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

/**
 * Test for implementations of {@link EncryptionMethod}.
 */
// TODO #358: Remove NoSuchAlgorithm try-catch-es when no longer necessary
public abstract class AbstractEncryptionMethodTest {

    /** The username used to query {@link EncryptionMethod#comparePassword}. */
    public static final String USERNAME = "Test_Player00";
    /**
     * List of passwords whose hash is provided to the class to test against; this verifies that previously constructed
     * hashes remain valid.
     */
    public static final String[] GIVEN_PASSWORDS = {"password", "PassWord1", "&^%te$t?Pw@_", "âË_3(íù*"};
    /**
     * List of passwords that are hashed at runtime and then tested against; this verifies that hashes that are
     * generated are valid.
     */
    private static final List<String> INTERNAL_PASSWORDS =
        ImmutableList.of("test1234", "Ab_C73", "(!#&$~`_-Aa0", "Ûïé1&?+A");

    /** The encryption method to test. */
    private EncryptionMethod method;
    /** Map with the hashes against which the entries in GIVEN_PASSWORDS are tested. */
    private Map<String, EncryptedPassword> hashes;

    /**
     * Create a new test for the given encryption method.
     *
     * @param method The encryption method to test
     * @param computedHashes The pre-generated hashes for the elements in {@link #GIVEN_PASSWORDS}
     */
    public AbstractEncryptionMethodTest(EncryptionMethod method, String... computedHashes) {
        if (method.hasSeparateSalt()) {
            throw new UnsupportedOperationException("Test must be initialized with EncryptedPassword objects if "
                + "the salt is stored separately. Use the other constructor");
        } else if (computedHashes.length != GIVEN_PASSWORDS.length) {
            throw new UnsupportedOperationException("Expected " + GIVEN_PASSWORDS.length + " hashes");
        }
        this.method = method;

        hashes = new HashMap<>();
        for (int i = 0; i < GIVEN_PASSWORDS.length; ++i) {
            hashes.put(GIVEN_PASSWORDS[i], new EncryptedPassword(computedHashes[i]));
        }
    }

    public AbstractEncryptionMethodTest(EncryptionMethod method, EncryptedPassword result0, EncryptedPassword result1,
                                        EncryptedPassword result2, EncryptedPassword result3) {
        if (!method.hasSeparateSalt()) {
            throw new UnsupportedOperationException("Salt is not stored separately, so test should be initialized"
                + " with the password hashes only. Use the other constructor");
        }
        this.method = method;

        hashes = new HashMap<>();
        hashes.put(GIVEN_PASSWORDS[0], result0);
        hashes.put(GIVEN_PASSWORDS[1], result1);
        hashes.put(GIVEN_PASSWORDS[2], result2);
        hashes.put(GIVEN_PASSWORDS[3], result3);
    }

    @Test
    public void testGivenPasswords() {
        // Test all entries in GIVEN_PASSWORDS except the last one
        for (int i = 0; i < GIVEN_PASSWORDS.length - 1; ++i) {
            String password = GIVEN_PASSWORDS[i];
            assertTrue("Hash for password '" + password + "' should match",
                doesGivenHashMatch(password, method));
        }

        // Note #375: Windows console seems to use its own character encoding (Windows-1252?) and it seems impossible to
        // force it to use UTF-8, so passwords with non-ASCII characters will fail. Since we do not recommend to use
        // such characters in passwords (something outside of our control, e.g. a database system, might also cause
        // problems), we will check the last password in GIVEN_PASSWORDS in a non-failing way; if the hash doesn't match
        // we'll just issue a message to System.err
        String lastPassword = GIVEN_PASSWORDS[GIVEN_PASSWORDS.length - 1];
        if (!doesGivenHashMatch(lastPassword, method)) {
            System.err.println("Note: Hash for password '" + lastPassword + "' does not match for method " + method);
        }
    }

    @Test
    public void testPasswordEquality() {
        List<String> internalPasswords = method.getClass().isAnnotationPresent(AsciiRestricted.class)
            ? INTERNAL_PASSWORDS.subList(0, INTERNAL_PASSWORDS.size() - 1)
            : INTERNAL_PASSWORDS;

        for (String password : internalPasswords) {
            final String salt = method.generateSalt();
            final String hash = method.computeHash(password, salt, USERNAME);
            EncryptedPassword encryptedPassword = new EncryptedPassword(hash, salt);

            // Check that the computeHash(password, salt, name) method has the same output for the returned salt
            if (testHashEqualityForSameSalt()) {
                assertThat("Computing a hash with the same salt will generate the same hash",
                    hash, equalTo(method.computeHash(password, salt, USERNAME)));
            }

            assertTrue("Generated hash for '" + password + "' should match password (hash = '" + hash + "')",
                method.comparePassword(password, encryptedPassword, USERNAME));
            if (!password.equals(password.toLowerCase())) {
                assertFalse("Lower-case of '" + password + "' should not match generated hash '" + hash + "'",
                    method.comparePassword(password.toLowerCase(), encryptedPassword, USERNAME));
            }
            if (!password.equals(password.toUpperCase())) {
                assertFalse("Upper-case of '" + password + "' should not match generated hash '" + hash + "'",
                    method.comparePassword(password.toUpperCase(), encryptedPassword, USERNAME));
            }
        }
    }

    private boolean doesGivenHashMatch(String password, EncryptionMethod method) {
        return method.comparePassword(password, hashes.get(password), USERNAME);
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

            if (method.hasSeparateSalt()) {
                EncryptedPassword encryptedPassword = method.computeHash(password, USERNAME);
                System.out.println(String.format("\t\tnew EncryptedPassword(\"%s\", \"%s\")%s// %s",
                    encryptedPassword.getHash(), encryptedPassword.getSalt(), delim, password));
            } else {
                System.out.println("\t\t\"" + method.computeHash(password, USERNAME).getHash()
                    + "\"" + delim + "// " + password);
            }
        }
        System.out.println("\t}");
        System.out.println("\n}");
    }

    /**
     * Return whether an encryption algorithm should be tested that it generates the same
     * hash for the same salt. If {@code true}, we call {@link EncryptionMethod#computeHash(String, String)}
     * and verify that {@link EncryptionMethod#computeHash(String, String, String)} generates
     * the same hash for the salt returned in the first call.
     *
     * @return Whether or not to test that the hash is the same for the same salt
     */
    protected boolean testHashEqualityForSameSalt() {
        return true;
    }

}
