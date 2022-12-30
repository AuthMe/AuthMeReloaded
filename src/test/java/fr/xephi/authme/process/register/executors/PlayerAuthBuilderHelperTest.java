package fr.xephi.authme.process.register.executors;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.security.crypts.HashedPassword;
import org.bukkit.entity.Player;
import org.junit.Test;

import static fr.xephi.authme.AuthMeMatchers.equalToHash;
import static fr.xephi.authme.AuthMeMatchers.hasAuthBasicData;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.lessThan;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link PlayerAuthBuilderHelper}.
 */
public class PlayerAuthBuilderHelperTest {

    @Test
    public void shouldConstructPlayerAuth() {
        // given
        Player player = mock(Player.class);
        given(player.getName()).willReturn("Noah");
        String ip = "192.168.34.47";
        TestHelper.mockIpAddressToPlayer(player, ip);
        HashedPassword hashedPassword = new HashedPassword("myHash0001");
        String email = "test@example.org";

        // when
        PlayerAuth auth = PlayerAuthBuilderHelper.createPlayerAuth(player, hashedPassword, email);

        // then
        assertThat(auth, hasAuthBasicData("noah", "Noah", email, null));
        assertThat(auth.getRegistrationIp(), equalTo("192.168.34.47"));
        assertThat(Math.abs(auth.getRegistrationDate() - System.currentTimeMillis()), lessThan(1000L));
        assertThat(auth.getPassword(), equalToHash("myHash0001"));
    }
}
