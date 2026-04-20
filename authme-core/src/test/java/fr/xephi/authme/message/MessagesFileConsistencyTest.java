package fr.xephi.authme.message;

import ch.jalu.configme.resource.PropertyReader;
import ch.jalu.configme.resource.YamlFileReader;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.util.StringUtils;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.fail;

/**
 * Tests that the project's default language file contains a text for all message keys.
 * <p>
 * Translators can change the file name in {@link #MESSAGES_FILE} to validate their translation.
 */
public class MessagesFileConsistencyTest {

    private static final String MESSAGES_FILE = MessagePathHelper.DEFAULT_MESSAGES_FILE;

    @Test
    public void shouldHaveAllMessages() {
        File file = TestHelper.getJarFile("/" + MESSAGES_FILE);
        PropertyReader reader = new YamlFileReader(file);
        List<String> errors = new ArrayList<>();
        for (MessageKey messageKey : MessageKey.values()) {
            validateMessage(messageKey, reader, errors);
        }

        if (!errors.isEmpty()) {
            fail("Validation errors in " + MESSAGES_FILE + ":\n- "
                + String.join("\n- ", errors));
        }
    }

    private static void validateMessage(MessageKey messageKey, PropertyReader reader, List<String> errors) {
        final String key = messageKey.getKey();
        final String message = reader.getString(key);

        if (StringUtils.isBlank(message)) {
            errors.add("Messages file should have message for key '" + key + "'");
            return;
        }

        List<String> missingTags = new ArrayList<>();
        for (String tag : messageKey.getTags()) {
            if (!message.contains(tag)) {
                missingTags.add(tag);
            }
        }

        if (!missingTags.isEmpty()) {
            String pluralS = missingTags.size() > 1 ? "s" : "";
            errors.add(String.format("Message with key '%s' missing tag%s: %s", key, pluralS,
                String.join(", ", missingTags)));
        }
    }
}
