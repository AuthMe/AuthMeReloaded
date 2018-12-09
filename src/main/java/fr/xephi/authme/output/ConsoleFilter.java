package fr.xephi.authme.output;

import fr.xephi.authme.service.LogFilterService;

import java.util.logging.Filter;
import java.util.logging.LogRecord;

/**
 * Console filter to replace sensitive AuthMe commands with a generic message.
 *
 * @author Xephi59
 */
public class ConsoleFilter implements Filter {

    private LogFilterService filterService;

    public ConsoleFilter(LogFilterService filterService) {
        this.filterService = filterService;
    }

    @Override
    public boolean isLoggable(LogRecord record) {
        if (record == null || record.getMessage() == null) {
            return true;
        }

        if (filterService.isSensitiveAuthMeCommand(record.getMessage())) {
            String playerName = record.getMessage().split(" ")[0];
            record.setMessage(playerName + " issued an AuthMe command");
        }
        return true;
    }

}
