package fr.xephi.authme.service;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PluginSettings;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collection;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link BukkitService}.
 */
@RunWith(MockitoJUnitRunner.class)
public class BukkitServiceTest {

    private BukkitService bukkitService;

    @Mock
    private AuthMe authMe;
    @Mock
    private Settings settings;
    @Mock
    private Server server;

    @Before
    public void constructBukkitService() {
        ReflectionTestUtils.setField(Bukkit.class, null, "server", server);
        given(settings.getProperty(PluginSettings.USE_ASYNC_TASKS)).willReturn(true);
        bukkitService = new BukkitService(authMe, settings);
    }

    /**
     * Checks that {@link BukkitService#getOnlinePlayersIsCollection} is initialized to {@code true} on startup;
     * the test scope is configured with a Bukkit implementation that returns a Collection and not an array.
     */
    @Test
    public void shouldHavePlayerListAsCollectionMethod() {
        // given / when
        boolean doesMethodReturnCollection = ReflectionTestUtils
            .getFieldValue(BukkitService.class, bukkitService, "getOnlinePlayersIsCollection");

        // then
        assertThat(doesMethodReturnCollection, equalTo(true));
    }

    @Test
    public void shouldRetrieveListOfOnlinePlayersFromReflectedMethod() {
        // given
        ReflectionTestUtils.setField(BukkitService.class, bukkitService, "getOnlinePlayersIsCollection", false);
        ReflectionTestUtils.setField(BukkitService.class, bukkitService, "getOnlinePlayers",
            ReflectionTestUtils.getMethod(BukkitServiceTest.class, "onlinePlayersImpl"));

        // when
        Collection<? extends Player> players = bukkitService.getOnlinePlayers();

        // then
        assertThat(players, hasSize(2));
    }

    @Test
    public void shouldDispatchCommand() {
        // given
        CommandSender sender = mock(CommandSender.class);
        String command = "help test abc";

        // when
        bukkitService.dispatchCommand(sender, command);

        // then
        verify(server).dispatchCommand(sender, command);
    }

    @Test
    public void shouldDispatchConsoleCommand() {
        // given
        ConsoleCommandSender consoleSender = mock(ConsoleCommandSender.class);
        given(server.getConsoleSender()).willReturn(consoleSender);
        String command = "my command";

        // when
        bukkitService.dispatchConsoleCommand(command);

        // then
        verify(server).dispatchCommand(consoleSender, command);
    }

    // Note: This method is used through reflections
    public static Player[] onlinePlayersImpl() {
        return new Player[]{
            mock(Player.class), mock(Player.class)
        };
    }

}
