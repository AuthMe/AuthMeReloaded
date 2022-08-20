package fr.xephi.authme.security.crypts;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.security.crypts.description.AsciiRestricted;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assume.assumeThat;

/**
 * Test for implementations of {@link EncryptionMethod}.
 */
public abstract class AbstractEncryptionMethodTest {

    /** The username used to query {@link EncryptionMethod#comparePassword}. */
    private static final String USERNAME = "Test_Player00";
    /**
     * List of passwords whose hash is provided to the class to test against; this verifies that previously constructed
     * hashes remain valid.
     */
    private static final String[] GIVEN_PASSWORDS = {"password", "PassWord1", "&^%te$t?Pw@_", "âË_3(íù*"};
    /**
     * List of passwords that are hashed at runtime and then tested against; this verifies that newly generated hashes
     * are valid.
     */
    private static final List<String> INTERNAL_PASSWORDS =
        ImmutableList.of("test1234", "Ab_C73", "(!#&$~`_-Aa0", "Ûïé1&?+A");

    private static final String[] BOGUS_HASHES = {"", "test", "$t$test$", "$SHA$Test$$$$", "$$$$$",
        "asdfg:hjkl", "::test", "~#$#~~~#$#~", "d41d8cd98f00b204e9800998ecf427e",
        "$2y$7a$da641e404b982ed" };

    /**
     * Certain hash algorithms are slow by design, which has a considerable effect on these unit tests.
     * Setting the property below to "true" will reduce the checks in these unit tests as to offer fast,
     * partial tests during development.
     */
    private static final boolean SKIP_LONG_TESTS =
        "true".equals(System.getProperty("project.skipExtendedHashTests"));

    /** The encryption method to test. */
    private EncryptionMethod method;
    /** Map with the hashes against which the entries in GIVEN_PASSWORDS are tested. */
    private Map<String, HashedPassword> hashes;

    /**
     * Creates a new test for the given encryption method. This is for encryption methods that do not have
     * a separate salt.
     *
     * @param method the encryption method to test
     * @param hash0 the pre-generated hash for the first entry in {@link #GIVEN_PASSWORDS}
     * @param hash1 hash for second given password
     * @param hash2 hash for third given password
     * @param hash3 hash for fourth given password
     */
    public AbstractEncryptionMethodTest(EncryptionMethod method, String hash0,
                                        String hash1, String hash2, String hash3) {
        verifyCorrectConstructorIsUsed(method, false);
        this.method = method;

        hashes = ImmutableMap.of(
            GIVEN_PASSWORDS[0], new HashedPassword(hash0),
            GIVEN_PASSWORDS[1], new HashedPassword(hash1),
            GIVEN_PASSWORDS[2], new HashedPassword(hash2),
            GIVEN_PASSWORDS[3], new HashedPassword(hash3));
    }

    /**
     * Creates a new test for the given encryption method. This is for encryption methods that use
     * a separate salt.
     *
     * @param method the encryption method to test
     * @param result0 the pre-generated hash for the first entry in {@link #GIVEN_PASSWORDS}
     * @param result1 hash for second given password
     * @param result2 hash for third given password
     * @param result3 hash for fourth given password
     */
    public AbstractEncryptionMethodTest(EncryptionMethod method, HashedPassword result0, HashedPassword result1,
                                        HashedPassword result2, HashedPassword result3) {
        verifyCorrectConstructorIsUsed(method, true);
        this.method = method;

        hashes = ImmutableMap.of(
            GIVEN_PASSWORDS[0], result0,
            GIVEN_PASSWORDS[1], result1,
            GIVEN_PASSWORDS[2], result2,
            GIVEN_PASSWORDS[3], result3);
    }

    @BeforeClass
    public static void setupLogger() {
        TestHelper.setupLogger();
    }

    protected void verifyCorrectConstructorIsUsed(EncryptionMethod method, boolean isConstructorWithSalt) {
        if (isConstructorWithSalt && !method.hasSeparateSalt()) {
            throw new UnsupportedOperationException("Salt is not stored separately, so test should be initialized"
                + " with the password hashes only. Use the other constructor");
        } else if (!isConstructorWithSalt && method.hasSeparateSalt()) {
            throw new UnsupportedOperationException("Test must be initialized with HashedPassword objects if "
                + "the salt is stored separately. Use the other constructor");
        }
    }

    @Test
    public void testGivenPasswords() {
        // Start with the 2nd to last password if we skip long tests
        int start = SKIP_LONG_TESTS ? GIVEN_PASSWORDS.length - 2 : 0;
        // Test entries in GIVEN_PASSWORDS except the last one
        for (int i = start; i < GIVEN_PASSWORDS.length - 1; ++i) {
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
            HashedPassword hashedPassword = new HashedPassword(hash, salt);

            // Check that the computeHash(password, salt, name) method has the same output for the returned salt
            if (testHashEqualityForSameSalt()) {
                assertThat("Computing a hash with the same salt will generate the same hash",
                    hash, equalTo(method.computeHash(password, salt, USERNAME)));
            }

            assertTrue("Generated hash for '" + password + "' should match password (hash = '" + hash + "')",
                method.comparePassword(password, hashedPassword, USERNAME));
            assumeThat(SKIP_LONG_TESTS, equalTo(false));

            if (!password.equals(password.toLowerCase(Locale.ROOT))) {
                assertFalse("Lower-case of '" + password + "' should not match generated hash '" + hash + "'",
                    method.comparePassword(password.toLowerCase(Locale.ROOT), hashedPassword, USERNAME));
            }
            if (!password.equals(password.toUpperCase(Locale.ROOT))) {
                assertFalse("Upper-case of '" + password + "' should not match generated hash '" + hash + "'",
                    method.comparePassword(password.toUpperCase(Locale.ROOT), hashedPassword, USERNAME));
            }
        }
    }

    /** Tests various strings to ensure that encryption methods don't rely on the hash's format too much. */
    @Test
    public void testMalformedHashes() {
        assumeThat(SKIP_LONG_TESTS, equalTo(false));
        String salt = method.hasSeparateSalt() ? "testSalt" : null;
        for (String bogusHash : BOGUS_HASHES) {
            HashedPassword hashedPwd = new HashedPassword(bogusHash, salt);
            assertFalse("Passing bogus hash '" + bogusHash + "' does not result in an error",
                method.comparePassword("Password", hashedPwd, "player"));
        }
    }

    private boolean doesGivenHashMatch(String password, EncryptionMethod method) {
        return method.comparePassword(password, hashes.get(password), USERNAME);
    }

    /**
     * Generates a test class for a given encryption method. Simply create a test class and run the following code,
     * replacing {@code XXX} with the actual class:
     * <p>
     * <code>@org.junit.Test public void generate() { AbstractEncryptionMethodTest.generateTest(new XXX()); }</code>
     * <p>
     * The output is the entire test class.
     *
     * @param method The method to create a test class for
     */
    protected static void generateTest(EncryptionMethod method) {
        String className = method.getClass().getSimpleName();
        // Create javadoc and "public class extends" and the constructor call "super(new Class(),"
        System.out.println("/**\n * Test for {@link " + className + "}.\n */");
        System.out.println("public class " + className + "Test extends AbstractEncryptionMethodTest {");
        System.out.println("\n\tpublic " + className + "Test() {");
        System.out.println("\t\tsuper(new " + className + "(),");

        // Iterate through the GIVEN_PASSWORDS and generate a hash so we can always check it later
        String delim = ",  ";
        for (String password : GIVEN_PASSWORDS) {
            if (password.equals(GIVEN_PASSWORDS[GIVEN_PASSWORDS.length - 1])) {
                delim = "); ";
            }

            // Encr. method uses separate salt, so we need to call the constructor that takes HashedPassword instances
            if (method.hasSeparateSalt()) {
                HashedPassword hashedPassword = method.computeHash(password, USERNAME);
                System.out.println(String.format("\t\tnew HashedPassword(\"%s\", \"%s\")%s// %s",
                    hashedPassword.getHash(), hashedPassword.getSalt(), delim, password));
            } else {
                // Encryption method doesn't have separate salt, so simply pass the generated hash to the constructor
                System.out.println("\t\t\"" + method.computeHash(password, USERNAME).getHash()
                    + "\"" + delim + "// " + password);
            }
        }

        // Close the constructor and class declarations
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
