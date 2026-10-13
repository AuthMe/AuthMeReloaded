package fr.xephi.authme.data;

import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;

/**
 * Test for {@link ProxySessionManager}.
 */
class ProxySessionManagerTest {

    @Test
    void shouldConsumeLoginRequestOnlyOnce() {
        // given
        ProxySessionManager manager = new ProxySessionManager();
        UUID premiumUuid = UUID.randomUUID();
        manager.processProxySessionMessage("Bobby", premiumUuid);

        // when
        ProxySessionManager.ProxyLoginRequest first = manager.consumeLoginRequest("BOBBY");
        ProxySessionManager.ProxyLoginRequest second = manager.consumeLoginRequest("bobby");

        // then
        assertThat(first.playerName(), equalTo("bobby"));
        assertThat(first.verifiedPremiumUuid(), equalTo(premiumUuid));
        assertThat(second, nullValue());
    }

    @Test
    void shouldRemoveLoginRequest() {
        // given
        ProxySessionManager manager = new ProxySessionManager();
        manager.processProxySessionMessage("Bobby");

        // when
        manager.removeLoginRequest("bobby");

        // then
        assertThat(manager.shouldResumeSession("Bobby"), equalTo(false));
        assertThat(manager.consumeLoginRequest("Bobby"), nullValue());
    }
}
