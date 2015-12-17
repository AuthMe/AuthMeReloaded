package fr.xephi.authme.output;

import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.util.WrapperMock;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.File;
import java.net.URL;

import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link Messages}.
 */
public class MessagesIntegrationTest {

    private static final String YML_TEST_FILE = "messages_test.yml";
    private Messages messages;

    /**
     * Loads the messages in the file {@code messages_test.yml} in the test resources folder.
     * The file does not contain all messages defined in {@link MessageKey} and its contents
     * reflect various test cases -- not what the keys stand for.
     */
    @Before
    public void setUpMessages() {
        WrapperMock.createInstance();

        Settings.messagesLanguage = "en";
        URL url = getClass().getClassLoader().getResource(YML_TEST_FILE);
        if (url == null) {
            throw new RuntimeException("File '" + YML_TEST_FILE + "' could not be loaded");
        }

        Settings.messageFile = new File(url.getFile());
        Settings.messagesLanguage = "en";
        messages = Messages.getInstance();
    }

    @Test
    public void shouldLoadMessageAndSplitAtNewLines() {
        // given
        MessageKey key = MessageKey.UNKNOWN_USER;

        // when
        String[] message = messages.retrieve(key);

        // then
        String[] lines = new String[]{"This test message", "includes", "some new lines"};
        assertThat(message, equalTo(lines));
    }

    @Test
    public void shouldLoadMessageAsStringWithNewLines() {
        // given
        MessageKey key = MessageKey.UNKNOWN_USER;

        // when
        String message = messages.retrieveSingle(key);

        // then
        assertThat(message, equalTo("This test message\nincludes\nsome new lines"));
    }

    @Test
    public void shouldFormatColorCodes() {
        // given
        MessageKey key = MessageKey.UNSAFE_QUIT_LOCATION;

        // when
        String[] message = messages.retrieve(key);

        // then
        assertThat(message, arrayWithSize(1));
        assertThat(message[0], equalTo("§cHere we have§bdefined some colors §dand some other §lthings"));
    }

    @Test
    public void shouldRetainApostrophes() {
        // given
        MessageKey key = MessageKey.NOT_LOGGED_IN;

        // when
        String[] message = messages.retrieve(key);

        // then
        assertThat(message, arrayWithSize(1));
        assertThat(message[0], equalTo("Apostrophes ' should be loaded correctly, don't you think?"));
    }

    @Test
    public void shouldReturnErrorForUnknownCode() {
        // given
        // The following is a key that is not defined in the test file
        MessageKey key = MessageKey.UNREGISTERED_SUCCESS;

        // when
        String[] message = messages.retrieve(key);

        // then
        assertThat(message, arrayWithSize(1));
        assertThat(message[0], startsWith("Error getting message with key '"));
    }

    @Test
    public void shouldSendMessageToPlayer() {
        // given
        MessageKey key = MessageKey.UNSAFE_QUIT_LOCATION;
        Player player = Mockito.mock(Player.class);

        // when
        messages.send(player, key);

        // then
        verify(player).sendMessage("§cHere we have§bdefined some colors §dand some other §lthings");
    }

    @Test
    public void shouldSendMultiLineMessageToPlayer() {
        // given
        MessageKey key = MessageKey.UNKNOWN_USER;
        Player player = Mockito.mock(Player.class);

        // when
        messages.send(player, key);

        // then
        String[] lines = new String[]{"This test message", "includes", "some new lines"};
        for (String line : lines) {
            verify(player).sendMessage(line);
        }
    }

    @Test
    public void shouldSendMessageToPlayerWithTagReplacement() {
        // given
        MessageKey key = MessageKey.CAPTCHA_WRONG_ERROR;
        CommandSender sender = Mockito.mock(CommandSender.class);

        // when
        messages.send(sender, key, "1234");

        // then
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(sender, times(1)).sendMessage(captor.capture());
        String message = captor.getValue();
        assertThat(message, equalTo("Use /captcha 1234 to solve the captcha"));
    }

    @Test
    public void shouldNotThrowForKeyWithNoTagReplacements() {
        // given
        MessageKey key = MessageKey.CAPTCHA_WRONG_ERROR;
        CommandSender sender = mock(CommandSender.class);

        // when
        messages.send(sender, key);

        // then
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(sender, times(1)).sendMessage(captor.capture());
        String message = captor.getValue();
        assertThat(message, equalTo("Use /captcha THE_CAPTCHA to solve the captcha"));
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowForInvalidReplacementCount() {
        // given
        MessageKey key = MessageKey.CAPTCHA_WRONG_ERROR;

        // when
        messages.send(mock(CommandSender.class), key, "rep", "rep2");

        // then - expect exception
    }

    @Test(expected = RuntimeException.class)
    public void shouldThrowForReplacementsOnKeyWithNoTags() {
        // given
        MessageKey key = MessageKey.UNKNOWN_USER;

        // when
        messages.send(mock(CommandSender.class), key, "Replacement");

        // then - expect exception
    }
}
