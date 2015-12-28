package fr.xephi.authme.security.crypts;

/**
 * Test for {@link IPB3}.
 */
public class IPB3Test extends AbstractEncryptionMethodTest {

    public IPB3Test() {
        super(new IPB3(),
            new HashResult("f8ecea1ce42b5babef369ff7692dbe3f", "1715b"),  //password
            new HashResult("40a93731a931352e0619cdf09b975040", "ba91c"),  //PassWord1
            new HashResult("a77ca982373946d5800430bd2947ba11", "a7725"),  //&^%te$t?Pw@_
            new HashResult("383d7b9e2b707d6e894ec7b30e3032c3", "fa9fd")); //âË_3(íù*
    }

}
