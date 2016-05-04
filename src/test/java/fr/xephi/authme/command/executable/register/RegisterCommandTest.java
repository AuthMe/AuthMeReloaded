package fr.xephi.authme.command.executable.register;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.command.CommandService;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.security.HashAlgorithm;
import fr.xephi.authme.settings.properties.EmailSettings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.hamcrest.Description;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Test for {@link RegisterCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class RegisterCommandTest {

    @Mock
    private CommandService commandService;
    @Mock
    private Management management;
    @Mock
    private Player sender;

    @BeforeClass
    public static void setup() {
        TestHelper.setupLogger();
    }

    @Before
    public void linkMocksAndProvideSettingDefaults() {
        given(commandService.getManagement()).willReturn(management);
        given(commandService.getProperty(SecuritySettings.PASSWORD_HASH)).willReturn(HashAlgorithm.BCRYPT);
        given(commandService.getProperty(RegistrationSettings.USE_EMAIL_REGISTRATION)).willReturn(false);
        given(commandService.getProperty(RestrictionSettings.ENABLE_PASSWORD_CONFIRMATION)).willReturn(false);
    }

    @Test
    public void shouldNotRunForNonPlayerSender() {
        // given
        CommandSender sender = mock(BlockCommandSender.class);
        RegisterCommand command = new RegisterCommand();

        // when
        command.executeCommand(sender, new ArrayList<String>(), commandService);

        // then
        verify(sender).sendMessage(argThat(containsString("Player only!")));
        verifyZeroInteractions(management);
    }

    @Test
    public void shouldForwardToManagementForTwoFactor() {
        // given
        given(commandService.getProperty(SecuritySettings.PASSWORD_HASH)).willReturn(HashAlgorithm.TWO_FACTOR);
        ExecutableCommand command = new RegisterCommand();

        // when
        command.executeCommand(sender, Collections.<String>emptyList(), commandService);

        // then
        verify(management).performRegister(sender, "", "", true);
    }

    @Test
    public void shouldReturnErrorForEmptyArguments() {
        // given
        ExecutableCommand command = new RegisterCommand();

        // when
        command.executeCommand(sender, Collections.<String>emptyList(), commandService);

        // then
        verify(commandService).send(sender, MessageKey.USAGE_REGISTER);
        verifyZeroInteractions(management);
    }

    @Test
    public void shouldReturnErrorForMissingConfirmation() {
        // given
        given(commandService.getProperty(RestrictionSettings.ENABLE_PASSWORD_CONFIRMATION)).willReturn(true);
        ExecutableCommand command = new RegisterCommand();

        // when
        command.executeCommand(sender, Collections.singletonList("arrrr"), commandService);

        // then
        verify(commandService).send(sender, MessageKey.USAGE_REGISTER);
        verifyZeroInteractions(management);
    }

    @Test
    public void shouldReturnErrorForMissingEmailConfirmation() {
        // given
        given(commandService.getProperty(RegistrationSettings.USE_EMAIL_REGISTRATION)).willReturn(true);
        given(commandService.getProperty(RegistrationSettings.ENABLE_CONFIRM_EMAIL)).willReturn(true);
        ExecutableCommand command = new RegisterCommand();

        // when
        command.executeCommand(sender, Collections.singletonList("test@example.org"), commandService);

        // then
        verify(commandService).send(sender, MessageKey.USAGE_REGISTER);
        verifyZeroInteractions(management);
    }

    @Test
    public void shouldThrowErrorForMissingEmailConfiguration() {
        // given
        given(commandService.getProperty(RegistrationSettings.USE_EMAIL_REGISTRATION)).willReturn(true);
        given(commandService.getProperty(RegistrationSettings.ENABLE_CONFIRM_EMAIL)).willReturn(false);
        given(commandService.getProperty(EmailSettings.MAIL_ACCOUNT)).willReturn("");
        ExecutableCommand command = new RegisterCommand();

        // when
        command.executeCommand(sender, Collections.singletonList("myMail@example.tld"), commandService);

        // then
        verify(sender).sendMessage(argThat(containsString("no email address")));
        verifyZeroInteractions(management);
    }

    @Test
    public void shouldRejectInvalidEmail() {
        // given
        String playerMail = "player@example.org";
        given(commandService.validateEmail(playerMail)).willReturn(false);

        given(commandService.getProperty(RegistrationSettings.USE_EMAIL_REGISTRATION)).willReturn(true);
        given(commandService.getProperty(RegistrationSettings.ENABLE_CONFIRM_EMAIL)).willReturn(true);
        given(commandService.getProperty(EmailSettings.MAIL_ACCOUNT)).willReturn("server@example.com");

        ExecutableCommand command = new RegisterCommand();

        // when
        command.executeCommand(sender, Arrays.asList(playerMail, playerMail), commandService);

        // then
        verify(commandService).validateEmail(playerMail);
        verify(commandService).send(sender, MessageKey.INVALID_EMAIL);
        verifyZeroInteractions(management);
    }

    @Test
    public void shouldRejectInvalidEmailConfirmation() {
        // given
        String playerMail = "bobber@bobby.org";
        given(commandService.validateEmail(playerMail)).willReturn(true);

        given(commandService.getProperty(RegistrationSettings.USE_EMAIL_REGISTRATION)).willReturn(true);
        given(commandService.getProperty(RegistrationSettings.ENABLE_CONFIRM_EMAIL)).willReturn(true);
        given(commandService.getProperty(EmailSettings.MAIL_ACCOUNT)).willReturn("server@example.com");

        ExecutableCommand command = new RegisterCommand();

        // when
        command.executeCommand(sender, Arrays.asList(playerMail, "invalid"), commandService);

        // then
        verify(commandService).send(sender, MessageKey.USAGE_REGISTER);
        verifyZeroInteractions(management);
    }

    @Test
    public void shouldPerformEmailRegistration() {
        // given
        String playerMail = "asfd@lakjgre.lds";
        given(commandService.validateEmail(playerMail)).willReturn(true);
        int passLength = 7;
        given(commandService.getProperty(EmailSettings.RECOVERY_PASSWORD_LENGTH)).willReturn(passLength);

        given(commandService.getProperty(RegistrationSettings.USE_EMAIL_REGISTRATION)).willReturn(true);
        given(commandService.getProperty(RegistrationSettings.ENABLE_CONFIRM_EMAIL)).willReturn(true);
        given(commandService.getProperty(EmailSettings.MAIL_ACCOUNT)).willReturn("server@example.com");
        ExecutableCommand command = new RegisterCommand();

        // when
        command.executeCommand(sender, Arrays.asList(playerMail, playerMail), commandService);

        // then
        verify(commandService).validateEmail(playerMail);
        verify(management).performRegister(eq(sender), argThat(stringWithLength(passLength)), eq(playerMail), true);
    }

    @Test
    public void shouldRejectInvalidPasswordConfirmation() {
        // given
        given(commandService.getProperty(RestrictionSettings.ENABLE_PASSWORD_CONFIRMATION)).willReturn(true);
        ExecutableCommand command = new RegisterCommand();

        // when
        command.executeCommand(sender, Arrays.asList("myPass", "mypass"), commandService);

        // then
        verify(commandService).send(sender, MessageKey.PASSWORD_MATCH_ERROR);
        verifyZeroInteractions(management);
    }

    @Test
    public void shouldPerformPasswordValidation() {
        // given
        ExecutableCommand command = new RegisterCommand();

        // when
        command.executeCommand(sender, Collections.singletonList("myPass"), commandService);

        // then
        verify(management).performRegister(sender, "myPass", "", true);
    }


    private static TypeSafeMatcher<String> stringWithLength(final int length) {
        return new TypeSafeMatcher<String>() {
            @Override
            protected boolean matchesSafely(String item) {
                return item != null && item.length() == length;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("String with length " + length);
            }
        };
    }
}
