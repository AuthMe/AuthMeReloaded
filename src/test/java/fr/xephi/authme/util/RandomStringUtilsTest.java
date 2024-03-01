package fr.xephi.authme.util;

import org.junit.jupiter.api.Test;

import java.util.regex.Pattern;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Test for {@link RandomStringUtils}.
 */
class RandomStringUtilsTest {

    @Test
    void shouldGenerateRandomStrings() {
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
    void shouldGenerateRandomHexString() {
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
    void shouldGenerateRandomLowerUpperString() {
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
    void shouldGenerateRandomNumberString() {
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

    @Test
    void shouldThrowForInvalidLength() {
        // given / when / then
        assertThrows(IllegalArgumentException.class,
            () -> RandomStringUtils.generate(-3));
    }
}
