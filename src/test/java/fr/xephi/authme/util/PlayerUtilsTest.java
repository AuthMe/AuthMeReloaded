package fr.xephi.authme.util;

import fr.xephi.authme.TestHelper;
import org.bukkit.entity.Player;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.UUID;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
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
        TestHelper.mockPlayerIp(player, ip);

        // when
        String result = PlayerUtils.getPlayerIp(player);

        // then
        assertThat(result, equalTo(ip));
    }

    @Test
    public void shouldGetUuid() {
        // given
        UUID uuid = UUID.randomUUID();
        Player player = mock(Player.class);
        given(player.getUniqueId()).willReturn(uuid);

        // when
        String result = PlayerUtils.getUuidOrName(player);

        // then
        assertThat(result, equalTo(uuid.toString()));
    }

    @Test
    public void shouldFallbackToName() {
        // given
        Player player = mock(Player.class);
        doThrow(NoSuchMethodError.class).when(player).getUniqueId();
        String name = "Bobby12";
        given(player.getName()).willReturn(name);

        // when
        String result = PlayerUtils.getUuidOrName(player);

        // then
        assertThat(result, equalTo(name));
    }

    @Test
    public void shouldHaveHiddenConstructor() {
        // given / when / then
        TestHelper.validateHasOnlyPrivateEmptyConstructor(PlayerUtils.class);
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
