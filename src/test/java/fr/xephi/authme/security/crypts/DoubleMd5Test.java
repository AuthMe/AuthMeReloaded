package fr.xephi.authme.security.crypts;

/**
 * Test for {@link DoubleMd5}.
 */
class DoubleMd5Test extends AbstractEncryptionMethodTest {

    DoubleMd5Test() {
        super(new DoubleMd5(),
            "696d29e0940a4957748fe3fc9efd22a3",  // password
            "c77aa2024d9fb7233a2872452d601aba",  // PassWord1
            "fbd5790af706ec19f8a7ef161878758b",  // &^%te$t?Pw@_
            "cf3b0b6c6c7a4da95019634fb732aaf0"); // âË_3(íù*
    }

}
