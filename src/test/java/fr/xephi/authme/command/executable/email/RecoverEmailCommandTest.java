package fr.xephi.authme.command.executable.email;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.mail.SendMailSSL;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.security.PasswordSecurity;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.settings.properties.EmailSettings;
import org.bukkit.entity.Player;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.runners.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.util.Collections;

import static fr.xephi.authme.AuthMeMatchers.stringWithLength;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Test for {@link RecoverEmailCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class RecoverEmailCommandTest {

    private static final String DEFAULT_EMAIL = "your@email.com";

    @InjectMocks
    private RecoverEmailCommand command;

    @Mock
    private PasswordSecurity passwordSecurity;

    @Mock
    private CommandService commandService;

    @Mock
    private DataSource dataSource;

    @Mock
    private PlayerCache playerCache;

    @Mock
    private SendMailSSL sendMailSsl;

    @BeforeClass
    public static void initLogger() {
        TestHelper.setupLogger();
    }

    @Test
    public void shouldHandleMissingMailProperties() {
        // given
        given(sendMailSsl.hasAllInformation()).willReturn(false);
        Player sender = mock(Player.class);

        // when
        command.executeCommand(sender, Collections.singletonList("some@email.tld"));

        // then
        verify(commandService).send(sender, MessageKey.INCOMPLETE_EMAIL_SETTINGS);
        verifyZeroInteractions(dataSource, passwordSecurity);
    }

    @Test
    public void shouldShowErrorForAuthenticatedUser() {
        // given
        String name = "Bobby";
        Player sender = mock(Player.class);
        given(sender.getName()).willReturn(name);
        given(sendMailSsl.hasAllInformation()).willReturn(true);
        given(playerCache.isAuthenticated(name)).willReturn(true);

        // when
        command.executeCommand(sender, Collections.singletonList("bobby@example.org"));

        // then
        verify(sendMailSsl).hasAllInformation();
        verifyZeroInteractions(dataSource);
        verify(commandService).send(sender, MessageKey.ALREADY_LOGGED_IN_ERROR);
    }

    @Test
    public void shouldShowRegisterMessageForUnregisteredPlayer() {
        // given
        String name = "Player123";
        Player sender = mock(Player.class);
        given(sender.getName()).willReturn(name);
        given(sendMailSsl.hasAllInformation()).willReturn(true);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        given(dataSource.getAuth(name)).willReturn(null);

        // when
        command.executeCommand(sender, Collections.singletonList("someone@example.com"));

        // then
        verify(sendMailSsl).hasAllInformation();
        verify(dataSource).getAuth(name);
        verifyNoMoreInteractions(dataSource);
        verify(commandService).send(sender, MessageKey.REGISTER_EMAIL_MESSAGE);
    }

    @Test
    public void shouldHandleDefaultEmail() {
        // given
        String name = "Tract0r";
        Player sender = mock(Player.class);
        given(sender.getName()).willReturn(name);
        given(sendMailSsl.hasAllInformation()).willReturn(true);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        given(dataSource.getAuth(name)).willReturn(authWithEmail(DEFAULT_EMAIL));

        // when
        command.executeCommand(sender, Collections.singletonList(DEFAULT_EMAIL));

        // then
        verify(sendMailSsl).hasAllInformation();
        verify(dataSource).getAuth(name);
        verifyNoMoreInteractions(dataSource);
        verify(commandService).send(sender, MessageKey.INVALID_EMAIL);
    }

    @Test
    public void shouldHandleInvalidEmailInput() {
        // given
        String name = "Rapt0r";
        Player sender = mock(Player.class);
        given(sender.getName()).willReturn(name);
        given(sendMailSsl.hasAllInformation()).willReturn(true);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        given(dataSource.getAuth(name)).willReturn(authWithEmail("raptor@example.org"));

        // when
        command.executeCommand(sender, Collections.singletonList("wrong-email@example.com"));

        // then
        verify(sendMailSsl).hasAllInformation();
        verify(dataSource).getAuth(name);
        verifyNoMoreInteractions(dataSource);
        verify(commandService).send(sender, MessageKey.INVALID_EMAIL);
    }

    @Test
    public void shouldResetPasswordAndSendEmail() {
        // given
        String name = "Vultur3";
        Player sender = mock(Player.class);
        given(sender.getName()).willReturn(name);
        given(sendMailSsl.hasAllInformation()).willReturn(true);
        given(playerCache.isAuthenticated(name)).willReturn(false);
        String email = "vulture@example.com";
        PlayerAuth auth = authWithEmail(email);
        given(dataSource.getAuth(name)).willReturn(auth);
        given(commandService.getProperty(EmailSettings.RECOVERY_PASSWORD_LENGTH)).willReturn(20);
        given(passwordSecurity.computeHash(anyString(), eq(name)))
            .willAnswer(new Answer<HashedPassword>() {
                @Override
                public HashedPassword answer(InvocationOnMock invocationOnMock) {
                    return new HashedPassword((String) invocationOnMock.getArguments()[0]);
                }
            });

        // when
        command.executeCommand(sender, Collections.singletonList(email.toUpperCase()));

        // then
        verify(sendMailSsl).hasAllInformation();
        verify(dataSource).getAuth(name);
        verify(passwordSecurity).computeHash(anyString(), eq(name));
        verify(dataSource).updatePassword(auth);
        assertThat(auth.getPassword().getHash(), stringWithLength(20));
        verify(sendMailSsl).sendPasswordMail(eq(auth), argThat(stringWithLength(20)));
        verify(commandService).send(sender, MessageKey.RECOVERY_EMAIL_SENT_MESSAGE);
    }


    private static PlayerAuth authWithEmail(String email) {
        return PlayerAuth.builder()
            .name("tester")
            .email(email)
            .build();
    }

}
