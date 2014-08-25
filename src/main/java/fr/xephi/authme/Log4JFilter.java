package fr.xephi.authme;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.Marker;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.Logger;
import org.apache.logging.log4j.message.Message;

/**
 *
 * @author Xephi59
 */
public class Log4JFilter implements org.apache.logging.log4j.core.Filter {

    public Log4JFilter() {
    }

    @Override
    public Result filter(LogEvent record) {
        try {
            if (record == null || record.getMessage() == null)
                return Result.NEUTRAL;
            String logM = record.getMessage().getFormattedMessage().toLowerCase();
            if (!logM.contains("issued server command:"))
                return Result.NEUTRAL;
            if (!logM.contains("/login ") && !logM.contains("/l ") && !logM.contains("/reg ") && !logM.contains("/changepassword ") && !logM.contains("/unregister ") && !logM.contains("/authme register ") && !logM.contains("/authme changepassword ") && !logM.contains("/authme reg ") && !logM.contains("/authme cp ") && !logM.contains("/register "))
                return Result.NEUTRAL;
            return Result.DENY;
        } catch (NullPointerException npe) {
            return Result.NEUTRAL;
        }
    }

    @Override
    public Result filter(Logger arg0, Level arg1, Marker arg2, String message,
            Object... arg4) {
        try {
            if (message == null)
                return Result.NEUTRAL;
            String logM = message.toLowerCase();
            if (!logM.contains("issued server command:"))
                return Result.NEUTRAL;
            if (!logM.contains("/login ") && !logM.contains("/l ") && !logM.contains("/reg ") && !logM.contains("/changepassword ") && !logM.contains("/unregister ") && !logM.contains("/authme register ") && !logM.contains("/authme changepassword ") && !logM.contains("/authme reg ") && !logM.contains("/authme cp ") && !logM.contains("/register "))
                return Result.NEUTRAL;
            return Result.DENY;
        } catch (NullPointerException npe) {
            return Result.NEUTRAL;
        }
    }

    @Override
    public Result filter(Logger arg0, Level arg1, Marker arg2, Object message,
            Throwable arg4) {
        try {
            if (message == null)
                return Result.NEUTRAL;
            String logM = message.toString().toLowerCase();
            if (!logM.contains("issued server command:"))
                return Result.NEUTRAL;
            if (!logM.contains("/login ") && !logM.contains("/l ") && !logM.contains("/reg ") && !logM.contains("/changepassword ") && !logM.contains("/unregister ") && !logM.contains("/authme register ") && !logM.contains("/authme changepassword ") && !logM.contains("/authme reg ") && !logM.contains("/authme cp ") && !logM.contains("/register "))
                return Result.NEUTRAL;
            return Result.DENY;
        } catch (NullPointerException npe) {
            return Result.NEUTRAL;
        }
    }

    @Override
    public Result filter(Logger arg0, Level arg1, Marker arg2, Message message,
            Throwable arg4) {
        try {
            if (message == null)
                return Result.NEUTRAL;
            String logM = message.getFormattedMessage().toLowerCase();
            if (!logM.contains("issued server command:"))
                return Result.NEUTRAL;
            if (!logM.contains("/login ") && !logM.contains("/l ") && !logM.contains("/reg ") && !logM.contains("/changepassword ") && !logM.contains("/unregister ") && !logM.contains("/authme register ") && !logM.contains("/authme changepassword ") && !logM.contains("/authme reg ") && !logM.contains("/authme cp ") && !logM.contains("/register "))
                return Result.NEUTRAL;
            return Result.DENY;
        } catch (NullPointerException npe) {
            return Result.NEUTRAL;
        }
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
