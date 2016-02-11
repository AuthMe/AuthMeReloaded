package fr.xephi.authme.output;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.util.StringUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;

import java.io.File;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Tests that the project's default language file contains a text for all message keys.
 */
public class MessagesFileConsistencyTest {

    private static final String DEFAULT_MESSAGES_FILE = "/messages/messages_en.yml";

    @Test
    public void shouldHaveAllMessages() {
        File file = TestHelper.getJarFile(DEFAULT_MESSAGES_FILE);
        FileConfiguration configuration = YamlConfiguration.loadConfiguration(file);
        for (MessageKey messageKey : MessageKey.values()) {
            verifyHasMessage(messageKey, configuration);
        }
    }

    private static void verifyHasMessage(MessageKey messageKey, FileConfiguration configuration) {
        final String key = messageKey.getKey();
        final String message = configuration.getString(key);

        assertThat("Default messages file should have message for key '" + key + "'",
            StringUtils.isEmpty(message), equalTo(false));


        for (String tag : messageKey.getTags()) {
            assertThat("The message for key '" + key + "' contains the tag '" + tag + "' in the default messages file",
                message.contains(tag), equalTo(true));
        }
    }
}
