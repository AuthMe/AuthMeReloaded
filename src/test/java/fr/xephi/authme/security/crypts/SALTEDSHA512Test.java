package fr.xephi.authme.security.crypts;

import org.junit.Ignore;

/**
 * Test for {@link SALTEDSHA512}.
 */
@Ignore
// TODO ljacqu 20151220: Currently cannot test because of closely coupled database call inside of class
public class SALTEDSHA512Test extends AbstractEncryptionMethodTest {

    public SALTEDSHA512Test() {
        super(new SALTEDSHA512(),
            "c8efe95e1ab02d9a0e7c7d11d4ac3cc068a8405b5810aac3a1b8b01927ab059563438131dc995156739daf74db40ffdc79b78f6aec9b2a468fe106b88c66c204",  // password
            "74c61af1bcbb3293cdc0959c7323d50be28c167eddc7a1b7eb029e38263c2cfb6eb090f41370a65249752aa316fa851091c2bd8420302e87d383529beea735b4",  // PassWord1
            "08eefcca4a17876441ebe61a02e8bc62cab7502dd87f8ec3b7f82edb2adace791b8dad31e74c5513cf99be502b732f5c5efffb239f4590d5c600d066a7037908",  // &^%te$t?Pw@_
            "a122490c4c7c18ad665b5ac9617c948741468a787a2ba42c6fd2530ea1d7874681b8575ee9a8907c42ff65dac69e4ada2852789759c17d51865ca915b259a65a"); // âË_3(íù*
    }

}
