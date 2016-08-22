package fr.xephi.authme.util;

import fr.xephi.authme.TestHelper;
import org.bukkit.entity.Player;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.UUID;
import java.util.regex.Pattern;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link Utils}.
 */
public class UtilsTest {

    @BeforeClass
    public static void setAuthmeInstance() {
        TestHelper.setupLogger();
    }

    @Test
    public void shouldCompilePattern() {
        // given
        String pattern = "gr(a|e)ys?";

        // when
        Pattern result = Utils.safePatternCompile(pattern);

        // then
        assertThat(result.toString(), equalTo(pattern));
    }

    @Test
    public void shouldDefaultToAllAllowedPattern() {
        // given
        String invalidPattern = "gr(a|eys?"; // missing closing ')'

        // when
        Pattern result = Utils.safePatternCompile(invalidPattern);

        // then
        assertThat(result.toString(), equalTo(".*?"));
    }

    @Test
    public void shouldGetPlayerIp() {
        // given
        Player player = mock(Player.class);
        String ip = "124.86.248.62";
        TestHelper.mockPlayerIp(player, ip);

        // when
        String result = Utils.getPlayerIp(player);

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
        String result = Utils.getUUIDorName(player);

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
        String result = Utils.getUUIDorName(player);

        // then
        assertThat(result, equalTo(name));
    }

    @Test
    public void shouldHavePrivateConstructorOnly() {
        // given / when / then
        TestHelper.validateHasOnlyPrivateEmptyConstructor(Utils.class);
    }

    @Test
    public void shouldCheckIfClassIsLoaded() {
        // given / when / then
        assertThat(Utils.isClassLoaded("org.bukkit.event.player.PlayerFishEvent"), equalTo(true));
        assertThat(Utils.isClassLoaded("com.someclass.doesnot.exist"), equalTo(false));
    }
}
