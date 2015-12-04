package fr.xephi.authme.output;

import fr.xephi.authme.util.StringUtils;

/**
 * Service class for the log filters.
 */
public final class LogFilterHelper {

    private static final String ISSUED_COMMAND_TEXT = "issued server command:";

    private static final String[] COMMANDS_TO_SKIP = {"/login ", "/l ", "/reg ", "/changepassword ",
        "/unregister ", "/authme register ", "/authme changepassword ", "/authme reg ", "/authme cp ",
        "/register "};

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
    public static boolean isSensitiveAuthMeCommand(String message) {
        if (message == null) {
            return false;
        }
        String lowerMessage = message.toLowerCase();
        return lowerMessage.contains(ISSUED_COMMAND_TEXT) && StringUtils.containsAny(lowerMessage, COMMANDS_TO_SKIP);
    }
}
