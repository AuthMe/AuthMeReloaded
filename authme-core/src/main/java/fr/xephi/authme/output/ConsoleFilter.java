package fr.xephi.authme.output;

import java.util.logging.Filter;
import java.util.logging.LogRecord;

/**
 * Console filter to replace sensitive AuthMe commands with a generic message.
 *
 * @author Xephi59
 */
public class ConsoleFilter implements Filter {

    @Override
    public boolean isLoggable(LogRecord record) {
        if (record == null || record.getMessage() == null) {
            return true;
        }

        if (LogFilterHelper.isSensitiveAuthMeCommand(record.getMessage())) {
            String playerName = record.getMessage().split(" ")[0];
            record.setMessage(playerName + " issued an AuthMe command");
        }
        return true;
    }

}
