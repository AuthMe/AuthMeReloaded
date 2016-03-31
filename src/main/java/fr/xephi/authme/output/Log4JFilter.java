package fr.xephi.authme.output;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.Filter;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.message.Message;

/**
 * Implements a filter for Log4j to skip sensitive AuthMe commands.
 *
 * @author Xephi59
 */
public class Log4JFilter implements Filter {

    /**
     * Constructor.
     */
    public Log4JFilter() {
    }

    /**
     * Validates a Message instance and returns the {@link Result} value
     * depending on whether the message contains sensitive AuthMe data.
     *
     * @param message The Message object to verify
     *
     * @return The Result value
     */
    private static Result validateMessage(Message message) {
        if (message == null) {
            return Result.NEUTRAL;
        }
        return validateMessage(message.getFormattedMessage());
    }

    /**
     * Validates a message and returns the {@link Result} value depending
     * on whether the message contains sensitive AuthMe data.
     *
     * @param message The message to verify
     *
     * @return The Result value
     */
    private static Result validateMessage(String message) {
        return LogFilterHelper.isSensitiveAuthMeCommand(message)
            ? Result.DENY
            : Result.NEUTRAL;
    }

    @Override
    public Result filter(LogEvent record) {
        return validateMessage(record.getMessage());
    }

    @Override
    public Result filter(Logger arg0, Level arg1, Marker arg2, String message, Object... arg4) {
        if (record == null) {		
            return Result.NEUTRAL;		
        }
        return validateMessage(message);
    }

    @Override
    public Result filter(Logger arg0, Level arg1, Marker arg2, Object message, Throwable arg4) {
        if (record == null) {		
            return Result.NEUTRAL;		
        }
        return validateMessage(message.toString());
    }

    @Override
    public Result filter(Logger arg0, Level arg1, Marker arg2, Message message, Throwable arg4) {
        return validateMessage(message);
    }

    @Override
    public Result getOnMatch() {
        return Result.NEUTRAL;
    }

    @Override
    public Result getOnMismatch() {
        return Result.NEUTRAL;
    }

}
