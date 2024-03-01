package fr.xephi.authme.security.crypts;

/**
 * Test for {@link Ipb3}.
 */
class Ipb3Test extends AbstractEncryptionMethodTest {

    Ipb3Test() {
        super(new Ipb3(),
            new HashedPassword("f8ecea1ce42b5babef369ff7692dbe3f", "1715b"),  //password
            new HashedPassword("40a93731a931352e0619cdf09b975040", "ba91c"),  //PassWord1
            new HashedPassword("a77ca982373946d5800430bd2947ba11", "a7725"),  //&^%te$t?Pw@_
            new HashedPassword("383d7b9e2b707d6e894ec7b30e3032c3", "fa9fd")); //âË_3(íù*
    }

}
