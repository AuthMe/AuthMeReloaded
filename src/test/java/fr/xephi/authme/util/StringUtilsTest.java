package fr.xephi.authme.util;

import fr.xephi.authme.TestHelper;
import org.junit.Test;

import java.net.MalformedURLException;

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

    @Test
    public void shouldCheckIsEmptyUtil() {
        // Should be true for null/empty/whitespace
        assertTrue(StringUtils.isEmpty(null));
        assertTrue(StringUtils.isEmpty(""));
        assertTrue(StringUtils.isEmpty(" \t"));

        // Should be false if string has content
        assertFalse(StringUtils.isEmpty("P"));
        assertFalse(StringUtils.isEmpty(" test"));
    }

    @Test
    public void shouldFormatException() {
        // given
        MalformedURLException ex = new MalformedURLException("Unrecognized URL format");

        // when
        String result = StringUtils.formatException(ex);

        // then
        assertThat(result, equalTo("[MalformedURLException]: Unrecognized URL format"));
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
    public void shouldHaveHiddenConstructor() {
        TestHelper.validateHasOnlyPrivateEmptyConstructor(StringUtils.class);
    }
}
