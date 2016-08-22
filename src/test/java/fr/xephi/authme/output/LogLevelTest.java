package fr.xephi.authme.output;

import org.junit.Test;

import static java.lang.String.format;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link LogLevel}.
 */
public class LogLevelTest {

    @Test
    public void shouldIncludeProperLevels() {
        checkLevelInclusion(LogLevel.INFO, true, false, false);
        checkLevelInclusion(LogLevel.FINE, true, true, false);
        checkLevelInclusion(LogLevel.DEBUG, true, true, true);
    }

    private void checkLevelInclusion(LogLevel level, boolean... expectedValues) {
        LogLevel[] levels = LogLevel.values();
        assertThat("Number of expected values corresponds to number of log levels",
            expectedValues.length, equalTo(levels.length));
        for (int i = 0; i < levels.length; ++i) {
            assertThat(format("%s.includes(%s) should be %b", level, levels[i], expectedValues[i]),
                level.includes(levels[i]), equalTo(expectedValues[i]));
        }
    }

}
