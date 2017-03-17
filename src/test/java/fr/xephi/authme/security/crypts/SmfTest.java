package fr.xephi.authme.security.crypts;

/**
 * Test for {@link Smf}.
 */
public class SmfTest extends AbstractEncryptionMethodTest {

    public SmfTest() {
        super(new Smf(),
            "9b361c66977bb059d460a20d3c21fb3394772df5",  // password
            "31a560bdd095a837945d46add1605108ba87b268",  // PassWord1
            "8d4b84544e0891be8c183fe9b1003cfac18c51a1",  // &^%te$t?Pw@_
            "03cca5af1eb0a93be47777651b2e7be4fd5d537d"); // âË_3(íù*
    }

}
