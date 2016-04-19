package fr.xephi.authme.output;

import org.junit.Test;
import org.mockito.Mockito;

import java.util.logging.LogRecord;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for {@link ConsoleFilter}.
 */
public class ConsoleFilterTest {

    private final ConsoleFilter filter = new ConsoleFilter();

    private static final String SENSITIVE_COMMAND = "User issued server command: /login test test";
    private static final String NORMAL_COMMAND = "User issued server command: /motd 2";


    @Test
    public void shouldReplaceSensitiveRecord() {
        // given
        LogRecord record = createRecord(SENSITIVE_COMMAND);

        // when
        boolean result = filter.isLoggable(record);

        // then
        assertThat(result, equalTo(true));
        verify(record).setMessage("User issued an AuthMe command");
    }

    @Test
    public void shouldNotFilterRegularCommand() {
        // given
        LogRecord record = createRecord(NORMAL_COMMAND);

        // when
        boolean result = filter.isLoggable(record);

        // then
        assertThat(result, equalTo(true));
        verify(record, never()).setMessage("User issued an AuthMe command");
    }

    @Test
    public void shouldManageRecordWithNullMessage() {
        // given
        LogRecord record = createRecord(null);

        // when
        boolean result = filter.isLoggable(record);

        // then
        assertThat(result, equalTo(true));
        verify(record, never()).setMessage("User issued an AuthMe command");
    }


    /**
     * Creates a mock of {@link LogRecord} and sets it to return the given message.
     *
     * @param message The message to set.
     *
     * @return Mock of LogRecord
     */
    private static LogRecord createRecord(String message) {
        LogRecord record = Mockito.mock(LogRecord.class);
        when(record.getMessage()).thenReturn(message);
        return record;
    }
}
