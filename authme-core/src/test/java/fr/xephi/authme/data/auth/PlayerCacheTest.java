package fr.xephi.authme.data.auth;

import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

class PlayerCacheTest {

    @Test
    void shouldKeepAuthenticationWithTheCurrentPlayerConnection() {
        Player first = mock(Player.class);
        Player second = mock(Player.class);
        given(first.getName()).willReturn("Example");
        given(second.getName()).willReturn("example");
        PlayerAuth firstAuth = PlayerAuth.builder().name("Example").build();
        PlayerAuth secondAuth = PlayerAuth.builder().name("Example").build();
        PlayerCache cache = new PlayerCache();

        cache.updatePlayer(first, firstAuth);
        cache.updatePlayer(second, secondAuth);
        cache.removePlayer(first);

        assertFalse(cache.isAuthenticated(first));
        assertTrue(cache.isAuthenticated(second));
        assertSame(secondAuth, cache.getAuth(second));
        assertSame(secondAuth, cache.getAuth("Example"));
    }
}
