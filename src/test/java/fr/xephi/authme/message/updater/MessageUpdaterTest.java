package fr.xephi.authme.message.updater;

import com.google.common.io.Files;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.message.MessageKey;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import java.io.File;
import java.io.IOException;

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
        String messagesFilePath = "messages/messages_en.yml";
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
        boolean wasChanged = messageUpdater.migrateAndSave(messagesFile, "does-not-exist", "messages/messages_en.yml");

        // then
        assertThat(wasChanged, equalTo(true));
        FileConfiguration configuration = YamlConfiguration.loadConfiguration(messagesFile);
        // Existing keys should not be overridden
        assertThat(configuration.getString(MessageKey.LOGIN_SUCCESS.getKey()), equalTo("&cHere we have&bdefined some colors &dand some other &lthings"));
        assertThat(configuration.getString(MessageKey.EMAIL_ALREADY_USED_ERROR.getKey()), equalTo(""));
        // Check that new keys were added
        assertThat(configuration.getString(MessageKey.SECOND.getKey()), equalTo("second"));
        assertThat(configuration.getString(MessageKey.ERROR.getKey()), equalTo("&4An unexpected error occurred, please contact an administrator!"));
    }

    @Test
    public void shouldMigrateOldEntries() throws IOException {
        // given
        File messagesFile = temporaryFolder.newFile();
        Files.copy(TestHelper.getJarFile(TestHelper.PROJECT_ROOT + "message/messages_en_old.yml"), messagesFile);

        // when
        boolean wasChanged = messageUpdater.migrateAndSave(messagesFile, "messages/messages_en.yml", "messages/messages_en.yml");

        // then
        assertThat(wasChanged, equalTo(true));
        FileConfiguration configuration = YamlConfiguration.loadConfiguration(messagesFile);
        assertThat(configuration.getString(MessageKey.PASSWORD_MATCH_ERROR.getKey()),
            equalTo("Password error message"));
        assertThat(configuration.getString(MessageKey.INVALID_NAME_CHARACTERS.getKey()),
            equalTo("not valid username: Allowed chars are %valid_chars"));
        assertThat(configuration.getString(MessageKey.INVALID_OLD_EMAIL.getKey()),
            equalTo("Email (old) is not valid!!"));
        assertThat(configuration.getString(MessageKey.CAPTCHA_WRONG_ERROR.getKey()),
            equalTo("The captcha code is %captcha_code for you"));
        assertThat(configuration.getString(MessageKey.CAPTCHA_FOR_REGISTRATION_REQUIRED.getKey()),
            equalTo("Now type /captcha %captcha_code"));
        assertThat(configuration.getString(MessageKey.SECONDS.getKey()),
            equalTo("seconds in plural"));
    }
}
