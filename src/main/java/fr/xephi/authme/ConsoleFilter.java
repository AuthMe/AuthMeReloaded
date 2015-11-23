package fr.xephi.authme;

import java.util.logging.Filter;
import java.util.logging.LogRecord;

/**
 * Console filter Class
 *
 * @author Xephi59
 * @version $Revision: 1.0 $
 */
public class ConsoleFilter implements Filter {

    public ConsoleFilter() {
    }

    /**
     * Method isLoggable.
     *
     * @param record LogRecord
     * @return boolean * @see java.util.logging.Filter#isLoggable(LogRecord)
     */
    @Override
    public boolean isLoggable(LogRecord record) {
        try {
            if (record == null || record.getMessage() == null)
                return true;
            String logM = record.getMessage().toLowerCase();
            if (!logM.contains("issued server command:"))
                return true;
            if (!logM.contains("/login ") && !logM.contains("/l ") && !logM.contains("/reg ") && !logM.contains("/changepassword ") && !logM.contains("/unregister ") && !logM.contains("/authme register ") && !logM.contains("/authme changepassword ") && !logM.contains("/authme reg ") && !logM.contains("/authme cp ") && !logM.contains("/register "))
                return true;
            String playerName = record.getMessage().split(" ")[0];
            record.setMessage(playerName + " issued an AuthMe command!");
            return true;
        } catch (NullPointerException npe) {
            return true;
        }
    }

}
