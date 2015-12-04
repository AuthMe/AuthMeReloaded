package fr.xephi.authme.command;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link CommandParts}.
 */
public class CommandPartsTest {

    @Test
    public void shouldPrintPartsForStringRepresentation() {
        // given
        CommandParts parts = new CommandParts(Arrays.asList("some", "parts", "for", "test"));

        // when
        String str = parts.toString();

        // then
        assertThat(str, equalTo("some parts for test"));
    }

    @Test
    public void shouldPrintEmptyStringForNoArguments() {
        // given
        CommandParts parts = new CommandParts(Collections.EMPTY_LIST);

        // when
        String str = parts.toString();

        // then
        assertThat(str, equalTo(""));
    }
}
