package fr.xephi.authme;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.message.Message;

import fr.xephi.authme.util.StringUtils;

/**
 * Implements a filter for Log4j to skip sensitive AuthMe commands.
 * @author Xephi59
 * @version $Revision: 1.0 $
 */
public class Log4JFilter implements org.apache.logging.log4j.core.Filter {
	
    /** List of commands (lower-case) to skip. */
    private static final String[] COMMANDS_TO_SKIP = { "/login ", "/l ", "/reg ", "/changepassword ",
        "/unregister ", "/authme register ", "/authme changepassword ", "/authme reg ", "/authme cp ", 
        "/register " };

    /** Constructor. */
    public Log4JFilter() {
    }

    /**
     * Method filter.
     * @param record LogEvent
     * @return Result
     * @see org.apache.logging.log4j.core.Filter#filter(LogEvent)
     */
    @Override
    public Result filter(LogEvent record) {
    	if (record == null) {
    		return Result.NEUTRAL;
    	}
    	return validateMessage(record.getMessage());
    }

    /**
     * Method filter.
     * @param arg0 Logger
     * @param arg1 Level
     * @param arg2 Marker
     * @param message String
     * @param arg4 Object[]
     * @return Result
     * @see org.apache.logging.log4j.core.Filter#filter(Logger, Level, Marker, String, Object[])
     */
    @Override
    public Result filter(Logger arg0, Level arg1, Marker arg2, String message,
            Object... arg4) {
    	return validateMessage(message);
    }

    /**
     * Method filter.
     * @param arg0 Logger
     * @param arg1 Level
     * @param arg2 Marker
     * @param message Object
     * @param arg4 Throwable
     * @return Result
     * @see org.apache.logging.log4j.core.Filter#filter(Logger, Level, Marker, Object, Throwable)
     */
    @Override
    public Result filter(Logger arg0, Level arg1, Marker arg2, Object message,
            Throwable arg4) {
    	if (message == null) {
    		return Result.NEUTRAL;
    	}
    	return validateMessage(message.toString());
    }

    /**
     * Method filter.
     * @param arg0 Logger
     * @param arg1 Level
     * @param arg2 Marker
     * @param message Message
     * @param arg4 Throwable
     * @return Result
     * @see org.apache.logging.log4j.core.Filter#filter(Logger, Level, Marker, Message, Throwable)
     */
    @Override
    public Result filter(Logger arg0, Level arg1, Marker arg2, Message message,
            Throwable arg4) {
    	return validateMessage(message);
    }

    /**
     * Method getOnMatch.
     * @return Result
     * @see org.apache.logging.log4j.core.Filter#getOnMatch()
     */
    @Override
    public Result getOnMatch() {
        return Result.NEUTRAL;
    }

    /**
     * Method getOnMismatch.
     * @return Result
     * @see org.apache.logging.log4j.core.Filter#getOnMismatch()
     */
    @Override
    public Result getOnMismatch() {
        return Result.NEUTRAL;
    }

	/**
	 * Validates a Message instance and returns the {@link Result} value
	 * depending depending on whether the message contains sensitive AuthMe
	 * data.
	 *
	 * @param message the Message object to verify
	
	 * @return the Result value */
    private static Result validateMessage(Message message) {
    	if (message == null) {
    		return Result.NEUTRAL;
    	}
    	return validateMessage(message.getFormattedMessage());
    }
    
	/**
	 * Validates a message and returns the {@link Result} value depending
	 * depending on whether the message contains sensitive AuthMe data.
	 *
	 * @param message the message to verify
	
	 * @return the Result value */
    private static Result validateMessage(String message) {
    	if (message == null) {
    		return Result.NEUTRAL;
    	}
    	
    	String lowerMessage = message.toLowerCase();
    	if (lowerMessage.contains("issued server command:") 
    			&& StringUtils.containsAny(lowerMessage, COMMANDS_TO_SKIP)) {
    		return Result.DENY;
    	}
    	return Result.NEUTRAL;
    }

}
