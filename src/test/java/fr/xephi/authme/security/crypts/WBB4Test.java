package fr.xephi.authme.security.crypts;

/**
 * Test for {@link WBB4}.
 */
public class WBB4Test extends AbstractEncryptionMethodTest {

    public WBB4Test() {
        super(new WBB4(),
            "$2a$08$7DGr.wROqEPe0Z3XJS7n5.k.QWehovLHbpI.UkdfRb4ns268WsR6C",  // password
            "$2a$08$yWWVUA4PB4mqW.0wyIvV3OdoH492HuLk5L3iaqUrpRK2.2zn08d/K",  // PassWord1
            "$2a$08$EHXUFt7bTT9Fnsu22KWvF.QDssiosV8YzH8CyWqulB/ckOA7qioJG",  // &^%te$t?Pw@_
            "$2a$08$ZZu5YH4zwpk0cr2dOYZpF.CkTKMvCBOAtTbAH7AwnOiL.n0mWkgDC"); // âË_3(íù*
    }

}
