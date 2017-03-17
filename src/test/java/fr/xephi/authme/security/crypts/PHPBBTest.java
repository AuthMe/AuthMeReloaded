package fr.xephi.authme.security.crypts;

/**
 * Test for {@link PhpBB}.
 */
public class PhpBBTest extends AbstractEncryptionMethodTest {

    public PhpBBTest() {
        super(new PhpBB(),
            "$H$7MaSGQb0xe3Fp/a.Q.Ewpw.UKfCv.t0",  // password
            "$H$7ESfAVjzqajC7fJFcZKZIhyds41MuW.",  // PassWord1
            "$H$7G65SXRPbR69jLg.qZTjtqsw36Ciw7.",  // &^%te$t?Pw@_
            "$H$7Brcg8zO9amr2SHVgz.pFxprDu40v4/"); // âË_3(íù*
    }

}
