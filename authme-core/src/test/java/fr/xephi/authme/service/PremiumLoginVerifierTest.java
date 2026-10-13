package fr.xephi.authme.service;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link PremiumLoginVerifier}.
 */
class PremiumLoginVerifierTest {

    private final PremiumLoginVerifier verifier =
        new PremiumLoginVerifier(mock(MojangApiService.class), mock(BukkitService.class));

    @Test
    void shouldConsumeVerifiedUuidOnlyOnce() {
        // given
        UUID mojangUuid = UUID.randomUUID();
        verifier.storeVerified("Notch", mojangUuid);

        // when
        UUID peeked = verifier.getVerifiedUuid("notch");
        UUID consumed = verifier.consumeVerifiedUuid("NOTCH");

        // then
        assertThat(peeked, equalTo(mojangUuid));
        assertThat(consumed, equalTo(mojangUuid));
        assertThat(verifier.getVerifiedUuid("Notch"), nullValue());
        assertThat(verifier.consumeVerifiedUuid("Notch"), nullValue());
    }

    @Test
    void shouldInvalidateEarlierVerificationWhenNewConnectionStartsVerifying() {
        // given
        verifier.storeVerified("Notch", UUID.randomUUID());

        // when
        verifier.startVerification("127.0.0.1:50000", "notch", null);

        // then
        assertThat(verifier.getVerifiedUuid("Notch"), nullValue());
    }
}
