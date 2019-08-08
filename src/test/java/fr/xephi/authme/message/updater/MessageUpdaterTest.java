package fr.xephi.authme.message.updater;

import ch.jalu.configme.properties.Property;
import ch.jalu.configme.resource.PropertyReader;
import ch.jalu.configme.resource.YamlFileReader;
import com.google.common.io.Files;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.message.MessageKey;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static fr.xephi.authme.message.MessagePathHelper.DEFAULT_MESSAGES_FILE;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link MessageUpdater}.
 */
public class MessageUpdaterTest {

    private MessageUpdater messageUpdater = new MessageUpdater();

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @BeforeClass
    public static void setUpLogger() {
        TestHelper.setupLogger();
    }

    @Test
    public void shouldNotUpdateDefaultFile() throws IOException {
        // given
        String messagesFilePath = DEFAULT_MESSAGES_FILE;
        File messagesFile = temporaryFolder.newFile();
        Files.copy(TestHelper.getJarFile("/" + messagesFilePath), messagesFile);
        long modifiedDate = messagesFile.lastModified();

        // when
        boolean wasChanged = messageUpdater.migrateAndSave(messagesFile, messagesFilePath, messagesFilePath);

        // then
        assertThat(wasChanged, equalTo(false));
        assertThat(messagesFile.lastModified(), equalTo(modifiedDate));
    }
    
    @Test
    public void shouldAddMissingKeys() throws IOException {
        // given
        File messagesFile = temporaryFolder.newFile();
        Files.copy(TestHelper.getJarFile(TestHelper.PROJECT_ROOT + "message/messages_test.yml"), messagesFile);
        
        // when
        boolean wasChanged = messageUpdater.migrateAndSave(messagesFile, "does-not-exist", DEFAULT_MESSAGES_FILE);

        // then
        assertThat(wasChanged, equalTo(true));
        PropertyReader reader = new YamlFileReader(messagesFile);
        // Existing keys should not be overridden
        assertThat(reader.getString(MessageKey.LOGIN_SUCCESS.getKey()), equalTo("&cHere we have&bdefined some colors &dand some other &lthings"));
        assertThat(reader.getString(MessageKey.EMAIL_ALREADY_USED_ERROR.getKey()), equalTo(""));
        // Check that new keys were added
        assertThat(reader.getString(MessageKey.SECOND.getKey()), equalTo("second"));
        assertThat(reader.getString(MessageKey.ERROR.getKey()), equalTo("&4An unexpected error occurred, please contact an administrator!"));
    }

    @Test
    public void shouldMigrateOldEntries() throws IOException {
        // given
        File messagesFile = temporaryFolder.newFile();
        Files.copy(TestHelper.getJarFile(TestHelper.PROJECT_ROOT + "message/messages_en_old.yml"), messagesFile);

        // when
        boolean wasChanged = messageUpdater.migrateAndSave(messagesFile, DEFAULT_MESSAGES_FILE, DEFAULT_MESSAGES_FILE);

        // then
        assertThat(wasChanged, equalTo(true));
        PropertyReader reader = new YamlFileReader(messagesFile);
        assertThat(reader.getString(MessageKey.PASSWORD_MATCH_ERROR.getKey()),
            equalTo("Password error message"));
        assertThat(reader.getString(MessageKey.INVALID_NAME_CHARACTERS.getKey()),
            equalTo("not valid username: Allowed chars are %valid_chars"));
        assertThat(reader.getString(MessageKey.INVALID_OLD_EMAIL.getKey()),
            equalTo("Email (old) is not valid!!"));
        assertThat(reader.getString(MessageKey.CAPTCHA_WRONG_ERROR.getKey()),
            equalTo("The captcha code is %captcha_code for you"));
        assertThat(reader.getString(MessageKey.CAPTCHA_FOR_REGISTRATION_REQUIRED.getKey()),
            equalTo("Now type /captcha %captcha_code"));
        assertThat(reader.getString(MessageKey.SECONDS.getKey()),
            equalTo("seconds in plural"));
    }

    @Test
    public void shouldPerformNewerMigrations() throws IOException {
        // given
        File messagesFile = temporaryFolder.newFile();
        Files.copy(TestHelper.getJarFile(TestHelper.PROJECT_ROOT + "message/messages_test2.yml"), messagesFile);

        // when
        boolean wasChanged = messageUpdater.migrateAndSave(messagesFile, DEFAULT_MESSAGES_FILE, DEFAULT_MESSAGES_FILE);

        // then
        assertThat(wasChanged, equalTo(true));
        PropertyReader reader = new YamlFileReader(messagesFile);
        assertThat(reader.getString(MessageKey.TWO_FACTOR_CREATE.getKey()), equalTo("Old 2fa create text"));
        assertThat(reader.getString(MessageKey.WRONG_PASSWORD.getKey()), equalTo("test2 - wrong password")); // from pre-5.5 key
        assertThat(reader.getString(MessageKey.SECOND.getKey()), equalTo("second")); // from messages_en.yml
    }

    @Test
    public void shouldHaveAllKeysInConfigurationData() {
        // given
        Set<String> messageKeysFromEnum = Arrays.stream(MessageKey.values())
            .map(MessageKey::getKey)
            .collect(Collectors.toSet());

        // when
        Set<String> messageKeysFromConfigData = MessageUpdater.createConfigurationData().getProperties().stream()
            .map(Property::getPath)
            .collect(Collectors.toSet());

        // then
        assertThat(messageKeysFromConfigData, equalTo(messageKeysFromEnum));
    }

    @Test
    public void shouldHaveCommentForAllRootPathsInConfigurationData() {
        // given
        Set<String> rootPaths = Arrays.stream(MessageKey.values())
            .map(key -> key.getKey().split("\\.")[0])
            .collect(Collectors.toSet());

        // when
        Map<String, List<String>> comments = MessageUpdater.createConfigurationData().getAllComments();

        // then
        assertThat(comments.keySet(), equalTo(rootPaths));
    }
}
