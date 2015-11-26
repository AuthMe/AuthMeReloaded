package fr.xephi.authme.settings;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.AuthMeMockUtil;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.util.WrapperMock;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import java.io.File;
import java.net.URL;

import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.startsWith;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link Messages}.
 */
public class MessagesTest {

    private static final String YML_TEST_FILE = "messages_test.yml";
    private Messages messages;

    /**
     * Loads the messages in the file {@code messages_test.yml} in the test resources folder.
     * The file does not contain all messages defined in {@link MessageKey} and its contents
     * reflect various test cases -- not what the keys stand for.
     */
    @Before
    public void setUpMessages() {
        WrapperMock.getInstance();

        Settings.messagesLanguage = "en";
        URL url = getClass().getClassLoader().getResource(YML_TEST_FILE);
        if (url == null) {
            throw new RuntimeException("File '" + YML_TEST_FILE + "' could not be loaded");
        }

        File file = new File(url.getFile());
        messages = new Messages(file, "en");
    }

    @Test
    public void shouldLoadMessageAndSplitAtNewLines() {
        // given
        MessageKey key = MessageKey.UNKNOWN_USER;

        // when
        String[] send = messages.retrieve(key);

        // then
        String[] lines = new String[]{"This test message", "includes", "some new lines"};
        assertThat(send, equalTo(lines));
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
}
