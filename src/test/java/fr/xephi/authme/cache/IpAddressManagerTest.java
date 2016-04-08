package fr.xephi.authme.cache;

import fr.xephi.authme.settings.NewSetting;
import fr.xephi.authme.settings.properties.HooksSettings;
import org.bukkit.entity.Player;
import java.net.InetAddress;
import java.net.InetSocketAddress;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link IpAddressManager}.
 */
public class IpAddressManagerTest {

    private static NewSetting mockSettings(boolean useVeryGames, boolean useBungee) {
        NewSetting settings = mock(NewSetting.class);
        given(settings.getProperty(HooksSettings.ENABLE_VERYGAMES_IP_CHECK)).willReturn(useVeryGames);
        given(settings.getProperty(HooksSettings.BUNGEECORD)).willReturn(useBungee);
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
