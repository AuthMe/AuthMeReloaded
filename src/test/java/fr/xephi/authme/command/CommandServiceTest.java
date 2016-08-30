package fr.xephi.authme.command;

import com.github.authme.configme.properties.Property;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import fr.xephi.authme.util.ValidationService;
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
 * Test for {@link CommandService}.
 */
@RunWith(MockitoJUnitRunner.class)
public class CommandServiceTest {

    @InjectMocks
    private CommandService commandService;
    @Mock
    private Messages messages;
    @Mock
    private Settings settings;
    @Mock
    private ValidationService validationService;

    @Test
    public void shouldSendMessage() {
        // given
        CommandSender sender = mock(CommandSender.class);

        // when
        commandService.send(sender, MessageKey.INVALID_EMAIL);

        // then
        verify(messages).send(sender, MessageKey.INVALID_EMAIL);
    }

    @Test
    public void shouldSendMessageWithReplacements() {
        // given
        CommandSender sender = mock(Player.class);

        // when
        commandService.send(sender, MessageKey.ANTIBOT_AUTO_ENABLED_MESSAGE, "10");

        // then
        verify(messages).send(sender, MessageKey.ANTIBOT_AUTO_ENABLED_MESSAGE, "10");
    }

    @Test
    public void shouldRetrieveMessage() {
        // given
        MessageKey key = MessageKey.USAGE_CAPTCHA;
        String[] givenMessages = new String[]{"Lorem ipsum...", "Test line test"};
        given(messages.retrieve(key)).willReturn(givenMessages);

        // when
        String[] result = commandService.retrieveMessage(key);

        // then
        assertThat(result, equalTo(givenMessages));
        verify(messages).retrieve(key);
    }

    @Test
    public void shouldRetrieveProperty() {
        // given
        Property<Integer> property = SecuritySettings.CAPTCHA_LENGTH;
        given(settings.getProperty(property)).willReturn(7);

        // when
        int result = commandService.getProperty(property);

        // then
        assertThat(result, equalTo(7));
        verify(settings).getProperty(property);
    }

    @Test
    public void shouldReturnSettings() {
        // given/when
        Settings result = commandService.getSettings();

        // then
        assertThat(result, equalTo(settings));
    }

    @Test
    public void shouldValidateEmail() {
        // given
        String email = "test@example.tld";
        given(validationService.validateEmail(email)).willReturn(true);

        // when
        boolean result = commandService.validateEmail(email);

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
        boolean result = commandService.isEmailFreeForRegistration(email, sender);

        // then
        assertThat(result, equalTo(true));
        verify(validationService).isEmailFreeForRegistration(email, sender);
    }

}
