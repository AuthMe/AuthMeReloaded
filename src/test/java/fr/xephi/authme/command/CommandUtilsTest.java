package fr.xephi.authme.command;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link CommandUtils}.
 */
public class CommandUtilsTest {

    @Test
    public void shouldPrintPartsForStringRepresentation() {
        // given
        Iterable<String> parts = Arrays.asList("some", "parts", "for", "test");

        // when
        String str = CommandUtils.labelsToString(parts);

        // then
        assertThat(str, equalTo("some parts for test"));
    }

    @Test
    public void shouldPrintEmptyStringForNoArguments() {
        // given
        List<String> parts = Collections.EMPTY_LIST;

        // when
        String str = CommandUtils.labelsToString(parts);

        // then
        assertThat(str, equalTo(""));
    }
}
