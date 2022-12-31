package fr.xephi.authme.util;

import org.junit.Test;

import static java.util.Arrays.asList;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

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
        boolean result = StringUtils.containsAny(text, asList("some", "words", "that", "do not", "exist", piece));

        // then
        assertThat(result, equalTo(true));
    }

    @Test
    public void shouldReturnFalseIfNoneFound() {
        // given
        String text = "This is a test string";

        // when
        boolean result = StringUtils.containsAny(text, asList("some", "other", "words", null));

        // then
        assertThat(result, equalTo(false));
    }

    @Test
    public void shouldReturnFalseForNullString() {
        // given/when
        boolean result = StringUtils.containsAny(null, asList("some", "words", "to", "check"));

        // then
        assertThat(result, equalTo(false));
    }

    @Test
    public void shouldCheckIfIsBlankString() {
        // Should be true for null/empty/whitespace
        assertTrue(StringUtils.isBlank(null));
        assertTrue(StringUtils.isBlank(""));
        assertTrue(StringUtils.isBlank(" \t"));

        // Should be false if string has content
        assertFalse(StringUtils.isBlank("P"));
        assertFalse(StringUtils.isBlank(" test"));
    }

    @Test
    public void shouldGetDifferenceWithNullString() {
        // given/when/then
        assertThat(StringUtils.getDifference(null, "test"), equalTo(1.0));
        assertThat(StringUtils.getDifference("test", null), equalTo(1.0));
        assertThat(StringUtils.getDifference(null, null), equalTo(1.0));
    }

    @Test
    public void shouldGetDifferenceBetweenTwoString() {
        // given/when/then
        assertThat(StringUtils.getDifference("test", "taste"), equalTo(0.4));
        assertThat(StringUtils.getDifference("test", "bear"), equalTo(0.75));
        assertThat(StringUtils.getDifference("test", "something"), greaterThan(0.88));
    }

    @Test
    public void shouldCheckIfHasNeedleInWord() {
        // given/when/then
        assertThat(StringUtils.isInsideString('@', "@hello"), equalTo(false));
        assertThat(StringUtils.isInsideString('?', "absent"), equalTo(false));
        assertThat(StringUtils.isInsideString('-', "abcd-"), equalTo(false));
        assertThat(StringUtils.isInsideString('@', "hello@example"), equalTo(true));
        assertThat(StringUtils.isInsideString('@', "D@Z"), equalTo(true));
    }
}
