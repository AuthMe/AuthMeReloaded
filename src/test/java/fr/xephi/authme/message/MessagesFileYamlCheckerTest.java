package fr.xephi.authme.message;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.util.StringUtils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.Assert.fail;

/**
 * Tests that all YML message files can be loaded.
 */
public class MessagesFileYamlCheckerTest {

    /** Path in the resources folder where the message files are located. */
    private static final String MESSAGES_FOLDER = "/messages/";
    /** Pattern of the message file names. */
    private static final Pattern MESSAGE_FILE_PATTERN = Pattern.compile("messages_\\w+\\.yml");
    /** Message key that is present in all files. Used to make sure that text is returned. */
    private static final MessageKey MESSAGE_KEY = MessageKey.LOGIN_MESSAGE;

    @Test
    public void shouldAllBeValidYaml() {
        // given
        List<File> messageFiles = getMessageFiles();

        // when
        List<String> errors = new ArrayList<>();
        for (File file : messageFiles) {
            String error = null;
            try {
                YamlConfiguration configuration = YamlConfiguration.loadConfiguration(file);
                if (StringUtils.isEmpty(configuration.getString(MESSAGE_KEY.getKey()))) {
                    error = "Message for '" + MESSAGE_KEY + "' is empty";
                }
            } catch (Exception e) {
                error = "Could not load file: " + StringUtils.formatException(e);
            }
            if (!StringUtils.isEmpty(error)) {
                errors.add(file.getName() + ": " + error);
            }
        }

        // then
        if (!errors.isEmpty()) {
            fail("Errors during verification of message files:\n-" + String.join("\n-", errors));
        }
    }


    private List<File> getMessageFiles() {
        File folder = TestHelper.getJarFile(MESSAGES_FOLDER);
        File[] files = folder.listFiles();
        if (files == null) {
            throw new IllegalStateException("Could not read folder '" + folder.getName() + "'");
        }

        List<File> messageFiles = new ArrayList<>();
        for (File file : files) {
            if (MESSAGE_FILE_PATTERN.matcher(file.getName()).matches()) {
                messageFiles.add(file);
            }
        }
        if (messageFiles.isEmpty()) {
            throw new IllegalStateException("Error getting message files: list of files is empty");
        }
        return messageFiles;
    }

}
