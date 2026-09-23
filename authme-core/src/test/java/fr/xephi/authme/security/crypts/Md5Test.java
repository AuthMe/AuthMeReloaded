package fr.xephi.authme.security.crypts;

/**
 * Test for {@link Md5}.
 */
class Md5Test extends AbstractEncryptionMethodTest {

    Md5Test() {
        super(new Md5(),
            "5f4dcc3b5aa765d61d8327deb882cf99",  // password
            "f2126d405f46ed603ff5b2950f062c96",  // PassWord1
            "0833dcd2bc741f90c46bbac5498fd08f",  // &^%te$t?Pw@_
            "d1accd961cb7b688c87278191c1dfed3"); // âË_3(íù*
    }

}
