package fr.xephi.authme.service;

import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PlayerPermission;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link CommonService}.
 */
@RunWith(MockitoJUnitRunner.class)
public class CommonServiceTest {

    @InjectMocks
    private CommonService commonService;

    @Mock
    private Settings settings;

    @Mock
    private Messages messages;

    @Mock
    private PermissionsManager permissionsManager;

    @Test
    public void shouldGetProperty() {
        // given
        given(settings.getProperty(SecuritySettings.CAPTCHA_LENGTH)).willReturn(8);

        // when
        int result = commonService.getProperty(SecuritySettings.CAPTCHA_LENGTH);

        // then
        verify(settings).getProperty(SecuritySettings.CAPTCHA_LENGTH);
        assertThat(result, equalTo(8));
    }

    @Test
    public void shouldSendMessageToPlayer() {
        // given
        CommandSender sender = mock(CommandSender.class);
        MessageKey key = MessageKey.ACCOUNT_NOT_ACTIVATED;

        // when
        commonService.send(sender, key);

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
        commonService.send(sender, key, replacements);

        // then
        verify(messages).send(sender, key, replacements);
    }

    @Test
    public void shouldRetrieveSingleMessage() {
        // given
        MessageKey key = MessageKey.ACCOUNT_NOT_ACTIVATED;
        Player player = mock(Player.class);
        String text = "Test text";
        given(messages.retrieveSingle(player, key)).willReturn(text);

        // when
        String result = commonService.retrieveSingleMessage(player, key);

        // then
        assertThat(result, equalTo(text));
        verify(messages).retrieveSingle(player, key);
    }

    @Test
    public void shouldCheckPermission() {
        // given
        Player player = mock(Player.class);
        PermissionNode permission = PlayerPermission.CHANGE_PASSWORD;
        given(permissionsManager.hasPermission(player, permission)).willReturn(true);

        // when
        boolean result = commonService.hasPermission(player, permission);

        // then
        verify(permissionsManager).hasPermission(player, permission);
        assertThat(result, equalTo(true));
    }
}
