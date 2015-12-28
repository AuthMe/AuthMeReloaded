package fr.xephi.authme.security.crypts;

import org.junit.Ignore;

/**
 * Test for {@link WORDPRESS}.
 */
@Ignore
// TODO #364: Need to skip an assertion due to the "internal salt" of Wordpress
public class WORDPRESSTest extends AbstractEncryptionMethodTest {

    public WORDPRESSTest() {
        super(new WORDPRESS(),
            "$P$B9wyjxuU4yrfjnnHNGSzH9ti9CC0Os1",  // password
            "$P$BjzPjjzPjjkRzvGGRTyYu0sNqcz6Ci0",  // PassWord1
            "$P$BjzPjjzPjrAOyB1V0WFdpisgCTFx.N/",  // &^%te$t?Pw@_
            "$P$BjzPjxxyjp2QdKcab/oTW8l/W0AgE21"); // âË_3(íù*
    }
}
