package fr.xephi.authme.security.crypts;

import org.junit.Ignore;

/**
 * Test for {@link WBB4}.
 */
@Ignore
// TODO #369: Fix WBB4 hash and un-ignore this test
public class WBB4Test extends AbstractEncryptionMethodTest {

    public WBB4Test() {
        super(new WBB4(),
            "$2a$08$GktrHRoOk0EHrl3ONsFmieIbjq7EIzBx8dhsWiCmn6sWwO3b3DoRO",  // password
            "$2a$08$ouvtovnHgPWz6YHuOhyct.I2/j1xTOLG8OTuEn1/YqtkiRJYUV7lq",  // PassWord1
            "$2a$08$z.qWFh7k0qvIu5.qiq/Wuu2HDCNH7LNlMDNhN61F1ISsV8wZRKD0.",  // &^%te$t?Pw@_
            "$2a$08$OU8e9dncXyz8UP5Z.gWP8Os1IK89pspCS4FPzj8hBjgCWmjbLVcO2"); // âË_3(íù*
    }

}
