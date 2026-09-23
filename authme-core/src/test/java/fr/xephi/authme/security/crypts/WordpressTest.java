package fr.xephi.authme.security.crypts;

/**
 * Test for {@link Wordpress}.
 */
class WordpressTest extends AbstractEncryptionMethodTest {

    WordpressTest() {
        super(new Wordpress(),
            "$P$B9wyjxuU4yrfjnnHNGSzH9ti9CC0Os1",  // password
            "$P$BjzPjjzPjjkRzvGGRTyYu0sNqcz6Ci0",  // PassWord1
            "$P$BjzPjjzPjrAOyB1V0WFdpisgCTFx.N/",  // &^%te$t?Pw@_
            "$P$BjzPjxxyjp2QdKcab/oTW8l/W0AgE21"); // âË_3(íù*
    }

    @Override
    protected boolean testHashEqualityForSameSalt() {
        // We need to skip the test because Wordpress uses an "internal salt" that is not exposed to the outside
        return false;
    }
}
