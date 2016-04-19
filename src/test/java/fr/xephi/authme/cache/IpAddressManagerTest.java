package fr.xephi.authme.cache;

import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.properties.HooksSettings;
import org.bukkit.entity.Player;
import org.junit.Test;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link IpAddressManager}.
 */
public class IpAddressManagerTest {

    @Test
    public void shouldRetrieveFromCache() {
        // given
        IpAddressManager ipAddressManager = new IpAddressManager(mockSettings(true));
        ipAddressManager.addCache("Test", "my test IP");

        // when
        String result = ipAddressManager.getPlayerIp(mockPlayer("test", "123.123.123.123"));

        // then
        assertThat(result, equalTo("my test IP"));
    }

    @Test
    public void shouldReturnPlainIp() {
        // given
        IpAddressManager ipAddressManager = new IpAddressManager(mockSettings(false));

        // when
        String result = ipAddressManager.getPlayerIp(mockPlayer("bobby", "8.8.8.8"));

        // then
        assertThat(result, equalTo("8.8.8.8"));
    }



    private static NewSetting mockSettings(boolean useVeryGames) {
        NewSetting settings = mock(NewSetting.class);
        given(settings.getProperty(HooksSettings.ENABLE_VERYGAMES_IP_CHECK)).willReturn(useVeryGames);
        return settings;
    }

    private static Player mockPlayer(String name, String ip) {
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        InetAddress inetAddress = mock(InetAddress.class);
        given(inetAddress.getHostAddress()).willReturn(ip);
        InetSocketAddress inetSocketAddress = new InetSocketAddress(inetAddress, 8093);
        given(player.getAddress()).willReturn(inetSocketAddress);
        return player;
    }

}
