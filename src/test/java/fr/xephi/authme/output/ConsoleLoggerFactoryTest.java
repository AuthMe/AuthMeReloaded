package fr.xephi.authme.output;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.junit.After;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

/**
 * Test for {@link ConsoleLoggerFactory}.
 */
public class ConsoleLoggerFactoryTest {

    @BeforeClass
    public static void initLogger() {
        removeSettingsAndClearMap();
        TestHelper.setupLogger();
    }

    @After
    public void resetConsoleLoggerFactoryToDefaults() {
        removeSettingsAndClearMap();
    }

    private static void removeSettingsAndClearMap() {
        setSettings(null);
        getConsoleLoggerMap().clear();
    }

    @Test
    public void shouldCreateLoggerWithProperNameAndDefaultLogLevel() {
        // given / when
        ConsoleLogger logger = ConsoleLoggerFactory.get(AuthMe.class);

        // then
        assertThat(logger.getName(), equalTo("fr.xephi.authme.AuthMe"));
        assertThat(logger.getLogLevel(), equalTo(LogLevel.INFO));
        assertThat(getConsoleLoggerMap().keySet(), contains("fr.xephi.authme.AuthMe"));
    }

    @Test
    public void shouldReturnSameInstanceForName() {
        // given / when
        ConsoleLogger logger1 = ConsoleLoggerFactory.get(String.class);
        ConsoleLogger logger2 = ConsoleLoggerFactory.get(Number.class);
        ConsoleLogger logger3 = ConsoleLoggerFactory.get(String.class);

        // then
        assertThat(ConsoleLoggerFactory.getTotalLoggers(), equalTo(2));
        assertThat(logger3, sameInstance(logger1));
        assertThat(logger2, not(sameInstance(logger1)));
    }

    @Test
    public void shouldInitializeAccordingToSettings() {
        // given
        Settings settings = mock(Settings.class);
        given(settings.getProperty(PluginSettings.LOG_LEVEL)).willReturn(LogLevel.FINE);
        given(settings.getProperty(SecuritySettings.USE_LOGGING)).willReturn(false);
        ConsoleLogger existingLogger = ConsoleLoggerFactory.get(String.class);

        // when
        ConsoleLoggerFactory.reloadSettings(settings);
        ConsoleLogger newLogger = ConsoleLoggerFactory.get(AuthMe.class);

        // then
        assertThat(existingLogger.getLogLevel(), equalTo(LogLevel.FINE));
        assertThat(newLogger.getLogLevel(), equalTo(LogLevel.FINE));
    }

    private static void setSettings(Settings settings) {
        ReflectionTestUtils.setField(ConsoleLoggerFactory.class, null, "settings", settings);
    }

    private static Map<String, ConsoleLogger> getConsoleLoggerMap() {
        return ReflectionTestUtils.getFieldValue(ConsoleLoggerFactory.class, null, "consoleLoggers");
    }
}
