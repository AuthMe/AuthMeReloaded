package fr.xephi.authme.data.auth;

import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.sameInstance;

/**
 * Test for {@link PlayerCache}.
 */
class PlayerCacheTest {

    @Test
    void shouldNotAuthenticatePlayerWhenRefreshingAbsentEntry() {
        // given
        PlayerCache playerCache = new PlayerCache();
        PlayerAuth auth = PlayerAuth.builder().name("bobby").build();

        // when
        playerCache.updatePlayerIfPresent(auth);

        // then
        assertThat(playerCache.isAuthenticated("Bobby"), equalTo(false));
    }

    @Test
    void shouldReplaceCachedEntryOfAuthenticatedPlayer() {
        // given
        PlayerCache playerCache = new PlayerCache();
        playerCache.updatePlayer(PlayerAuth.builder().name("bobby").build());
        PlayerAuth refreshedAuth = PlayerAuth.builder().name("bobby").email("bobby@example.org").build();

        // when
        playerCache.updatePlayerIfPresent(refreshedAuth);

        // then
        assertThat(playerCache.getAuth("Bobby"), sameInstance(refreshedAuth));
    }
}
