package fr.xephi.authme.process.register.executors;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.security.crypts.HashedPassword;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.Ignore;
import org.junit.Test;

import static fr.xephi.authme.AuthMeMatchers.hasAuthBasicData;
import static fr.xephi.authme.AuthMeMatchers.hasAuthLocation;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link PlayerAuthBuilderHelper}.
 */
public class PlayerAuthBuilderHelperTest {

    @Test
    @Ignore // TODO #792: last IP should be NULL + check registration info
    public void shouldConstructPlayerAuth() {
        // given
        Player player = mock(Player.class);
        given(player.getName()).willReturn("Noah");
        String ip = "192.168.34.47";
        TestHelper.mockPlayerIp(player, ip);
        World world = mock(World.class);
        given(world.getName()).willReturn("worldName");
        Location location = new Location(world, 123, 80, -99, 2.45f, 7.61f);
        given(player.getLocation()).willReturn(location);
        HashedPassword hashedPassword = new HashedPassword("myHash0001");
        String email = "test@example.org";

        // when
        PlayerAuth auth = PlayerAuthBuilderHelper.createPlayerAuth(player, hashedPassword, email);

        // then
        assertThat(auth, hasAuthBasicData("noah", "Noah", email, ip));
        assertThat(auth, hasAuthLocation(123, 80, -99, "worldName", 2.45f, 7.61f));
    }

    @Test
    public void shouldHaveHiddenConstructor() {
        TestHelper.validateHasOnlyPrivateEmptyConstructor(PlayerAuthBuilderHelper.class);
    }

}
