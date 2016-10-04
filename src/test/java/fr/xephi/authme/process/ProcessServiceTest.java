package fr.xephi.authme.process;

import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.permission.AuthGroupHandler;
import fr.xephi.authme.permission.AuthGroupType;
import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PlayerPermission;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.service.ValidationService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link ProcessService}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ProcessServiceTest {

    @InjectMocks
    private ProcessService processService;

    @Mock
    private ValidationService validationService;

    @Mock
    private Settings settings;

    @Mock
    private Messages messages;

    @Mock
    private PermissionsManager permissionsManager;

    @Mock
    private AuthGroupHandler authGroupHandler;

    @Test
    public void shouldGetProperty() {
        // given
        given(settings.getProperty(SecuritySettings.CAPTCHA_LENGTH)).willReturn(8);

        // when
        int result = processService.getProperty(SecuritySettings.CAPTCHA_LENGTH);

        // then
        verify(settings).getProperty(SecuritySettings.CAPTCHA_LENGTH);
        assertThat(result, equalTo(8));
    }

    @Test
    public void shouldReturnSettings() {
        // given/when
        Settings result = processService.getSettings();

        // then
        assertThat(result, equalTo(settings));
    }

    @Test
    public void shouldSendMessageToPlayer() {
        // given
        CommandSender sender = mock(CommandSender.class);
        MessageKey key = MessageKey.ACCOUNT_NOT_ACTIVATED;

        // when
        processService.send(sender, key);

        // then
        verify(messages).send(sender, key);
    }

    @Test
    public void shouldSendMessageWithReplacements() {
        // given
        CommandSender sender = mock(CommandSender.class);
        MessageKey key = MessageKey.ACCOUNT_NOT_ACTIVATED;
        String[] replacements = new String[]{"test", "toast"};

        // when
        processService.send(sender, key, replacements);

        // then
        verify(messages).send(sender, key, replacements);
    }

    @Test
    public void shouldRetrieveMessage() {
        // given
        MessageKey key = MessageKey.ACCOUNT_NOT_ACTIVATED;
        String[] lines = new String[]{"First message line", "second line"};
        given(messages.retrieve(key)).willReturn(lines);

        // when
        String[] result = processService.retrieveMessage(key);

        // then
        assertThat(result, equalTo(lines));
        verify(messages).retrieve(key);
    }

    @Test
    public void shouldRetrieveSingleMessage() {
        // given
        MessageKey key = MessageKey.ACCOUNT_NOT_ACTIVATED;
        String text = "Test text";
        given(messages.retrieveSingle(key)).willReturn(text);

        // when
        String result = processService.retrieveSingleMessage(key);

        // then
        assertThat(result, equalTo(text));
        verify(messages).retrieveSingle(key);
    }

    @Test
    public void shouldValidateEmail() {
        // given
        String email = "test@example.tld";
        given(validationService.validateEmail(email)).willReturn(true);

        // when
        boolean result = processService.validateEmail(email);

        // then
        assertThat(result, equalTo(true));
        verify(validationService).validateEmail(email);
    }

    @Test
    public void shouldCheckIfEmailCanBeUsed() {
        // given
        String email = "mail@example.com";
        CommandSender sender = mock(CommandSender.class);
        given(validationService.isEmailFreeForRegistration(email, sender))
            .willReturn(true);

        // when
        boolean result = processService.isEmailFreeForRegistration(email, sender);

        // then
        assertThat(result, equalTo(true));
        verify(validationService).isEmailFreeForRegistration(email, sender);
    }

    @Test
    public void shouldCheckPermission() {
        // given
        Player player = mock(Player.class);
        PermissionNode permission = PlayerPermission.CHANGE_PASSWORD;
        given(permissionsManager.hasPermission(player, permission)).willReturn(true);

        // when
        boolean result = processService.hasPermission(player, permission);

        // then
        verify(permissionsManager).hasPermission(player, permission);
        assertThat(result, equalTo(true));
    }

    @Test
    public void shouldSetPermissionGroup() {
        // given
        Player player = mock(Player.class);
        AuthGroupType type = AuthGroupType.LOGGED_IN;
        given(authGroupHandler.setGroup(player, type)).willReturn(true);

        // when
        boolean result = processService.setGroup(player, type);

        // then
        verify(authGroupHandler).setGroup(player, type);
        assertThat(result, equalTo(true));
    }
}
