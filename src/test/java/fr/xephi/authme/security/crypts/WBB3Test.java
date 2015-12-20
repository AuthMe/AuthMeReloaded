package fr.xephi.authme.security.crypts;

import org.junit.Ignore;

/**
 * Test for {@link WBB3}.
 */
@Ignore
// TODO #364 ljacqu 20151220: Unignore test after fixing closely coupled DB dependency
public class WBB3Test extends AbstractEncryptionMethodTest {

    public WBB3Test() {
        super(new WBB3(),
            "ca426c4d20a82cd24c7bb07d94d69f3757e3d07d",  // password
            "72d59d27674a3cace2600ff152ba8b46274e27e9",  // PassWord1
            "23daf26602e52591156968a14c2a6592b5be4743",  // &^%te$t?Pw@_
            "d3908efe4a15314066391dd8572883c70b16fd8a"); // âË_3(íù*
    }

}
