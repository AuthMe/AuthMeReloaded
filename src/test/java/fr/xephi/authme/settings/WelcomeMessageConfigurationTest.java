package fr.xephi.authme.settings;

import ch.jalu.injector.testing.BeforeInjecting;
import ch.jalu.injector.testing.DelayedInjectionRunner;
import ch.jalu.injector.testing.InjectDelayed;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.GeoIpService;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Test for {@link WelcomeMessageConfiguration}.
 */
@RunWith(DelayedInjectionRunner.class)
public class WelcomeMessageConfigurationTest {

    @InjectDelayed
    private WelcomeMessageConfiguration welcomeMessageConfiguration;
    @Mock
    private Server server;
    @Mock
    private BukkitService bukkitService;
    @Mock
    private GeoIpService geoIpService;
    @Mock
    private PlayerCache playerCache;
    @DataFolder
    private File testPluginFolder;

    private File welcomeFile;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @BeforeInjecting
    public void createPluginFolder() throws IOException {
        testPluginFolder = temporaryFolder.newFolder();
        welcomeFile = new File(testPluginFolder, "welcome.txt");
        welcomeFile.createNewFile();
    }

    @Test
    public void shouldLoadWelcomeMessage() throws IOException {
        // given
        String welcomeMessage = "This is my welcome message for testing\nBye!";
        Files.write(welcomeFile.toPath(), welcomeMessage.getBytes());
        Player player = mock(Player.class);
        welcomeMessageConfiguration.reload();

        // when
        List<String> result = welcomeMessageConfiguration.getWelcomeMessage(player);

        // then
        assertThat(result, hasSize(2));
        assertThat(result, contains(welcomeMessage.split("\\n")));
        verifyZeroInteractions(player, playerCache, geoIpService, bukkitService, server);
    }

    @Test
    public void shouldReplaceNameAndIpAndCountry() throws IOException {
        // given
        String welcomeMessage = "Hello {PLAYER}, your IP is {IP}\nYour country is {COUNTRY}.\nWelcome to {SERVER}!";
        Files.write(welcomeFile.toPath(), welcomeMessage.getBytes());
        welcomeMessageConfiguration.reload();

        Player player = mock(Player.class);
        given(player.getName()).willReturn("Bobby");
        TestHelper.mockPlayerIp(player, "123.45.66.77");
        given(geoIpService.getCountryName("123.45.66.77")).willReturn("Syldavia");
        given(server.getServerName()).willReturn("CrazyServer");

        // when
        List<String> result = welcomeMessageConfiguration.getWelcomeMessage(player);

        // then
        assertThat(result, hasSize(3));
        assertThat(result.get(0), equalTo("Hello Bobby, your IP is 123.45.66.77"));
        assertThat(result.get(1), equalTo("Your country is Syldavia."));
        assertThat(result.get(2), equalTo("Welcome to CrazyServer!"));
        verify(server, only()).getServerName();
        verifyZeroInteractions(playerCache);
    }
}
