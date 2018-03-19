package fr.xephi.authme.command.executable.register;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.captcha.RegistrationCaptchaManager;
import fr.xephi.authme.mail.EmailService;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.process.register.RegisterSecondaryArgument;
import fr.xephi.authme.process.register.RegistrationType;
import fr.xephi.authme.process.register.executors.EmailRegisterParams;
import fr.xephi.authme.process.register.executors.PasswordRegisterParams;
import fr.xephi.authme.process.register.executors.RegistrationMethod;
import fr.xephi.authme.process.register.executors.TwoFactorRegisterParams;
import fr.xephi.authme.security.HashAlgorithm;
import fr.xephi.authme.service.CommonService;
import fr.xephi.authme.service.ValidationService;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.bukkit.command.BlockCommandSender;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Collections;

import static fr.xephi.authme.IsEqualByReflectionMatcher.hasEqualValuesOnAllFields;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
 * Test for {@link RegisterCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class RegisterCommandTest {

    @InjectMocks
    private RegisterCommand command;

    @Mock
    private CommonService commonService;

    @Mock
    private Management management;

    @Mock
    private EmailService emailService;

    @Mock
    private ValidationService validationService;

    @Mock
    private RegistrationCaptchaManager registrationCaptchaManager;

    @BeforeClass
    public static void setup() {
        TestHelper.setupLogger();
    }

    @Before
    public void linkMocksAndProvideSettingDefaults() {
        given(commonService.getProperty(SecuritySettings.PASSWORD_HASH)).willReturn(HashAlgorithm.BCRYPT);
        given(commonService.getProperty(RegistrationSettings.REGISTRATION_TYPE)).willReturn(RegistrationType.PASSWORD);
        given(commonService.getProperty(RegistrationSettings.REGISTER_SECOND_ARGUMENT)).willReturn(RegisterSecondaryArgument.NONE);
    }

    @Test
    public void shouldNotRunForNonPlayerSender() {
        // given
        CommandSender sender = mock(BlockCommandSender.class);

        // when
        command.executeCommand(sender, Collections.emptyList());

        // then
        verify(sender).sendMessage(argThat(containsString("Player only!")));
        verifyZeroInteractions(management, emailService);
    }

    @Test
    public void shouldForwardToManagementForTwoFactor() {
        // given
        given(commonService.getProperty(SecuritySettings.PASSWORD_HASH)).willReturn(HashAlgorithm.TWO_FACTOR);
        Player player = mockPlayerWithName("test2");

        // when
        command.executeCommand(player, Collections.emptyList());

        // then
        verify(registrationCaptchaManager).isCaptchaRequired("test2");
        verify(management).performRegister(eq(RegistrationMethod.TWO_FACTOR_REGISTRATION),
            argThat(hasEqualValuesOnAllFields(TwoFactorRegisterParams.of(player))));
        verifyZeroInteractions(emailService);
    }

    @Test
    public void shouldReturnErrorForEmptyArguments() {
        // given
        Player player = mock(Player.class);

        // when
        command.executeCommand(player, Collections.emptyList());

        // then
        verify(commonService).send(player, MessageKey.USAGE_REGISTER);
        verifyZeroInteractions(management, emailService);
    }

    @Test
    public void shouldReturnErrorForMissingConfirmation() {
        // given
        given(commonService.getProperty(RegistrationSettings.REGISTRATION_TYPE)).willReturn(RegistrationType.PASSWORD);
        given(commonService.getProperty(RegistrationSettings.REGISTER_SECOND_ARGUMENT)).willReturn(RegisterSecondaryArgument.CONFIRMATION);
        Player player = mock(Player.class);

        // when
        command.executeCommand(player, Collections.singletonList("arrrr"));

        // then
        verify(commonService).send(player, MessageKey.USAGE_REGISTER);
        verifyZeroInteractions(management, emailService);
    }

    @Test
    public void shouldReturnErrorForMissingEmailConfirmation() {
        // given
        given(emailService.hasAllInformation()).willReturn(true);
        given(commonService.getProperty(RegistrationSettings.REGISTRATION_TYPE)).willReturn(RegistrationType.EMAIL);
        given(commonService.getProperty(RegistrationSettings.REGISTER_SECOND_ARGUMENT)).willReturn(RegisterSecondaryArgument.EMAIL_MANDATORY);
        given(validationService.validateEmail(anyString())).willReturn(true);
        Player player = mock(Player.class);

        // when
        command.executeCommand(player, Collections.singletonList("test@example.org"));

        // then
        verify(commonService).send(player, MessageKey.USAGE_REGISTER);
        verifyZeroInteractions(management);
    }

    @Test
    public void shouldThrowErrorForMissingEmailConfiguration() {
        // given
        given(commonService.getProperty(RegistrationSettings.REGISTRATION_TYPE)).willReturn(RegistrationType.EMAIL);
        given(emailService.hasAllInformation()).willReturn(false);
        Player player = mock(Player.class);

        // when
        command.executeCommand(player, Collections.singletonList("myMail@example.tld"));

        // then
        verify(commonService).send(player, MessageKey.INCOMPLETE_EMAIL_SETTINGS);
        verify(emailService).hasAllInformation();
        verifyZeroInteractions(management);
    }

    @Test
    public void shouldRejectInvalidEmail() {
        // given
        String playerMail = "player@example.org";
        given(validationService.validateEmail(playerMail)).willReturn(false);
        given(commonService.getProperty(RegistrationSettings.REGISTRATION_TYPE)).willReturn(RegistrationType.EMAIL);
        given(emailService.hasAllInformation()).willReturn(true);
        Player player = mock(Player.class);

        // when
        command.executeCommand(player, Arrays.asList(playerMail, playerMail));

        // then
        verify(validationService).validateEmail(playerMail);
        verify(commonService).send(player, MessageKey.INVALID_EMAIL);
        verifyZeroInteractions(management);
    }

    @Test
    public void shouldRejectInvalidEmailConfirmation() {
        // given
        String playerMail = "bobber@bobby.org";
        given(validationService.validateEmail(playerMail)).willReturn(true);
        given(commonService.getProperty(RegistrationSettings.REGISTRATION_TYPE)).willReturn(RegistrationType.EMAIL);
        given(commonService.getProperty(RegistrationSettings.REGISTER_SECOND_ARGUMENT)).willReturn(RegisterSecondaryArgument.CONFIRMATION);
        given(emailService.hasAllInformation()).willReturn(true);
        Player player = mock(Player.class);

        // when
        command.executeCommand(player, Arrays.asList(playerMail, "invalid"));

        // then
        verify(commonService).send(player, MessageKey.USAGE_REGISTER);
        verify(emailService).hasAllInformation();
        verifyZeroInteractions(management);
    }

    @Test
    public void shouldPerformEmailRegistration() {
        // given
        String playerMail = "asfd@lakjgre.lds";
        given(validationService.validateEmail(playerMail)).willReturn(true);
        given(commonService.getProperty(RegistrationSettings.REGISTRATION_TYPE)).willReturn(RegistrationType.EMAIL);
        given(commonService.getProperty(RegistrationSettings.REGISTER_SECOND_ARGUMENT)).willReturn(RegisterSecondaryArgument.CONFIRMATION);
        given(emailService.hasAllInformation()).willReturn(true);
        Player player = mockPlayerWithName("brett");

        // when
        command.executeCommand(player, Arrays.asList(playerMail, playerMail));

        // then
        verify(registrationCaptchaManager).isCaptchaRequired("brett");
        verify(validationService).validateEmail(playerMail);
        verify(emailService).hasAllInformation();
        verify(management).performRegister(eq(RegistrationMethod.EMAIL_REGISTRATION),
            argThat(hasEqualValuesOnAllFields(EmailRegisterParams.of(player, playerMail))));
    }

    @Test
    public void shouldRejectInvalidPasswordConfirmation() {
        // given
        given(commonService.getProperty(RegistrationSettings.REGISTRATION_TYPE)).willReturn(RegistrationType.PASSWORD);
        given(commonService.getProperty(RegistrationSettings.REGISTER_SECOND_ARGUMENT)).willReturn(RegisterSecondaryArgument.CONFIRMATION);
        Player player = mock(Player.class);

        // when
        command.executeCommand(player, Arrays.asList("myPass", "mypass"));

        // then
        verify(commonService).send(player, MessageKey.PASSWORD_MATCH_ERROR);
        verifyZeroInteractions(management, emailService);
    }

    @Test
    public void shouldPerformPasswordRegistration() {
        // given
        Player player = mockPlayerWithName("newPlayer");

        // when
        command.executeCommand(player, Collections.singletonList("myPass"));

        // then
        verify(registrationCaptchaManager).isCaptchaRequired("newPlayer");
        verify(management).performRegister(eq(RegistrationMethod.PASSWORD_REGISTRATION),
            argThat(hasEqualValuesOnAllFields(PasswordRegisterParams.of(player, "myPass", null))));
    }

    @Test
    public void shouldPerformMailValidationForPasswordWithEmail() {
        // given
        given(commonService.getProperty(RegistrationSettings.REGISTRATION_TYPE)).willReturn(RegistrationType.PASSWORD);
        given(commonService.getProperty(RegistrationSettings.REGISTER_SECOND_ARGUMENT)).willReturn(RegisterSecondaryArgument.EMAIL_MANDATORY);
        String email = "email@example.org";
        given(validationService.validateEmail(email)).willReturn(true);
        Player player = mock(Player.class);

        // when
        command.executeCommand(player, Arrays.asList("myPass", email));

        // then
        verify(validationService).validateEmail(email);
        verify(management).performRegister(eq(RegistrationMethod.PASSWORD_REGISTRATION),
            argThat(hasEqualValuesOnAllFields(PasswordRegisterParams.of(player, "myPass", email))));
    }

    @Test
    public void shouldStopForInvalidEmail() {
        // given
        given(commonService.getProperty(RegistrationSettings.REGISTRATION_TYPE)).willReturn(RegistrationType.PASSWORD);
        given(commonService.getProperty(RegistrationSettings.REGISTER_SECOND_ARGUMENT)).willReturn(RegisterSecondaryArgument.EMAIL_OPTIONAL);
        String email = "email@example.org";
        given(validationService.validateEmail(email)).willReturn(false);
        Player player = mockPlayerWithName("Waaa");

        // when
        command.executeCommand(player, Arrays.asList("myPass", email));

        // then
        verify(registrationCaptchaManager).isCaptchaRequired("Waaa");
        verify(validationService).validateEmail(email);
        verify(commonService).send(player, MessageKey.INVALID_EMAIL);
        verifyZeroInteractions(management);
    }

    @Test
    public void shouldPerformNormalPasswordRegisterForOneArgument() {
        // given
        given(commonService.getProperty(RegistrationSettings.REGISTRATION_TYPE)).willReturn(RegistrationType.PASSWORD);
        given(commonService.getProperty(RegistrationSettings.REGISTER_SECOND_ARGUMENT)).willReturn(RegisterSecondaryArgument.EMAIL_OPTIONAL);
        Player player = mockPlayerWithName("Doa");

        // when
        command.executeCommand(player, Collections.singletonList("myPass"));

        // then
        verify(registrationCaptchaManager).isCaptchaRequired("Doa");
        verify(management).performRegister(eq(RegistrationMethod.PASSWORD_REGISTRATION),
            argThat(hasEqualValuesOnAllFields(PasswordRegisterParams.of(player, "myPass", null))));
    }

    @Test
    public void shouldRequestCaptcha() {
        // given
        given(registrationCaptchaManager.isCaptchaRequired(anyString())).willReturn(true);
        String name = "Brian";
        Player player = mockPlayerWithName(name);
        String captcha = "AB923C";
        given(registrationCaptchaManager.getCaptchaCodeOrGenerateNew(name)).willReturn(captcha);

        // when
        command.executeCommand(player, Arrays.asList("myPass", "myPass"));

        // then
        verify(registrationCaptchaManager).isCaptchaRequired(name);
        verify(commonService).send(player, MessageKey.CAPTCHA_FOR_REGISTRATION_REQUIRED, captcha);
        verifyZeroInteractions(management, validationService);
    }

    private static Player mockPlayerWithName(String name) {
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        return player;
    }
}
