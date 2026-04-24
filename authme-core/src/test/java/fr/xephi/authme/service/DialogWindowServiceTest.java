package fr.xephi.authme.service;

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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class DialogWindowServiceTest {

    @InjectMocks
    private DialogWindowService dialogWindowService;

    @Mock
    private CommonService commonService;

    @Mock
    private Messages messages;

    @Test
    public void shouldBuildPostJoinRegisterDialogForOptionalEmailFlow() {
        // given
        Player player = org.mockito.Mockito.mock(Player.class);
        given(commonService.retrieveSingleMessage(eq(player), any(MessageKey.class)))
            .willAnswer(invocation -> invocation.getArgument(1, MessageKey.class).getKey());

        // when
        DialogWindowSpec dialog = dialogWindowService.createRegisterDialog(
            player, RegistrationType.PASSWORD, RegisterSecondaryArgument.EMAIL_OPTIONAL);

        // then
        assertThat(dialog.title(), is("dialog.register.title"));
        assertThat(dialog.primaryButtonLabel(), is("dialog.register.button"));
        assertThat(dialog.showSecondaryButton(), is(false));
        assertThat(dialog.canCloseWithEscape(), is(false));
        assertThat(dialog.inputs().stream().map(input -> input.id()).toList(), contains("password", "email"));
    }

    @Test
    public void shouldBuildPreJoinLoginDialogWithConfiguredToggles() {
        // given
        given(messages.retrieveSingle(eq("Bobby"), any(MessageKey.class)))
            .willAnswer(invocation -> invocation.getArgument(1, MessageKey.class).getKey());
        given(commonService.getProperty(RegistrationSettings.PRE_JOIN_DIALOG_SHOW_CANCEL_BUTTON)).willReturn(true);
        given(commonService.getProperty(RegistrationSettings.PRE_JOIN_DIALOG_ALLOW_CLOSE_WITH_ESCAPE)).willReturn(true);

        // when
        DialogWindowSpec dialog = dialogWindowService.createPreJoinLoginDialog("Bobby");

        // then
        assertThat(dialog.title(), is("dialog.login.title"));
        assertThat(dialog.primaryButtonLabel(), is("dialog.login.button"));
        assertThat(dialog.secondaryButtonLabel(), is("dialog.button.cancel"));
        assertThat(dialog.showSecondaryButton(), is(true));
        assertThat(dialog.canCloseWithEscape(), is(true));
        assertThat(dialog.inputs().stream().map(input -> input.id()).toList(), contains("password"));
    }
}
