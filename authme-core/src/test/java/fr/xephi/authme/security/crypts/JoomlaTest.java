package fr.xephi.authme.security.crypts;

/**
 * Test for {@link Joomla}.
 */
class JoomlaTest extends AbstractEncryptionMethodTest {

    JoomlaTest() {
        super(new Joomla(),
            "b18c99813cd96df3a706652f47177490:377c4aaf92c5ed57711306909e6065ca", // password
            "c5af71da91a8841d95937ba24a5b7fdb:07068e5850930b794526a614438cafc7", // PassWord1
            "f5fccd5166af7080833d7c7a6a531295:7cb6eeabcfac67ffe1341ec43375a9e6", // &^%te$t?Pw@_
            "dce946c6864d2223caeed9d80f356bcc:0c55fa3eca8c42557a989700ac1c4b8e"  // âË_3(íù*
        );
    }

}
