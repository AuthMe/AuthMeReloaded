package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.service.BukkitService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.hamcrest.Matchers.both;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
 * Test for {@link GetIpCommand}.
 */
@ExtendWith(MockitoExtension.class)
class GetIpCommandTest {

    @InjectMocks
    private GetIpCommand command;

    @Mock
    private BukkitService bukkitService;

    @Mock
    private DataSource dataSource;


    @Test
    void shouldGetIpOfPlayer() {
        // given
        given(bukkitService.getPlayerExact(anyString())).willReturn(null);
        given(dataSource.getAuth(anyString())).willReturn(null);
        CommandSender sender = mock(CommandSender.class);
        String name = "Testt";

        // when
        command.executeCommand(sender, Collections.singletonList(name));

        // then
        verify(bukkitService).getPlayerExact(name);
        verify(dataSource).getAuth(name);
        verify(sender, only()).sendMessage(argThat(containsString("not registered")));
    }

    @Test
    void shouldReturnIpAddressOfPlayer() {
        // given
        String playerName = "charlie";
        String ip = "123.34.56.88";
        Player player = mockPlayer(playerName, ip);
        given(bukkitService.getPlayerExact(playerName)).willReturn(player);
        PlayerAuth auth = PlayerAuth.builder().name("t").lastIp("44.33.22.11").registrationIp("77.11.44.88").build();
        given(dataSource.getAuth(playerName)).willReturn(auth);
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.singletonList(playerName));

        // then
        verify(bukkitService).getPlayerExact(playerName);
        verify(dataSource).getAuth(playerName);
        verify(sender).sendMessage(argThat(both(containsString(playerName)).and(containsString(ip))));
        verify(sender).sendMessage(argThat(both(containsString("44.33.22.11")).and(containsString("77.11.44.88"))));
    }

    @Test
    void shouldHandleUnregisteredOnlinePlayer() {
        // given
        String playerName = "Test";
        String ip = "44.111.22.33";
        Player player = mockPlayer(playerName, ip);
        given(bukkitService.getPlayerExact(playerName)).willReturn(player);
        given(dataSource.getAuth(anyString())).willReturn(null);
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.singletonList(playerName));

        // then
        verify(bukkitService).getPlayerExact(playerName);
        verify(dataSource).getAuth(playerName);
        verify(sender).sendMessage(argThat(both(containsString(playerName)).and(containsString(ip))));
        verify(sender).sendMessage(argThat(containsString("not registered")));
    }

    private static Player mockPlayer(String name, String ip) {
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        TestHelper.mockIpAddressToPlayer(player, ip);
        return player;
    }
}
