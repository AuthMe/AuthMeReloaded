package fr.xephi.authme.service;

import fr.xephi.authme.mail.EmailService;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.platform.DialogWindowSpec;
import fr.xephi.authme.process.register.RegisterSecondaryArgument;
import fr.xephi.authme.process.register.RegistrationType;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class DialogWindowServiceTest {

    @InjectMocks
    private DialogWindowService dialogWindowService;

    @Mock
    private CommonService commonService;

    @Mock
    private Messages messages;

    @Mock
    private EmailService emailService;

    @Test
    public void shouldBuildPostJoinLoginDialogWithForgotPasswordButtonWhenEmailServiceConfigured() {
        // given
        Player player = org.mockito.Mockito.mock(Player.class);
        given(commonService.retrieveSingleMessage(eq(player), any(MessageKey.class)))
            .willAnswer(invocation -> invocation.getArgument(1, MessageKey.class).getKey());
        given(commonService.getProperty(RegistrationSettings.DIALOG_SHOW_FORGOT_PASSWORD_BUTTON)).willReturn(true);
        given(commonService.getProperty(RegistrationSettings.DIALOG_SHOW_BODY)).willReturn(true);
        given(emailService.hasAllInformation()).willReturn(true);

        // when
        DialogWindowSpec dialog = dialogWindowService.createLoginDialog(player);

        // then
        assertThat(dialog.title(), is("dialog.login.title"));
        assertThat(dialog.primaryButtonLabel(), is("dialog.login.button"));
        assertThat(dialog.secondaryButtonLabel(), is("dialog.login.recovery_button"));
        assertThat(dialog.showSecondaryButton(), is(true));
        assertThat(dialog.secondaryButtonCommand(), is("email recover $(email)"));
        assertThat(dialog.canCloseWithEscape(), is(false));
        assertThat(dialog.body(), is("dialog.login.body"));
        assertThat(dialog.inputs().stream().map(input -> input.id()).toList(), contains("password", "email"));
    }

    @Test
    public void shouldNotShowForgotPasswordButtonWhenEmailServiceNotConfigured() {
        // given
        Player player = org.mockito.Mockito.mock(Player.class);
        given(commonService.retrieveSingleMessage(eq(player), any(MessageKey.class)))
            .willAnswer(invocation -> invocation.getArgument(1, MessageKey.class).getKey());
        given(commonService.getProperty(RegistrationSettings.DIALOG_SHOW_FORGOT_PASSWORD_BUTTON)).willReturn(true);
        given(commonService.getProperty(RegistrationSettings.DIALOG_SHOW_BODY)).willReturn(false);
        given(emailService.hasAllInformation()).willReturn(false);

        // when — email service not configured: recovery must not appear regardless of the flag
        DialogWindowSpec dialog = dialogWindowService.createLoginDialog(player);

        // then
        assertThat(dialog.showSecondaryButton(), is(false));
        assertThat(dialog.secondaryButtonCommand(), is(nullValue()));
        assertThat(dialog.inputs().stream().map(input -> input.id()).toList(), contains("password"));
    }

    @Test
    public void shouldBuildPostJoinLoginDialogWithoutForgotPasswordButtonWhenDisabledByConfig() {
        // given
        Player player = org.mockito.Mockito.mock(Player.class);
        given(commonService.retrieveSingleMessage(eq(player), any(MessageKey.class)))
            .willAnswer(invocation -> invocation.getArgument(1, MessageKey.class).getKey());
        given(commonService.getProperty(RegistrationSettings.DIALOG_SHOW_FORGOT_PASSWORD_BUTTON)).willReturn(false);
        given(commonService.getProperty(RegistrationSettings.DIALOG_SHOW_BODY)).willReturn(false);
        given(emailService.hasAllInformation()).willReturn(true);

        // when — disabled by config even when email service is available
        DialogWindowSpec dialog = dialogWindowService.createLoginDialog(player);

        // then
        assertThat(dialog.showSecondaryButton(), is(false));
        assertThat(dialog.secondaryButtonCommand(), is(nullValue()));
        assertThat(dialog.body(), is(nullValue()));
        assertThat(dialog.inputs().stream().map(input -> input.id()).toList(), contains("password"));
    }

    @Test
    public void shouldBuildPostJoinRegisterDialogWithEmailBeforePasswordForMandatoryEmailFlow() {
        // given
        Player player = org.mockito.Mockito.mock(Player.class);
        given(commonService.retrieveSingleMessage(eq(player), any(MessageKey.class)))
            .willAnswer(invocation -> invocation.getArgument(1, MessageKey.class).getKey());
        given(commonService.getProperty(RegistrationSettings.DIALOG_SHOW_BODY)).willReturn(false);

        // when
        DialogWindowSpec dialog = dialogWindowService.createRegisterDialog(
            player, RegistrationType.PASSWORD, RegisterSecondaryArgument.EMAIL_MANDATORY);

        // then — email comes before password for mandatory email flow
        assertThat(dialog.inputs().stream().map(input -> input.id()).toList(), contains("email", "password"));
    }

    @Test
    public void shouldBuildPostJoinRegisterDialogForOptionalEmailFlow() {
        // given
        Player player = org.mockito.Mockito.mock(Player.class);
        given(commonService.retrieveSingleMessage(eq(player), any(MessageKey.class)))
            .willAnswer(invocation -> invocation.getArgument(1, MessageKey.class).getKey());
        given(commonService.getProperty(RegistrationSettings.DIALOG_SHOW_BODY)).willReturn(false);

        // when
        DialogWindowSpec dialog = dialogWindowService.createRegisterDialog(
            player, RegistrationType.PASSWORD, RegisterSecondaryArgument.EMAIL_OPTIONAL);

        // then — password first, then optional email
        assertThat(dialog.title(), is("dialog.register.title"));
        assertThat(dialog.primaryButtonLabel(), is("dialog.register.button"));
        assertThat(dialog.showSecondaryButton(), is(false));
        assertThat(dialog.canCloseWithEscape(), is(false));
        assertThat(dialog.inputs().stream().map(input -> input.id()).toList(), contains("password", "email"));
    }

    @Test
    public void shouldBuildPreJoinLoginDialogWithCancelButtonWhenEmailServiceNotConfigured() {
        // given
        given(messages.retrieveSingle(eq("Bobby"), any(MessageKey.class)))
            .willAnswer(invocation -> invocation.getArgument(1, MessageKey.class).getKey());
        given(commonService.getProperty(RegistrationSettings.DIALOG_SHOW_FORGOT_PASSWORD_BUTTON)).willReturn(true);
        given(commonService.getProperty(RegistrationSettings.PRE_JOIN_DIALOG_SHOW_CANCEL_BUTTON)).willReturn(true);
        given(commonService.getProperty(RegistrationSettings.PRE_JOIN_DIALOG_ALLOW_CLOSE_WITH_ESCAPE)).willReturn(true);
        given(commonService.getProperty(RegistrationSettings.DIALOG_SHOW_BODY)).willReturn(true);
        given(emailService.hasAllInformation()).willReturn(false);

        // when — email not configured: no recovery, cancel button shown instead
        DialogWindowSpec dialog = dialogWindowService.createPreJoinLoginDialog("Bobby");

        // then
        assertThat(dialog.title(), is("dialog.login.title"));
        assertThat(dialog.primaryButtonLabel(), is("dialog.login.button"));
        assertThat(dialog.secondaryButtonLabel(), is("dialog.button.cancel"));
        assertThat(dialog.showSecondaryButton(), is(true));
        assertThat(dialog.canCloseWithEscape(), is(true));
        assertThat(dialog.body(), is("dialog.login.body"));
        assertThat(dialog.inputs().stream().map(input -> input.id()).toList(), contains("password"));
        assertThat(dialog.secondaryButtonCommand(), is(nullValue()));
    }

    @Test
    public void shouldBuildPreJoinLoginDialogWithRecoveryButtonWhenEmailServiceConfigured() {
        // given
        given(messages.retrieveSingle(eq("Bobby"), any(MessageKey.class)))
            .willAnswer(invocation -> invocation.getArgument(1, MessageKey.class).getKey());
        given(commonService.getProperty(RegistrationSettings.DIALOG_SHOW_FORGOT_PASSWORD_BUTTON)).willReturn(true);
        given(commonService.getProperty(RegistrationSettings.DIALOG_SHOW_BODY)).willReturn(false);
        given(emailService.hasAllInformation()).willReturn(true);

        // when — email configured: recovery button shown, cancel suppressed, email field present
        DialogWindowSpec dialog = dialogWindowService.createPreJoinLoginDialog("Bobby");

        // then — email field is on a separate recovery dialog now; only password on login dialog
        assertThat(dialog.secondaryButtonLabel(), is("dialog.login.recovery_button"));
        assertThat(dialog.showSecondaryButton(), is(true));
        assertThat(dialog.canCloseWithEscape(), is(false));
        assertThat(dialog.secondaryButtonCommand(), is(notNullValue()));
        assertThat(dialog.inputs().stream().map(input -> input.id()).toList(), contains("password"));
    }
}
