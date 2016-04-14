package fr.xephi.authme.security.crypts;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.util.WrapperMock;
import org.junit.BeforeClass;

/**
 * Test for {@link XFBCRYPT}.
 */
public class XFBCRYPTTest extends AbstractEncryptionMethodTest {

    @BeforeClass
    public static void setup() {
        WrapperMock.createInstance();
        TestHelper.setupLogger();
    }

    public XFBCRYPTTest() {
        super(new XFBCRYPT(),
            "$2a$10$UtuON/ZG.x8EWG/zQbryB.BHfQVrfxk3H7qykzP.UJQ8YiLjZyfqq",  // password
            "$2a$10$Q.ocUo.YtHTdI4nu3pcpKun6BILcmWHm541ANULucmuU/ps1QKY4K",  // PassWord1
            "$2a$10$yHjm02.K4HP5iFU1F..yLeTeo7PWZVbKAr/QGex5jU4.J3mdq/uuO",  // &^%te$t?Pw@_
            "$2a$10$joIayhGStExKWxNbiqMMPOYFSpQ76HVNjpOB7.QwTmG5q.TiJJ.0e"); // âË_3(íù*
    }
}
