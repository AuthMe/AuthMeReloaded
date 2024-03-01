package fr.xephi.authme.message;

import fr.xephi.authme.util.StringUtils;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test for {@link MessageKey}.
 */
class MessageKeyTest {

    @Test
    void shouldHaveUniqueMessageKeys() {
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
    void shouldHaveWellFormedPlaceholders() {
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
