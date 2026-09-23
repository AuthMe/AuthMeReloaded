package fr.xephi.authme.security.crypts;

/**
 * Test for {@link Sha1}.
 */
class Sha1Test extends AbstractEncryptionMethodTest {

    Sha1Test() {
        super(new Sha1(),
            "5baa61e4c9b93f3f0682250b6cf8331b7ee68fd8",  // password
            "285d0c707f9644b75e1a87a62f25d0efb56800f0",  // PassWord1
            "a42ef8e61e890af80461ca5dcded25cbfcf407a4",  // &^%te$t?Pw@_
            "64a8fb6e043105ba6cf3f2d63d59ca24d80aabbb"); // âË_3(íù*
    }

}
