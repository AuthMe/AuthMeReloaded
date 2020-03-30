package fr.xephi.authme.security.crypts;

/**
 * Test for {@link RoyalAuth}.
 */
class RoyalAuthTest extends AbstractEncryptionMethodTest {

    RoyalAuthTest() {
        super(new RoyalAuth(),
            "5d21ef9236896bc4ac508e524e2da8a0def555dac1cdfc7259d62900d1d3f553826210c369870673ae2cf1c41abcf4f92670d76af1db044d33559324f5c2a339",  // password
            "ecc685f4328bc54093c086ced66c5c11855e117ea22940632d5c0f55fff84d94bfdcc74e05f5d95bbdd052823a7057910748bc1c7a07af96b3e86731a4f11794",  // PassWord1
            "2c0b4674f7c2c266db13ae4382cbeee3083167a774f6e73793a6268a0b8b2c3c6b324a99596f4a7958e58c5311c77e25975a3b517ce17adfc4eaece821e3dd19",  // &^%te$t?Pw@_
            "f7bdc87552f7f7d19b68de5e6be6e48f4a6f277d9a5b00f470958062ab3a82b6c62ab8df86ef38636a632e10ef7bf8e3b5cafe8af53bb628919a84676ee0b4b7"); // âË_3(íù*
    }

}
