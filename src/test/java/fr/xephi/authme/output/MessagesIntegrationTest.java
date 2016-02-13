package fr.xephi.authme.output;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.ConsoleLoggerTestInitializer;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.util.WrapperMock;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.File;
import java.util.logging.Logger;

import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assume.assumeThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link Messages}.
 */
public class MessagesIntegrationTest {

    private static final String YML_TEST_FILE = "/messages_test.yml";
    private static final String YML_DEFAULT_TEST_FILE = "/messages_default.yml";
    private Messages messages;

    @BeforeClass
    public static void setup() {
        WrapperMock.createInstance();
        ConsoleLoggerTestInitializer.setupLogger();
    }

    /**
     * Loads the messages in the file {@code messages_test.yml} in the test resources folder.
     * The file does not contain all messages defined in {@link MessageKey} and its contents
     * reflect various test cases -- not what the keys stand for.
     * <p>
     * Similarly, the {@code messages_default.yml} from the test resources represents a default
     * file that should contain all messages, but again, for testing, it just contains a few.
     */
    @Before
    public void setUpMessages() {
        File testFile = TestHelper.getJarFile(YML_TEST_FILE);
        File defaultFile = TestHelper.getJarFile(YML_DEFAULT_TEST_FILE);
        messages = new Messages(testFile, defaultFile);
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

    @Test
    public void shouldLogErrorForInvalidReplacementCount() {
        // given
        Logger logger = mock(Logger.class);
        ConsoleLogger.setLogger(logger);
        MessageKey key = MessageKey.CAPTCHA_WRONG_ERROR;

        // when
        messages.send(mock(CommandSender.class), key, "rep", "rep2");

        // then
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(logger).warning(captor.capture());
        assertThat(captor.getValue(), containsString("Invalid number of replacements"));
    }

    @Test
    public void shouldThrowForReplacementsOnKeyWithNoTags() {
        // given
        Logger logger = mock(Logger.class);
        ConsoleLogger.setLogger(logger);
        MessageKey key = MessageKey.UNKNOWN_USER;

        // when
        messages.send(mock(CommandSender.class), key, "Replacement");

        // then
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(logger).warning(captor.capture());
        assertThat(captor.getValue(), containsString("Invalid number of replacements"));
    }

    @Test
    public void shouldGetMessageFromDefaultFile() {
        // given
        // Key is only present in default file
        MessageKey key = MessageKey.MUST_REGISTER_MESSAGE;

        // when
        String message = messages.retrieveSingle(key);

        // then
        assertThat(message, equalTo("Message from default file"));
    }

    @Test
    public void shouldNotUseMessageFromDefaultFile() {
        // given
        // Key is present in both files
        MessageKey key = MessageKey.WRONG_PASSWORD;

        // when
        String message = messages.retrieveSingle(key);

        // then
        assertThat(message, equalTo("§cWrong password!"));
    }

    @Test
    public void shouldReturnErrorForMissingMessage() {
        // given
        // Key is not present in test file or default file
        MessageKey key = MessageKey.TWO_FACTOR_CREATE;

        // when
        String message = messages.retrieveSingle(key);

        // then
        assertThat(message, containsString("Error retrieving message"));
    }

    @Test
    public void shouldAllowNullAsDefaultFile() {
        // given
        Messages testMessages = new Messages(TestHelper.getJarFile(YML_TEST_FILE), null);
        // Key not present in test file
        MessageKey key = MessageKey.TWO_FACTOR_CREATE;

        // when
        String message = testMessages.retrieveSingle(key);

        // then
        assertThat(message, containsString("Error retrieving message"));
    }

    @Test
    public void shouldLoadOtherFile() {
        // given
        MessageKey key = MessageKey.WRONG_PASSWORD;
        // assumption: message comes back as defined in messages_test.yml
        assumeThat(messages.retrieveSingle(key), equalTo("§cWrong password!"));

        // when
        messages.reload(TestHelper.getJarFile("/messages_test2.yml"));

        // then
        assertThat(messages.retrieveSingle(key), equalTo("test2 - wrong password"));
        // check that default message handling still works
        assertThat(messages.retrieveSingle(MessageKey.MUST_REGISTER_MESSAGE),
            equalTo("Message from default file"));
    }
}
