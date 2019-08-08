package fr.xephi.authme.message;

import com.google.common.collect.ImmutableMap;
import com.google.common.io.Files;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.message.updater.MessageUpdater;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.util.FileUtils;
import fr.xephi.authme.util.expiring.Duration;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import java.io.File;
import java.io.IOException;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static org.hamcrest.Matchers.arrayWithSize;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
 * Test for {@link Messages}.
 */
public class MessagesIntegrationTest {

    private static final String TEST_MESSAGES_LOCAL_PATH = "message/messages_test.yml";
    private static final String YML_TEST_FILE = TestHelper.PROJECT_ROOT + TEST_MESSAGES_LOCAL_PATH;
    private Messages messages;
    private MessagesFileHandler messagesFileHandler;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    private File dataFolder;

    @BeforeClass
    public static void setup() {
        TestHelper.setupLogger();
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
    public void setUpMessages() throws IOException {
        dataFolder = temporaryFolder.newFolder();
        File testFile = new File(dataFolder, MessagePathHelper.createMessageFilePath("test"));
        new File(dataFolder, MessagePathHelper.MESSAGES_FOLDER).mkdirs();
        FileUtils.create(testFile);
        Files.copy(TestHelper.getJarFile(YML_TEST_FILE), testFile);

        messagesFileHandler = createMessagesFileHandler();
        messages = new Messages(messagesFileHandler);
    }

    @AfterClass
    public static void removeLoggerReferences() {
        ConsoleLogger.initialize(null, null);
    }

    @Test
    public void shouldLoadMessageAndSplitAtNewLines() {
        // given
        MessageKey key = MessageKey.UNKNOWN_USER;
        CommandSender sender = mock(CommandSender.class);
        given(sender.getName()).willReturn("Tester");

        // when
        String[] message = messages.retrieve(key, sender);

        // then
        String[] lines = new String[]{"We've got", "new lines", "and ' apostrophes"};
        assertThat(message, equalTo(lines));
    }

    @Test
    public void shouldLoadMessageAsStringWithNewLines() {
        // given
        MessageKey key = MessageKey.UNKNOWN_USER;
        CommandSender sender = mock(CommandSender.class);
        given(sender.getName()).willReturn("Tester");

        // when
        String message = messages.retrieveSingle(sender, key);

        // then
        assertThat(message, equalTo("We've got\nnew lines\nand ' apostrophes"));
    }

    @Test
    public void shouldFormatColorCodes() {
        // given
        MessageKey key = MessageKey.LOGIN_SUCCESS;
        CommandSender sender = mock(CommandSender.class);
        given(sender.getName()).willReturn("Tester");

        // when
        String[] message = messages.retrieve(key, sender);

        // then
        assertThat(message, arrayWithSize(1));
        assertThat(message[0], equalTo("§cHere we have§bdefined some colors §dand some other §lthings"));
    }

    @Test
    public void shouldNotSendEmptyMessage() {
        // given
        MessageKey key = MessageKey.EMAIL_ALREADY_USED_ERROR;
        CommandSender sender = mock(CommandSender.class);
        given(sender.getName()).willReturn("Tester");

        // when
        messages.send(sender, key);

        // then
        verify(sender, never()).sendMessage(anyString());
    }

    @Test
    public void shouldSendMessageToPlayer() {
        // given
        MessageKey key = MessageKey.LOGIN_SUCCESS;
        Player player = Mockito.mock(Player.class);
        given(player.getName()).willReturn("Tester");
        given(player.getDisplayName()).willReturn("§cTesty");

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
        given(player.getName()).willReturn("Tester");
        given(player.getDisplayName()).willReturn("§cTesty");

        // when
        messages.send(player, key);

        // then
        String[] lines = new String[]{"We've got", "new lines", "and ' apostrophes"};
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(player, times(3)).sendMessage(captor.capture());
        assertThat(captor.getAllValues(), contains(lines));
    }

    @Test
    public void shouldSendMessageToPlayerWithNameReplacement() {
        // given
        MessageKey key = MessageKey.REGISTER_MESSAGE;
        Player player = Mockito.mock(Player.class);
        given(player.getName()).willReturn("Tester");
        given(player.getDisplayName()).willReturn("§cTesty");

        // when
        messages.send(player, key);

        // then
        verify(player).sendMessage("§3Please Tester, register to the §cTesty§3.");
    }

    @Test
    public void shouldSendMessageToPlayerWithTagReplacement() {
        // given
        MessageKey key = MessageKey.CAPTCHA_WRONG_ERROR;
        CommandSender sender = Mockito.mock(CommandSender.class);
        given(sender.getName()).willReturn("Tester");

        // when
        messages.send(sender, key, "1234");

        // then
        verify(sender, times(1)).sendMessage("Use /captcha 1234 to solve the captcha");
    }

    @Test
    public void shouldNotLogErrorForKeyWithNoTagReplacements() {
        // given
        MessageKey key = MessageKey.CAPTCHA_WRONG_ERROR;
        CommandSender sender = mock(CommandSender.class);
        given(sender.getName()).willReturn("Tester");

        // when
        messages.send(sender, key);

        // then
        verify(sender).sendMessage("Use /captcha %captcha_code to solve the captcha");
    }

    @Test
    public void shouldLogErrorForInvalidReplacementCount() {
        // given
        Logger logger = mock(Logger.class);
        ConsoleLogger.initialize(logger, null);
        MessageKey key = MessageKey.CAPTCHA_WRONG_ERROR;
        CommandSender sender = mock(CommandSender.class);
        given(sender.getName()).willReturn("Tester");

        // when
        messages.send(sender, key, "rep", "rep2");

        // then
        verify(logger).warning(argThat(containsString("Invalid number of replacements")));
    }

    @Test
    public void shouldSendErrorForReplacementsOnKeyWithNoTags() {
        // given
        Logger logger = mock(Logger.class);
        ConsoleLogger.initialize(logger, null);
        MessageKey key = MessageKey.UNKNOWN_USER;
        CommandSender sender = mock(CommandSender.class);
        given(sender.getName()).willReturn("Tester");

        // when
        messages.send(sender, key, "Replacement");

        // then
        verify(logger).warning(argThat(containsString("Invalid number of replacements")));
    }

    @Test
    public void shouldNotUseMessageFromDefaultFile() {
        // given
        // Key is present in both files
        MessageKey key = MessageKey.WRONG_PASSWORD;
        CommandSender sender = mock(CommandSender.class);
        given(sender.getName()).willReturn("Tester");

        // when
        String message = messages.retrieveSingle(sender, key);

        // then
        assertThat(message, equalTo("§cWrong password!"));
    }

    @Test
    public void shouldRetrieveMessageWithReplacements() {
        // given
        MessageKey key = MessageKey.CAPTCHA_WRONG_ERROR;
        CommandSender sender = mock(CommandSender.class);
        given(sender.getName()).willReturn("Tester");

        // when
        String result = messages.retrieveSingle(sender.getName(), key, "24680");

        // then
        assertThat(result, equalTo("Use /captcha 24680 to solve the captcha"));
    }

    @Test
    public void shouldFormatDurationObjects() throws IOException {
        // given
        // Use the JAR's messages_en.yml file for this, so copy to the file we're using and reload the file handler
        File testFile = new File(dataFolder, MessagePathHelper.createMessageFilePath("test"));
        Files.copy(TestHelper.getJarFile("/" + MessagePathHelper.DEFAULT_MESSAGES_FILE), testFile);
        messagesFileHandler.reload();

        Map<Duration, String> expectedTexts = ImmutableMap.<Duration, String>builder()
            .put(new Duration(1, TimeUnit.SECONDS), "1 second")
            .put(new Duration(12, TimeUnit.SECONDS), "12 seconds")
            .put(new Duration(1, TimeUnit.MINUTES), "1 minute")
            .put(new Duration(0, TimeUnit.MINUTES), "0 minutes")
            .put(new Duration(1, TimeUnit.HOURS), "1 hour")
            .put(new Duration(-4, TimeUnit.HOURS), "-4 hours")
            .put(new Duration(1, TimeUnit.DAYS), "1 day")
            .put(new Duration(44, TimeUnit.DAYS), "44 days")
            .build();

        // when / then
        for (Map.Entry<Duration, String> entry : expectedTexts.entrySet()) {
            assertThat(messages.formatDuration(entry.getKey()), equalTo(entry.getValue()));
        }
    }

    private MessagesFileHandler createMessagesFileHandler() {
        Settings settings = mock(Settings.class);
        given(settings.getProperty(PluginSettings.MESSAGES_LANGUAGE)).willReturn("test");

        MessagesFileHandler messagesFileHandler = new MessagesFileHandler();
        ReflectionTestUtils.setField(AbstractMessageFileHandler.class, messagesFileHandler, "settings", settings);
        ReflectionTestUtils.setField(AbstractMessageFileHandler.class, messagesFileHandler, "dataFolder", dataFolder);
        ReflectionTestUtils.setField(MessagesFileHandler.class, messagesFileHandler, "messageUpdater", mock(MessageUpdater.class));
        ReflectionTestUtils.invokePostConstructMethods(messagesFileHandler);
        return messagesFileHandler;
    }
}
