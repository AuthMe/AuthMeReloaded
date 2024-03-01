package fr.xephi.authme.security.crypts;

/**
 * Test for {@link MyBB}.
 */
class MyBBTest extends AbstractEncryptionMethodTest {

    MyBBTest() {
        super(new MyBB(),
            new HashedPassword("57c7a16d860833db5030738f5a465d2b", "acdc14e6"),  //password
            new HashedPassword("08fbdf721f2c42d9780b7d66df0ba830", "792fd7fb"),  //PassWord1
            new HashedPassword("d602f38fb59ad9e185d5604f5d4ddb36", "4b5534a4"),  //&^%te$t?Pw@_
            new HashedPassword("b3c39410d0ab8ae2a65c257820797fad", "e5a6cb14")); //âË_3(íù*
    }

}
