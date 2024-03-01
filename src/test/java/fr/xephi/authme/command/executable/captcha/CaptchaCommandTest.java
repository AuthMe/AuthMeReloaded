package fr.xephi.authme.command.executable.captcha;

import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.data.captcha.LoginCaptchaManager;
import fr.xephi.authme.data.captcha.RegistrationCaptchaManager;
import fr.xephi.authme.data.limbo.LimboService;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.service.CommonService;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Test for {@link CaptchaCommand}.
 */
@ExtendWith(MockitoExtension.class)
class CaptchaCommandTest {

    @InjectMocks
    private CaptchaCommand command;

    @Mock
    private LoginCaptchaManager loginCaptchaManager;

    @Mock
    private RegistrationCaptchaManager registrationCaptchaManager;

    @Mock
    private PlayerCache playerCache;

    @Mock
    private CommonService commonService;

    @Mock
    private LimboService limboService;

    @Mock
    private DataSource dataSource;

    @Test
    void shouldDetectIfPlayerIsLoggedIn() {
        // given
        String name = "creeper011";
        Player player = mockPlayerWithName(name);
        given(playerCache.isAuthenticated(name)).willReturn(true);

        // when
        command.executeCommand(player, Collections.singletonList("123"));

        // then
        verify(commonService).send(player, MessageKey.ALREADY_LOGGED_IN_ERROR);
    }

    @Test
    void shouldShowLoginUsageIfCaptchaIsNotRequired() {
        // given
        String name = "bobby";
        Player player = mockPlayerWithName(name);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        given(loginCaptchaManager.isCaptchaRequired(name)).willReturn(false);
        given(dataSource.isAuthAvailable(name)).willReturn(true);

        // when
        command.executeCommand(player, Collections.singletonList("1234"));

        // then
        verify(commonService).send(player, MessageKey.USAGE_LOGIN);
        verify(loginCaptchaManager).isCaptchaRequired(name);
        verifyNoMoreInteractions(loginCaptchaManager, registrationCaptchaManager);
    }

    @Test
    void shouldHandleCorrectCaptchaInput() {
        // given
        String name = "smith";
        Player player = mockPlayerWithName(name);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        given(loginCaptchaManager.isCaptchaRequired(name)).willReturn(true);
        String captchaCode = "3991";
        given(loginCaptchaManager.checkCode(player, captchaCode)).willReturn(true);

        // when
        command.executeCommand(player, Collections.singletonList(captchaCode));

        // then
        verify(loginCaptchaManager).isCaptchaRequired(name);
        verify(loginCaptchaManager).checkCode(player, captchaCode);
        verifyNoMoreInteractions(loginCaptchaManager);
        verify(commonService).send(player, MessageKey.CAPTCHA_SUCCESS);
        verify(commonService).send(player, MessageKey.LOGIN_MESSAGE);
        verify(limboService).unmuteMessageTask(player);
        verifyNoMoreInteractions(commonService);
    }

    @Test
    void shouldHandleWrongCaptchaInput() {
        // given
        String name = "smith";
        Player player = mockPlayerWithName(name);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        given(loginCaptchaManager.isCaptchaRequired(name)).willReturn(true);
        String captchaCode = "2468";
        given(loginCaptchaManager.checkCode(player, captchaCode)).willReturn(false);
        String newCode = "1337";
        given(loginCaptchaManager.getCaptchaCodeOrGenerateNew(name)).willReturn(newCode);

        // when
        command.executeCommand(player, Collections.singletonList(captchaCode));

        // then
        verify(loginCaptchaManager).isCaptchaRequired(name);
        verify(loginCaptchaManager).checkCode(player, captchaCode);
        verify(loginCaptchaManager).getCaptchaCodeOrGenerateNew(name);
        verifyNoMoreInteractions(loginCaptchaManager);
        verify(commonService).send(player, MessageKey.CAPTCHA_WRONG_ERROR, newCode);
        verifyNoMoreInteractions(commonService);
    }

    @Test
    void shouldVerifyWithRegisterCaptchaManager() {
        // given
        String name = "john";
        Player player = mockPlayerWithName(name);
        given(loginCaptchaManager.isCaptchaRequired(name)).willReturn(false);
        given(registrationCaptchaManager.isCaptchaRequired(name)).willReturn(true);
        String captchaCode = "A89Y3";
        given(registrationCaptchaManager.checkCode(player, captchaCode)).willReturn(true);

        // when
        command.executeCommand(player, Collections.singletonList(captchaCode));

        // then
        verify(registrationCaptchaManager).checkCode(player, captchaCode);
        verify(loginCaptchaManager, only()).isCaptchaRequired(name);
        verify(commonService).send(player, MessageKey.REGISTER_CAPTCHA_SUCCESS);
        verify(commonService).send(player, MessageKey.REGISTER_MESSAGE);
    }

    @Test
    void shouldHandleFailedRegisterCaptcha() {
        // given
        String name = "asfd";
        Player player = mockPlayerWithName(name);
        given(registrationCaptchaManager.isCaptchaRequired(name)).willReturn(true);
        String captchaCode = "SFL3";
        given(registrationCaptchaManager.checkCode(player, captchaCode)).willReturn(false);
        given(registrationCaptchaManager.getCaptchaCodeOrGenerateNew(name)).willReturn("new code");

        // when
        command.executeCommand(player, Collections.singletonList(captchaCode));

        // then
        verify(registrationCaptchaManager).checkCode(player, captchaCode);
        verify(registrationCaptchaManager).getCaptchaCodeOrGenerateNew(name);
        verify(commonService).send(player, MessageKey.CAPTCHA_WRONG_ERROR, "new code");
    }

    @Test
    void shouldShowRegisterUsageWhenRegistrationCaptchaIsSolved() {
        // given
        String name = "alice";
        Player player = mockPlayerWithName(name);
        given(registrationCaptchaManager.isCaptchaRequired(name)).willReturn(false);

        // when
        command.executeCommand(player, Collections.singletonList("test"));

        // then
        verify(registrationCaptchaManager, only()).isCaptchaRequired(name);
        verify(commonService).send(player, MessageKey.USAGE_REGISTER);
    }

    private static Player mockPlayerWithName(String name) {
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        return player;
    }
}
