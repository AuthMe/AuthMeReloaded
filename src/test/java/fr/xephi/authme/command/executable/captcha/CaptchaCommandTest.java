package fr.xephi.authme.command.executable.captcha;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.settings.custom.SecuritySettings;
import fr.xephi.authme.util.WrapperMock;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;
import java.util.Collections;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link CaptchaCommand}.
 */
public class CaptchaCommandTest {

    private WrapperMock wrapperMock;
    private CommandService commandService;

    @Before
    public void setUpWrapperMock() {
        wrapperMock = WrapperMock.createInstance();
        commandService = mock(CommandService.class);
        given(commandService.getProperty(SecuritySettings.USE_CAPTCHA)).willReturn(true);
    }

    @Test
    public void shouldRejectNonPlayerSender() {
        // given
        CommandSender sender = Mockito.mock(BlockCommandSender.class);
        ExecutableCommand command = new CaptchaCommand();

        // when
        command.executeCommand(sender, new ArrayList<String>(), commandService);

        // then
        assertThat(wrapperMock.wasMockCalled(AuthMe.class), equalTo(false));
        assertThat(wrapperMock.wasMockCalled(Messages.class), equalTo(false));
    }

    @Test
    public void shouldRejectIfCaptchaIsNotUsed() {
        // given
        Player player = mockPlayerWithName("testplayer");
        ExecutableCommand command = new CaptchaCommand();
        given(commandService.getProperty(SecuritySettings.USE_CAPTCHA)).willReturn(false);

        // when
        command.executeCommand(player, Collections.singletonList("1234"), commandService);

        // then
        verify(commandService).send(player, MessageKey.USAGE_LOGIN);
    }

    private static Player mockPlayerWithName(String name) {
        Player player = Mockito.mock(Player.class);
        when(player.getName()).thenReturn(name);
        return player;
    }
}
