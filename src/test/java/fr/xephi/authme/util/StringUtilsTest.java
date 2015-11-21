package fr.xephi.authme.util;

import org.junit.Test;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link StringUtils}.
 */
public class StringUtilsTest {

    @Test
    public void shouldFindContainedItem() {
        // given
        String text = "This is a test of containsAny()";
        String piece = "test";

        // when
        boolean result = StringUtils.containsAny(text, "some", "words", "that", "do not", "exist", piece);

        // then
        assertThat(result, equalTo(true));
    }

    @Test
    public void shouldReturnFalseIfNoneFound() {
        // given
        String text = "This is a test string";

        // when
        boolean result = StringUtils.containsAny(text, "some", "other", "words", null);

        // then
        assertThat(result, equalTo(false));
    }

    @Test
    public void shouldReturnFalseForNullString() {
        // given/when
        boolean result = StringUtils.containsAny(null, "some", "words", "to", "check");

        // then
        assertThat(result, equalTo(false));
    }
}
