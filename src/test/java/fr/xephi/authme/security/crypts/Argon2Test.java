package fr.xephi.authme.security.crypts;

import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Test for {@link Argon2}.
 */
class Argon2Test extends AbstractEncryptionMethodTest {

    private static final boolean IS_LIBRARY_LOADED = Argon2.isLibraryLoaded();

    Argon2Test() {
        super(new Argon2(),
            "$argon2i$v=19$m=65536,t=2,p=1$dOP8NiXsPTcMgzI4Z8Rbew$ShdowtoTEWTL5UTFz1UgQOigb9JOlm4ZxWPA6WbIeUw",  // password
            "$argon2i$v=19$m=65536,t=2,p=1$amZHbPfgc5peKd/4w1AI1g$Q2PUiOVw47TACijP57U0xf7QfiZ00HV4eFzMDA6yKRE",  // PassWord1
            "$argon2i$v=19$m=65536,t=2,p=1$58v7dWNn9/bpD00QLzSebw$7cMC7p0qceE3Mgf2yQp4X7c+UkO9oyJwQ7S6XTBubNs",  // &^%te$t?Pw@_
            "$argon2i$v=19$m=65536,t=2,p=1$93OSU71DgBOzpmhti7+6rQ$sSSI6QQQdoG9DlGwLjYz576kTek89nwr9CyNpy6bsL0"); // âË_3(íù*

        assumeTrue(IS_LIBRARY_LOADED, "Argon2 library is not loaded - skipping test");
    }

    @Override
    protected boolean testHashEqualityForSameSalt() {
        // Argon2 has a salt but it is handled internally
        return false;
    }
}
