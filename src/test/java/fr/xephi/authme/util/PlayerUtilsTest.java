package fr.xephi.authme.util;

import fr.xephi.authme.TestHelper;
import org.bukkit.entity.Player;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link PlayerUtils}.
 */
public class PlayerUtilsTest {

    @BeforeClass
    public static void setAuthmeInstance() {
        TestHelper.setupLogger();
    }

    @Test
    public void shouldGetPlayerIp() {
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
    public void shouldCheckIfIsNpc() {
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
