package fr.xephi.authme.message;

import fr.xephi.authme.util.StringUtils;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Test for {@link MessageKey}.
 */
public class MessageKeyTest {

    @Test
    public void shouldHaveUniqueMessageKeys() {
        // given
        MessageKey[] messageKeys = MessageKey.values();
        Set<String> keys = new HashSet<>();

        // when / then
        for (MessageKey messageKey : messageKeys) {
            String key = messageKey.getKey();
            if (!keys.add(key)) {
                fail("Found key '" + messageKey.getKey() + "' twice!");
            } else if (StringUtils.isBlank(key)) {
                fail("Key for message key '" + messageKey + "' is empty");
            }
        }
    }

    @Test
    public void shouldHaveWellFormedPlaceholders() {
        // given
        MessageKey[] messageKeys = MessageKey.values();

        // when / then
        for (MessageKey messageKey : messageKeys) {
            String[] tags = messageKey.getTags();
            Arrays.stream(tags)
                .forEach(tag -> assertThat("Tag '" + tag + "' corresponds to valid format for key '" + messageKey + "'",
                    tag, matchesPattern("^%[a-z_]+$")));
        }
    }
}
