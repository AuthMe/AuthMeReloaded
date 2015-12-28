package fr.xephi.authme.security;

import org.junit.Test;

import java.util.regex.Pattern;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

/**
 * Test for {@link RandomString}.
 */
public class RandomStringTest {

    @Test
    public void shouldGenerateRandomStrings() {
        // given
        int[] lengths = {0, 1, 19, 142, 1872};
        Pattern badChars = Pattern.compile(".*[^0-9a-z].*");

        // when / then
        for (int length : lengths) {
            String result = RandomString.generate(length);
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
            String result = RandomString.generateHex(length);
            assertThat("Result '" + result + "' should have length " + length,
                result.length(), equalTo(length));
            assertThat("Result '" + result + "' should only have characters a-f, 0-9",
                badChars.matcher(result).matches(), equalTo(false));
        }
    }

    @Test(expected = IllegalArgumentException.class)
    public void shouldThrowForInvalidLength() {
        // given/when
        RandomString.generate(-3);

        // then - throw exception
    }

}
