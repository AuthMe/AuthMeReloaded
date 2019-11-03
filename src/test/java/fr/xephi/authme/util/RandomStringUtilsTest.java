package fr.xephi.authme.util;

import org.junit.Test;

import java.util.regex.Pattern;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link RandomStringUtils}.
 */
public class RandomStringUtilsTest {

    @Test
    public void shouldGenerateRandomStrings() {
        // given
        int[] lengths = {0, 1, 19, 142, 1872};
        Pattern badChars = Pattern.compile(".*[^0-9a-z].*");

        // when / then
        for (int length : lengths) {
            String result = RandomStringUtils.generate(length);
            assertThat("Result '" + result + "' should have length " + length,
                result.length(), equalTo(length));
            assertThat("Result '" + result + "' should only have characters a-z, 0-9",
                badChars.matcher(result).matches(), equalTo(false));
        }
    }

    @Test
    public void shouldGenerateRandomHexString() {
        // given
        int[] lengths = {0, 1, 21, 160, 1784};
        Pattern badChars = Pattern.compile(".*[^0-9a-f].*");

        // when / then
        for (int length : lengths) {
            String result = RandomStringUtils.generateHex(length);
            assertThat("Result '" + result + "' should have length " + length,
                result.length(), equalTo(length));
            assertThat("Result '" + result + "' should only have characters a-f, 0-9",
                badChars.matcher(result).matches(), equalTo(false));
        }
    }

    @Test
    public void shouldGenerateRandomLowerUpperString() {
        // given
        int[] lengths = {0, 1, 17, 143, 1808};
        Pattern badChars = Pattern.compile(".*[^0-9a-zA-Z].*");

        // when / then
        for (int length : lengths) {
            String result = RandomStringUtils.generateLowerUpper(length);
            assertThat("Result '" + result + "' should have length " + length,
                result.length(), equalTo(length));
            assertThat("Result '" + result + "' should only have characters a-z, A-Z, 0-9",
                badChars.matcher(result).matches(), equalTo(false));
        }
    }

    @Test
    public void shouldGenerateRandomNumberString() {
        // given
        int[] lengths = {0, 1, 18, 147, 1833};
        Pattern badChars = Pattern.compile(".*[^0-9].*");

        // when / then
        for (int length : lengths) {
            String result = RandomStringUtils.generateNum(length);
            assertThat("Result '" + result + "' should have length " + length,
                result.length(), equalTo(length));
            assertThat("Result '" + result + "' should only have characters 0-9",
                badChars.matcher(result).matches(), equalTo(false));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowForInvalidLength() {
        // given/when
        RandomStringUtils.generate(-3);

        // then - throw exception
    }
}
