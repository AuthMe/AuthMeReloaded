package fr.xephi.authme.security.crypts;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

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
    private static final String[] INTERNAL_PASSWORDS = {"test1234", "Ab_C73", "(!#&$~`_-Aa0", "Ûïé1&?+A"};

    /** The encryption method to test. */
    private EncryptionMethod method;
    /** Map with the hashes against which the entries in GIVEN_PASSWORDS are tested. */
    private Map<String, String> hashes;
    /** The accompanying salts for the hashes in {@link #hashes} if necessary. Can be empty otherwise. */
    private Map<String, String> salts;

    /**
     * Create a new test for the given encryption method.
     *
     * @param method The encryption method to test
     * @param hash0  The pre-generated hash for the first {@link #GIVEN_PASSWORDS}
     * @param hash1  The pre-generated hash for the second {@link #GIVEN_PASSWORDS}
     * @param hash2  The pre-generated hash for the third {@link #GIVEN_PASSWORDS}
     * @param hash3  The pre-generated hash for the fourth {@link #GIVEN_PASSWORDS}
     */
    public AbstractEncryptionMethodTest(EncryptionMethod method, String hash0, String hash1,
                                        String hash2, String hash3) {
        // TODO #358: Throw if method.hasSeparateSalt() is true
        this.method = method;
        hashes = new HashMap<>();
        hashes.put(GIVEN_PASSWORDS[0], hash0);
        hashes.put(GIVEN_PASSWORDS[1], hash1);
        hashes.put(GIVEN_PASSWORDS[2], hash2);
        hashes.put(GIVEN_PASSWORDS[3], hash3);
        salts = new HashMap<>();
    }

    public AbstractEncryptionMethodTest(EncryptionMethod method, HashResult result0, HashResult result1,
                                        HashResult result2, HashResult result3) {
        // TODO #358: Throw if method.hasSeparateSalt() is false
        this.method = method;

        hashes = new HashMap<>();
        hashes.put(GIVEN_PASSWORDS[0], result0.getHash());
        hashes.put(GIVEN_PASSWORDS[1], result1.getHash());
        hashes.put(GIVEN_PASSWORDS[2], result2.getHash());
        hashes.put(GIVEN_PASSWORDS[3], result3.getHash());

        salts = new HashMap<>();
        salts.put(GIVEN_PASSWORDS[0], result0.getSalt());
        salts.put(GIVEN_PASSWORDS[1], result1.getSalt());
        salts.put(GIVEN_PASSWORDS[2], result2.getSalt());
        salts.put(GIVEN_PASSWORDS[3], result3.getSalt());
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
        // TODO #358: Remove instanceof and use this code always
        if (method instanceof NewEncrMethod) {
            NewEncrMethod method1 = (NewEncrMethod) method;
            for (String password : INTERNAL_PASSWORDS) {
                final String salt = method1.generateSalt();
                final String hash = method1.computeHash(password, salt, USERNAME);

                // Check that the computeHash(password, salt, name) method has the same output for the returned salt
                assertThat(hash, equalTo(method1.computeHash(password, salt, USERNAME)));

                assertTrue("Generated hash for '" + password + "' should match password (hash = '" + hash + "')",
                    method1.comparePassword(hash, password, salt, USERNAME));
                if (!password.equals(password.toLowerCase())) {
                    assertFalse("Lower-case of '" + password + "' should not match generated hash '" + hash + "'",
                        method1.comparePassword(hash, password.toLowerCase(), salt, USERNAME));
                }
                if (!password.equals(password.toUpperCase())) {
                    assertFalse("Upper-case of '" + password + "' should not match generated hash '" + hash + "'",
                        method1.comparePassword(hash, password.toUpperCase(), salt, USERNAME));
                }
            }
            return;
        }

        fail("No longer supporting old EncryptionMethod implementations");
    }

    private boolean doesGivenHashMatch(String password, EncryptionMethod method) {
        // TODO #358: Remove casting checks and remove old code below
        if (method instanceof NewEncrMethod) {
            NewEncrMethod method1 = (NewEncrMethod) method;
            String hash = hashes.get(password);
            String salt = salts.get(password);
            return method1.comparePassword(hash, password, salt, USERNAME);
        }


        // TODO #358: Remove line below
        return method.comparePassword(hashes.get(password), password, USERNAME);
    }

    // @org.junit.Test public void a() { AbstractEncryptionMethodTest.generateTest(); }
    // TODO #364: Remove this method
    static void generateTest(EncryptionMethod method) {
        String className = method.getClass().getSimpleName();
        System.out.println("/**\n * Test for {@link " + className + "}.\n */");
        System.out.println("public class " + className + "Test extends AbstractEncryptionMethodTest {");
        System.out.println("\n\tpublic " + className + "Test() {");
        System.out.println("\t\tsuper(new " + className + "(),");

        NewEncrMethod method1 = null;
        if (method instanceof NewEncrMethod) {
            method1 = (NewEncrMethod) method;
            if (!method1.hasSeparateSalt()) method1 = null;
        }


        String delim = ",  ";
        for (String password : GIVEN_PASSWORDS) {
            if (password.equals(GIVEN_PASSWORDS[GIVEN_PASSWORDS.length - 1])) {
                delim = "); ";
            }
            if (method1 != null) {
                HashResult hashResult = method1.computeHash(password, USERNAME);
                System.out.println(String.format("\t\tnew HashResult(\"%s\", \"%s\")%s// %s",
                    hashResult.getHash(), hashResult.getSalt(), delim, password));
            } else {
                System.out.println("\t\t\"" + method.computeHash(password, null, USERNAME)
                    + "\"" + delim + "// " + password);
            }
        }
        System.out.println("\t}");
        System.out.println("\n}");
    }

}
