package fr.xephi.authme;

import fr.xephi.authme.output.LogLevel;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Test for {@link ConsoleLogger}.
 */
@RunWith(MockitoJUnitRunner.class)
public class ConsoleLoggerTest {

    @Mock
    private Logger logger;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File logFile;

    @Before
    public void setMockLogger() throws IOException {
        ConsoleLogger.setLogger(logger);
        File folder = temporaryFolder.newFolder();
        File logFile = new File(folder, "authme.log");
        if (!logFile.createNewFile()) {
            throw new IOException("Could not create file '" + logFile.getPath() + "'");
        }
        ConsoleLogger.setLogFile(logFile);
        this.logFile = logFile;
    }

    @After
    public void closeFileHandlers() {
        ConsoleLogger.close();
    }

    /**
     * Resets the ConsoleLogger back to its defaults after running all tests. Especially important
     * is that we no longer enable logging to a file as the log file we've supplied will no longer
     * be around after this test class has finished.
     */
    @AfterClass
    public static void resetConsoleToDefault() {
        ConsoleLogger.setLoggingOptions(newSettings(false, LogLevel.FINE));
    }

    @Test
    public void shouldLogToFile() throws IOException {
        // given
        ConsoleLogger.setLoggingOptions(newSettings(true, LogLevel.FINE));

        // when
        ConsoleLogger.fine("Logging a FINE message");
        ConsoleLogger.debug("Logging a DEBUG message");
        ConsoleLogger.info("This is an INFO message");

        // then
        verify(logger, times(2)).info(anyString());
        verifyNoMoreInteractions(logger);
        List<String> loggedLines = Files.readAllLines(logFile.toPath(), StandardCharsets.UTF_8);
        assertThat(loggedLines, hasSize(2));
        assertThat(loggedLines.get(0), containsString("[FINE] Logging a FINE message"));
        assertThat(loggedLines.get(1), containsString("[INFO] This is an INFO message"));
    }

    @Test
    public void shouldNotLogToFile() throws IOException {
        // given
        ConsoleLogger.setLoggingOptions(newSettings(false, LogLevel.DEBUG));

        // when
        ConsoleLogger.debug("Created test");
        ConsoleLogger.warning("Encountered a warning");

        // then
        verify(logger).info("[DEBUG] Created test");
        verify(logger).warning("Encountered a warning");
        verifyNoMoreInteractions(logger);
        assertThat(logFile.length(), equalTo(0L));
    }

    @Test
    public void shouldLogStackTraceToFile() throws IOException {
        // given
        ConsoleLogger.setLoggingOptions(newSettings(true, LogLevel.INFO));
        Exception e = new IllegalStateException("Test exception message");

        // when
        ConsoleLogger.info("Info text");
        ConsoleLogger.debug("Debug message");
        ConsoleLogger.fine("Fine-level message");
        ConsoleLogger.logException("Exception occurred:", e);

        // then
        verify(logger).info("Info text");
        verify(logger).warning("Exception occurred: [IllegalStateException]: Test exception message");
        verifyNoMoreInteractions(logger);
        List<String> loggedLines = Files.readAllLines(logFile.toPath(), StandardCharsets.UTF_8);
        assertThat(loggedLines.size(), greaterThan(3));
        assertThat(loggedLines.get(0), containsString("[INFO] Info text"));
        assertThat(loggedLines.get(1),
            containsString("[WARN] Exception occurred: [IllegalStateException]: Test exception message"));
        // Check that we have this class' full name somewhere in the file -> stacktrace of Exception e
        assertThat(String.join("", loggedLines), containsString(getClass().getCanonicalName()));
    }

    @Test
    public void shouldSupportVariousDebugMethods() throws IOException {
        // given
        ConsoleLogger.setLoggingOptions(newSettings(true, LogLevel.DEBUG));

        // when
        ConsoleLogger.debug("Got {0} entries", 17);
        ConsoleLogger.debug("Player `{0}` is in world `{1}`", "Bobby", new World("world"));
        ConsoleLogger.debug("{0} quick {1} jump over {2} lazy {3} (reason: {4})", 5, "foxes", 3, "dogs", null);
        ConsoleLogger.debug(() -> "Too little too late");

        // then
        verify(logger).log(Level.INFO, "[DEBUG] Got {0} entries", 17);
        verify(logger).log(Level.INFO, "[DEBUG] Player `{0}` is in world `{1}`", new Object[]{"Bobby", new World("world")});
        verify(logger).log(Level.INFO, "[DEBUG] {0} quick {1} jump over {2} lazy {3} (reason: {4})",
            new Object[]{5, "foxes", 3, "dogs", null});
        verify(logger).info("[DEBUG] Too little too late");

        List<String> loggedLines = Files.readAllLines(logFile.toPath(), StandardCharsets.UTF_8);
        assertThat(loggedLines, contains(
            containsString("[DEBUG] Got {0} entries {17}"),
            containsString("[DEBUG] Player `{0}` is in world `{1}` {Bobby, w[world]}"),
            containsString("[DEBUG] {0} quick {1} jump over {2} lazy {3} (reason: {4}) {5, foxes, 3, dogs, null}"),
            containsString("[DEBUG] Too little too late")));
    }

    @Test
    public void shouldHaveHiddenConstructor() {
        TestHelper.validateHasOnlyPrivateEmptyConstructor(ConsoleLogger.class);
    }

    private static Settings newSettings(boolean logToFile, LogLevel logLevel) {
        Settings settings = mock(Settings.class);
        given(settings.getProperty(SecuritySettings.USE_LOGGING)).willReturn(logToFile);
        given(settings.getProperty(PluginSettings.LOG_LEVEL)).willReturn(logLevel);
        return settings;
    }

    private static final class World {
        private final String name;

        World(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "w[" + name + "]";
        }

        @Override
        public boolean equals(Object other) {
            if (other instanceof World) {
                return Objects.equals(this.name, ((World) other).name);
            }
            return false;
        }
    }
}
