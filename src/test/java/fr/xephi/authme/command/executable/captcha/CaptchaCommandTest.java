package fr.xephi.authme.command.executable.captcha;

import fr.xephi.authme.data.CaptchaManager;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.message.MessageKey;
import org.bukkit.entity.Player;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Test for {@link CaptchaCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class CaptchaCommandTest {

    @InjectMocks
    private CaptchaCommand command;

    @Mock
    private CaptchaManager captchaManager;

    @Mock
    private PlayerCache playerCache;

    @Mock
    private CommandService commandService;

    @Test
    public void shouldDetectIfPlayerIsLoggedIn() {
        // given
        String name = "creeper011";
        Player player = mockPlayerWithName(name);
        given(playerCache.isAuthenticated(name)).willReturn(true);

        // when
        command.executeCommand(player, Collections.singletonList("123"));

        // then
        verify(commandService).send(player, MessageKey.ALREADY_LOGGED_IN_ERROR);
    }

    @Test
    public void shouldShowLoginUsageIfCaptchaIsNotRequired() {
        // given
        String name = "bobby";
        Player player = mockPlayerWithName(name);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        given(captchaManager.isCaptchaRequired(name)).willReturn(false);

        // when
        command.executeCommand(player, Collections.singletonList("1234"));

        // then
        verify(commandService).send(player, MessageKey.USAGE_LOGIN);
        verify(captchaManager).isCaptchaRequired(name);
        verifyNoMoreInteractions(captchaManager);
    }

    @Test
    public void shouldHandleCorrectCaptchaInput() {
        // given
        String name = "smith";
        Player player = mockPlayerWithName(name);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        given(captchaManager.isCaptchaRequired(name)).willReturn(true);
        String captchaCode = "3991";
        given(captchaManager.checkCode(name, captchaCode)).willReturn(true);

        // when
        command.executeCommand(player, Collections.singletonList(captchaCode));

        // then
        verify(captchaManager).isCaptchaRequired(name);
        verify(captchaManager).checkCode(name, captchaCode);
        verifyNoMoreInteractions(captchaManager);
        verify(commandService).send(player, MessageKey.CAPTCHA_SUCCESS);
        verify(commandService).send(player, MessageKey.LOGIN_MESSAGE);
        verifyNoMoreInteractions(commandService);
    }

    @Test
    public void shouldHandleWrongCaptchaInput() {
        // given
        String name = "smith";
        Player player = mockPlayerWithName(name);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        given(captchaManager.isCaptchaRequired(name)).willReturn(true);
        String captchaCode = "2468";
        given(captchaManager.checkCode(name, captchaCode)).willReturn(false);
        String newCode = "1337";
        given(captchaManager.generateCode(name)).willReturn(newCode);

        // when
        command.executeCommand(player, Collections.singletonList(captchaCode));

        // then
        verify(captchaManager).isCaptchaRequired(name);
        verify(captchaManager).checkCode(name, captchaCode);
        verify(captchaManager).generateCode(name);
        verifyNoMoreInteractions(captchaManager);
        verify(commandService).send(player, MessageKey.CAPTCHA_WRONG_ERROR, newCode);
        verifyNoMoreInteractions(commandService);
    }

    private static Player mockPlayerWithName(String name) {
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        return player;
    }
}
