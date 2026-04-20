package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.task.purge.PurgeExecutor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Locale;

import static fr.xephi.authme.service.BukkitServiceTestHelper.setBukkitServiceToRunTaskAsynchronously;
import static fr.xephi.authme.service.BukkitServiceTestHelper.setBukkitServiceToRunTaskOptionallyAsync;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
 * Test for {@link PurgePlayerCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class PurgePlayerCommandTest {

    @InjectMocks
    private PurgePlayerCommand command;

    @Mock
    private BukkitService bukkitService;

    @Mock
    private PurgeExecutor purgeExecutor;

    @Mock
    private DataSource dataSource;

    @Test
    public void shouldNotExecutePurgeForRegisteredPlayer() {
        // given
        String name = "Bobby";
        given(dataSource.isAuthAvailable(name)).willReturn(true);
        CommandSender sender = mock(CommandSender.class);
        setBukkitServiceToRunTaskAsynchronously(bukkitService);

        // when
        command.executeCommand(sender, singletonList(name));

        // then
        verify(sender).sendMessage(argThat(containsString("This player is still registered")));
        verifyNoInteractions(purgeExecutor);
    }

    @Test
    public void shouldExecutePurge() {
        // given
        String name = "Frank";
        given(dataSource.isAuthAvailable(name)).willReturn(false);
        OfflinePlayer player = mock(OfflinePlayer.class);
        given(bukkitService.getOfflinePlayer(name)).willReturn(player);
        CommandSender sender = mock(CommandSender.class);
        setBukkitServiceToRunTaskAsynchronously(bukkitService);

        // when
        command.executeCommand(sender, singletonList(name));

        // then
        verify(dataSource).isAuthAvailable(name);
        verify(purgeExecutor).executePurge(singletonList(player), singletonList(name.toLowerCase(Locale.ROOT)));
    }

    @Test
    public void shouldExecutePurgeOfRegisteredPlayer() {
        // given
        String name = "GhiJKlmn7";
        OfflinePlayer player = mock(OfflinePlayer.class);
        given(bukkitService.getOfflinePlayer(name)).willReturn(player);
        CommandSender sender = mock(CommandSender.class);
        setBukkitServiceToRunTaskAsynchronously(bukkitService);

        // when
        command.executeCommand(sender, asList(name, "force"));

        // then
        verify(purgeExecutor).executePurge(singletonList(player), singletonList(name.toLowerCase(Locale.ROOT)));
    }
}
