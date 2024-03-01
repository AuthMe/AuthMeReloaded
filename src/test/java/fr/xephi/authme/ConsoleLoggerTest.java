package fr.xephi.authme;

import fr.xephi.authme.output.LogLevel;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PluginSettings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.List;
import java.util.logging.Logger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Test for {@link ConsoleLogger}.
 */
@ExtendWith(MockitoExtension.class)
class ConsoleLoggerTest {

    private ConsoleLogger consoleLogger;

    @Mock
    private Logger logger;

    @TempDir
    File tempFolder;

    private File logFile;

    @BeforeEach
    void setMockLogger() throws IOException {
        File logFile = new File(tempFolder, "authme.log");
        if (!logFile.createNewFile()) {
            throw new IOException("Could not create file '" + logFile.getPath() + "'");
        }
        ConsoleLogger.initialize(logger, logFile);
        this.logFile = logFile;
        this.consoleLogger = new ConsoleLogger("test");
    }

    @AfterEach
    void closeFileHandlers() {
        ConsoleLogger.closeFileWriter();
    }

    /**
     * Resets the ConsoleLogger back to its defaults after running all tests. Especially important
     * is that we no longer enable logging to a file as the log file we've supplied will no longer
     * be around after this test class has finished.
     */
    @AfterAll
    static void resetConsoleToDefault() {
        ConsoleLogger.initializeSharedSettings(newSettings(false, LogLevel.INFO));
    }

    @Test
    void shouldLogToFile() throws IOException {
        // given
        Settings settings = newSettings(true, LogLevel.FINE);
        ConsoleLogger.initializeSharedSettings(settings);
        consoleLogger.initializeSettings(settings);

        // when
        consoleLogger.fine("Logging a FINE message");
        consoleLogger.debug("Logging a DEBUG message");
        consoleLogger.info("This is an INFO message");

        // then
        verify(logger, times(2)).info(anyString());
        verifyNoMoreInteractions(logger);
        List<String> loggedLines = Files.readAllLines(logFile.toPath(), StandardCharsets.UTF_8);
        assertThat(loggedLines, hasSize(2));
        assertThat(loggedLines.get(0), containsString("[FINE] Logging a FINE message"));
        assertThat(loggedLines.get(1), containsString("[INFO] This is an INFO message"));
    }

    @Test
    void shouldNotLogToFile() {
        // given
        Settings settings = newSettings(false, LogLevel.DEBUG);
        ConsoleLogger.initializeSharedSettings(settings);
        consoleLogger.initializeSettings(settings);

        // when
        consoleLogger.debug("Created test");
        consoleLogger.warning("Encountered a warning");

        // then
        verify(logger).info("[DEBUG] Created test");
        verify(logger).warning("Encountered a warning");
        verifyNoMoreInteractions(logger);
        assertThat(logFile.length(), equalTo(0L));
    }

    @Test
    void shouldLogStackTraceToFile() throws IOException {
        // given
        Settings settings = mock(Settings.class);
        given(settings.getProperty(SecuritySettings.USE_LOGGING)).willReturn(true);
        ConsoleLogger.initializeSharedSettings(settings);
        Exception e = new IllegalStateException("Test exception message");

        // when
        consoleLogger.info("Info text");
        consoleLogger.debug("Debug message");
        consoleLogger.fine("Fine-level message");
        consoleLogger.logException("Exception occurred:", e);

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
    void shouldSupportVariousDebugMethods() throws IOException {
        // given
        Settings settings = newSettings(true, LogLevel.DEBUG);
        ConsoleLogger.initializeSharedSettings(settings);
        consoleLogger.initializeSettings(settings);

        // when
        consoleLogger.debug("Got {0} entries", 17);
        consoleLogger.debug("Player `{0}` is in world `{1}`", "Bobby", new WorldDummy("world"));
        consoleLogger.debug("{0} quick {1} jump over {2} lazy {3} (reason: {4})", 5, "foxes", 3, "dogs", null);
        consoleLogger.debug(() -> "Too little too late");

        // then
        verify(logger).info("[DEBUG] Got 17 entries");
        verify(logger).info("[DEBUG] Player `Bobby` is in world `w[world]`");
        verify(logger).info("[DEBUG] 5 quick foxes jump over 3 lazy dogs (reason: null)");
        verify(logger).info("[DEBUG] Too little too late");
        verifyNoMoreInteractions(logger);

        List<String> loggedLines = Files.readAllLines(logFile.toPath(), StandardCharsets.UTF_8);
        assertThat(loggedLines, contains(
            containsString("[DEBUG] Got 17 entries"),
            containsString("[DEBUG] Player `Bobby` is in world `w[world]`"),
            containsString("[DEBUG] 5 quick foxes jump over 3 lazy dogs (reason: null)"),
            containsString("[DEBUG] Too little too late")));
    }

    @Test
    void shouldCloseFileWriterDespiteExceptionOnFlush() throws IOException {
        // given
        FileWriter fileWriter = mock(FileWriter.class);
        doThrow(new IOException("Error during flush")).when(fileWriter).flush();
        ReflectionTestUtils.setField(ConsoleLogger.class, null, "fileWriter", fileWriter);

        // when
        ConsoleLogger.closeFileWriter();

        // then
        verify(fileWriter).flush();
        verify(fileWriter).close();
        assertThat(ReflectionTestUtils.getFieldValue(ConsoleLogger.class, null, "fileWriter"), nullValue());
    }

    @Test
    void shouldHandleExceptionOnFileWriterClose() throws IOException {
        // given
        FileWriter fileWriter = mock(FileWriter.class);
        doThrow(new IOException("Cannot close")).when(fileWriter).close();
        ReflectionTestUtils.setField(ConsoleLogger.class, null, "fileWriter", fileWriter);

        // when
        ConsoleLogger.closeFileWriter();

        // then
        verify(fileWriter).flush();
        verify(fileWriter).close();
        assertThat(ReflectionTestUtils.getFieldValue(ConsoleLogger.class, null, "fileWriter"), nullValue());
    }

    private static Settings newSettings(boolean logToFile, LogLevel logLevel) {
        Settings settings = mock(Settings.class);
        given(settings.getProperty(SecuritySettings.USE_LOGGING)).willReturn(logToFile);
        given(settings.getProperty(PluginSettings.LOG_LEVEL)).willReturn(logLevel);
        return settings;
    }

    private static final class WorldDummy {
        private final String name;

        WorldDummy(String name) {
            this.name = name;
        }

        @Override
        public String toString() {
            return "w[" + name + "]";
        }
    }
}
