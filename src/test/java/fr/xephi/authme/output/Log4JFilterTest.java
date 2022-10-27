package fr.xephi.authme.output;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.when;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter.Result;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.message.Message;
import org.junit.Test;
import org.mockito.Mockito;

/**
 * Test for {@link Log4JFilter}.
 */
public class Log4JFilterTest {
	
	private final Log4JFilter log4JFilter = new Log4JFilter();
	
	private static final String SENSITIVE_COMMAND = "User issued server command: /login pass pass";
	private static final String NORMAL_COMMAND = "User issued server command: /help";
	private static final String OTHER_COMMAND = "Starting the server... Write /l for logs";

	// ---------
	// Test the filter(LogEvent) method
	// ---------
	@Test
	public void shouldFilterSensitiveLogEvent() {
		// given
		Message message = mockMessage(SENSITIVE_COMMAND);
		LogEvent event = Mockito.mock(LogEvent.class);
		when(event.getMessage()).thenReturn(message);
		
		// when
		Result result = log4JFilter.filter(event);
		
		// then
		assertThat(result, equalTo(Result.DENY));
	}
	
	@Test
	public void shouldNotFilterIrrelevantLogEvent() {
		// given
		Message message = mockMessage(NORMAL_COMMAND);
		LogEvent event = Mockito.mock(LogEvent.class);
		when(event.getMessage()).thenReturn(message);
		
		// when
		Result result = log4JFilter.filter(event);
		
		// then
		assertThat(result, equalTo(Result.NEUTRAL));
	}
	
	@Test
	public void shouldNotFilterNonCommandLogEvent() {
		// given
		Message message = mockMessage(OTHER_COMMAND);
		LogEvent event = Mockito.mock(LogEvent.class);
		when(event.getMessage()).thenReturn(message);
		
		// when
		Result result = log4JFilter.filter(event);
		
		// then
		assertThat(result, equalTo(Result.NEUTRAL));
	}
	
	@Test
	public void shouldNotFilterLogEventWithNullMessage() {
		// given
		Message message = mockMessage(null);
		LogEvent event = Mockito.mock(LogEvent.class);
		when(event.getMessage()).thenReturn(message);
		
		// when
		Result result = log4JFilter.filter(event);
		
		// then
		assertThat(result, equalTo(Result.NEUTRAL));
	}
	
	@Test
	public void shouldNotFilterWhenLogEventIsNull() {
		// given / when
		Result result = log4JFilter.filter(null);
		
		// then
		assertThat(result, equalTo(Result.NEUTRAL));
	}
	
	// ----------
	// Test filter(Logger, Level, Marker, String, Object...)
	// ----------
	@Test
	public void shouldFilterSensitiveStringMessage() {
		// given / when
		Result result = log4JFilter.filter(null, null, null, SENSITIVE_COMMAND);
		
		// then
		assertThat(result, equalTo(Result.DENY));
	}
	
	@Test
	public void shouldNotFilterNormalStringMessage() {
		// given / when
		Result result = log4JFilter.filter(null, null, null, NORMAL_COMMAND);
		
		// then
		assertThat(result, equalTo(Result.NEUTRAL));
	}
	
	@Test
	public void shouldNotFilterNonCommandStringMessage() {
		// given / when
		Result result = log4JFilter.filter(null, null, null, OTHER_COMMAND);
		
		// then
		assertThat(result, equalTo(Result.NEUTRAL));
	}
	
	@Test
	public void shouldReturnNeutralForNullMessage() {
		// given / when
		Result result = log4JFilter.filter(null, null, null, null);
		
		// then
		assertThat(result, equalTo(Result.NEUTRAL));
	}
	
	// --------
	// Test filter(Logger, Level, Marker, Object, Throwable)
	// --------
	@Test
	public void shouldFilterSensitiveObjectMessage() {
		// given / when
		Result result = log4JFilter.filter(null, null, null, (Object) SENSITIVE_COMMAND, new Exception());
		
		// then
		assertThat(result, equalTo(Result.DENY));
	}
	
	@Test
	public void shouldNotFilterNullObjectParam() {
		// given / when
		Result result = log4JFilter.filter(null, null, null, (Object) null, new Exception());
		
		// then
		assertThat(result, equalTo(Result.NEUTRAL));
	}
	
	@Test
	public void shouldNotFilterIrrelevantMessage() {
		// given / when
		Result result = log4JFilter.filter(null, null, null, (Object) OTHER_COMMAND, new Exception());
		
		// then
		assertThat(result, equalTo(Result.NEUTRAL));
	}
	
	@Test
	public void shouldNotFilterNonSensitiveCommand() {
		// given / when
		Result result = log4JFilter.filter(null, null, null, (Object) NORMAL_COMMAND, new Exception());
		
		// then
		assertThat(result, equalTo(Result.NEUTRAL));
	}
	
	// --------
	// Test filter(Logger, Level, Marker, Message, Throwable)
	// --------
	@Test
	public void shouldFilterSensitiveMessage() {
		// given
		Message message = mockMessage(SENSITIVE_COMMAND);
		
		// when
		Result result = log4JFilter.filter(null, null, null, message, new Exception());
		
		// then
		assertThat(result, equalTo(Result.DENY));
	}
	
	@Test
	public void shouldNotFilterNonSensitiveMessage() {
		// given
		Message message = mockMessage(NORMAL_COMMAND);
		
		// when
		Result result = log4JFilter.filter(null, null, null, message, new Exception());
		
		// then
		assertThat(result, equalTo(Result.NEUTRAL));
	}
	
	@Test
	public void shouldNotFilterNonCommandMessage() {
		// given
		Message message = mockMessage(OTHER_COMMAND);
		
		// when
		Result result = log4JFilter.filter(null, null, null, message, new Exception());
		
		// then
		assertThat(result, equalTo(Result.NEUTRAL));
	}
	
	@Test
	public void shouldNotFilterNullMessage() {
		// given / when
		Result result = log4JFilter.filter(null, null, null, (Object) null, new Exception());
		
		// then
		assertThat(result, equalTo(Result.NEUTRAL));
	}
	
	/**
	 * Mocks a {@link Message} object and makes it return the given formatted message.
	 *
	 * @param formattedMessage the formatted message the mock should return
	 * @return Message mock
     */
	private static Message mockMessage(String formattedMessage) {
		Message message = Mockito.mock(Message.class);
		when(message.getFormattedMessage()).thenReturn(formattedMessage);
		return message;
	}

}
