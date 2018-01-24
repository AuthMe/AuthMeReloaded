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

    // TODO #1467: Check migration of old keys
}
