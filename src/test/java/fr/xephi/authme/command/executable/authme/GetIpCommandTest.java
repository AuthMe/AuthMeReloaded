package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.service.BukkitService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
 * Test for {@link GetIpCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class GetIpCommandTest {

    @InjectMocks
    private GetIpCommand command;

    @Mock
    private BukkitService bukkitService;


    @Test
    public void shouldGetIpOfPlayer() {
        // given
        given(bukkitService.getPlayerExact(anyString())).willReturn(null);
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.singletonList("Testt"));

        // then
        verify(bukkitService).getPlayerExact("Testt");
        verify(sender).sendMessage(argThat(containsString("not online")));
    }

    @Test
    public void shouldReturnIpAddressOfPlayer() {
        // given
        String playerName = "charlie";
        String ip = "123.34.56.88";
        Player player = mockPlayer(playerName, ip);
        given(bukkitService.getPlayerExact(playerName)).willReturn(player);
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.singletonList(playerName));

        // then
        verify(bukkitService).getPlayerExact(playerName);
        verify(sender).sendMessage(argThat(allOf(containsString(playerName), containsString(ip))));
    }

    private static Player mockPlayer(String name, String ip) {
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        TestHelper.mockPlayerIp(player, ip);
        return player;
    }
}
