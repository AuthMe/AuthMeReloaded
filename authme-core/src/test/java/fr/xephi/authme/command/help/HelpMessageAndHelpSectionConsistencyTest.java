package fr.xephi.authme.command.help;

import fr.xephi.authme.util.StringUtils;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.fail;

/**
 * Test for enums {@link HelpMessage} and {@link HelpSection}.
 */
class HelpMessageAndHelpSectionConsistencyTest {

    @Test
    void shouldHaveUniqueNonEmptyKeys() {
        // given
        Set<String> keys = new HashSet<>();

        // when / then
        for (HelpMessage message : HelpMessage.values()) {
            assertThat("Key for message '" + message + "' is empty",
                StringUtils.isBlank(message.getKey()), equalTo(false));
            if (!keys.add(message.getKey())) {
                fail("Key for message '" + message + "' is already used elsewhere");
            }
        }
        for (HelpSection section : HelpSection.values()) {
            assertThat("Key for section '" + section + "' is empty",
                StringUtils.isBlank(section.getKey()), equalTo(false));
            if (!keys.add(section.getKey())) {
                fail("Key for section '" + section + "' is already used elsewhere");
            }
        }
    }
}
