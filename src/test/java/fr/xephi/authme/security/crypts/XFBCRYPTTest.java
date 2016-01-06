package fr.xephi.authme.security.crypts;

import org.junit.Ignore;
import org.junit.Test;

/**
 * Test for {@link XFBCRYPT}.
 */
@Ignore
// TODO #137: Create a test class as for the other encryption methods. Simply run the following test and copy the
// output -- that's your test class! (Once XFBCRYPT.java actually works properly)
// @org.junit.Test public void a() { AbstractEncryptionMethodTest.generateTest(new XFBCRYPT()); }
public class XFBCRYPTTest {

    @Test
    public void shouldComputeHash() {
        System.out.println(new XFBCRYPT().computeHash("Test", "name"));
    }
}
