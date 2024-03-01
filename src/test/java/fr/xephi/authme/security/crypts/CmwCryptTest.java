package fr.xephi.authme.security.crypts;

/**
 * Test for {@link CmwCrypt}.
 */
class CmwCryptTest extends AbstractEncryptionMethodTest {

    CmwCryptTest() {
        super(new CmwCrypt(),
            "1619d7adc23f4f633f11014d2f22b7d8",  // password
            "c651798d2d9da38f86654107ae60c86a",  // PassWord1
            "1fff869a744700cdb623a403c46e93ea",  // &^%te$t?Pw@_
            "6436230e0effff37af79302147319dda"); // âË_3(íù*
    }
}
