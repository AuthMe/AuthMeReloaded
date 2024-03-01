package fr.xephi.authme.util;

import fr.xephi.authme.TestHelper;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link PlayerUtils}.
 */
class PlayerUtilsTest {

    @BeforeAll
    static void setUpLogger() {
        TestHelper.setupLogger();
    }

    @Test
    void shouldGetPlayerIp() {
        // given
        Player player = mock(Player.class);
        String ip = "124.86.248.62";
        TestHelper.mockIpAddressToPlayer(player, ip);

        // when
        String result = PlayerUtils.getPlayerIp(player);

        // then
        assertThat(result, equalTo(ip));
    }

    @Test
    void shouldCheckIfIsNpc() {
        // given
        Player player1 = mock(Player.class);
        given(player1.hasMetadata("NPC")).willReturn(false);
        Player player2 = mock(Player.class);
        given(player2.hasMetadata("NPC")).willReturn(true);

        // when
        boolean result1 = PlayerUtils.isNpc(player1);
        boolean result2 = PlayerUtils.isNpc(player2);

        // then
        assertThat(result1, equalTo(false));
        assertThat(result2, equalTo(true));
    }
}
