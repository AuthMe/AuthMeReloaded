package fr.xephi.authme.output;

import com.google.common.annotations.VisibleForTesting;
import fr.xephi.authme.util.StringUtils;

/**
 * Service class for the log filters.
 */
final class LogFilterHelper {

    private static final String ISSUED_COMMAND_TEXT = "issued server command:";

    @VisibleForTesting
    static final String[] COMMANDS_TO_SKIP = {
        "/login ", "/l ", "/log ", "/register ", "/reg ", "/unregister ", "/unreg ",
        "/changepassword ", "/cp ", "/changepass ", "/authme register ",  "/authme reg ", "/authme r ",
        "/authme changepassword ", "/authme password ", "/authme changepass ", "/authme cp "
    };

    private LogFilterHelper() {
        // Util class
    }

    /**
     * Validate a message and return whether the message contains a sensitive AuthMe command.
     *
     * @param message The message to verify
     *
     * @return True if it is a sensitive AuthMe command, false otherwise
     */
    static boolean isSensitiveAuthMeCommand(String message) {
        if (message == null) {
            return false;
        }
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains(ISSUED_COMMAND_TEXT) && StringUtils.containsAny(lowerMessage, COMMANDS_TO_SKIP);
    }
}
