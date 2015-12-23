package fr.xephi.authme.command.executable.captcha;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.util.WrapperMock;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.ArrayList;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link CaptchaCommand}.
 */
public class CaptchaCommandTest {

    private WrapperMock wrapperMock;

    @Before
    public void setUpWrapperMock() {
        wrapperMock = WrapperMock.createInstance();
        Settings.useCaptcha = true;
    }

    @Test
    public void shouldRejectNonPlayerSender() {
        // given
        CommandSender sender = Mockito.mock(BlockCommandSender.class);
        ExecutableCommand command = new CaptchaCommand();

        // when
        command.executeCommand(sender, new ArrayList<String>(), mock(CommandService.class));

        // then
        assertThat(wrapperMock.wasMockCalled(AuthMe.class), equalTo(false));
        assertThat(wrapperMock.wasMockCalled(Messages.class), equalTo(false));
    }

    @Test
    @Ignore
    public void shouldRejectIfCaptchaIsNotUsed() {
        // given
        Player player = mockPlayerWithName("testplayer");
        ExecutableCommand command = new CaptchaCommand();

        // when
        command.executeCommand(player, new ArrayList<String>(),  mock(CommandService.class));

        // then
        verify(wrapperMock.getMessages()).send(player, MessageKey.USAGE_LOGIN);
    }

    private static Player mockPlayerWithName(String name) {
        Player player = Mockito.mock(Player.class);
        when(player.getName()).thenReturn(name);
        return player;
    }
}
