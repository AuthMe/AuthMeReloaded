package fr.xephi.authme.security.crypts;

/**
 * Test for {@link Md5vB}.
 */
class Md5vBTest extends AbstractEncryptionMethodTest {

    Md5vBTest() {
        super(new Md5vB(),
            "$MD5vb$bd9832fffa287321$5006d371fcb813f2347987f902a024ad",  // password
            "$MD5vb$5e492c1166b5a828$c954fa5ee561700a097826971653b57f",  // PassWord1
            "$MD5vb$3ec43cd46a61d70b$59687c0976f2e327b1245c8063f7008c",  // &^%te$t?Pw@_
            "$MD5vb$2fb6bf22929e3127$a7155b88e2899561fe16b14ccdb0d935"); // âË_3(íù*
    }

}
