package fr.xephi.authme.security.crypts;

/**
 * Test for {@link Sha256}.
 */
class Sha256Test extends AbstractEncryptionMethodTest {

    Sha256Test() {
        super(new Sha256(),
            "$SHA$11aa0706173d7272$dbba96681c2ae4e0bfdf226d70fbbc5e4ee3d8071faa613bc533fe8a64817d10", // password
            "$SHA$3c72a18a29b08d40$8e50a7a4f69a80f4893dc921eac84bd74b3f9ebfa22908302c9965eac3aa45e5", // PassWord1
            "$SHA$584cea1cfab90030$adc006330e73d81e463fe02a4fe9b17bdbbcc05955bff72fb27cf2089f0b3859", // &^%te$t?Pw@_
            "$SHA$0b503d90dd9949d4$ba70c330242e0daa9a154ec9f4cce7f01dd05aff489d37c653e36a507c74d84f"  // âË_3(íù*
        );
    }

}
