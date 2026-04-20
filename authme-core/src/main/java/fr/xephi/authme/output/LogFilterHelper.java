package fr.xephi.authme.output;

import com.google.common.annotations.VisibleForTesting;
import fr.xephi.authme.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

/**
 * Service class for the log filters.
 */
final class LogFilterHelper {

    @VisibleForTesting
    static final List<String> COMMANDS_TO_SKIP = withAndWithoutAuthMePrefix(
        "/login ", "/l ", "/log ", "/register ", "/reg ", "/unregister ", "/unreg ",
        "/changepassword ", "/cp ", "/changepass ", "/authme register ",  "/authme reg ", "/authme r ",
        "/authme changepassword ", "/authme password ", "/authme changepass ", "/authme cp ", "/email setpassword ");

    private static final String ISSUED_COMMAND_TEXT = "issued server command:";

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
        String lowerMessage = message.toLowerCase(Locale.ROOT);
        return lowerMessage.contains(ISSUED_COMMAND_TEXT) && StringUtils.containsAny(lowerMessage, COMMANDS_TO_SKIP);
    }

    private static List<String> withAndWithoutAuthMePrefix(String... commands) {
        List<String> commandList = new ArrayList<>(commands.length * 2);
        for (String command : commands) {
            commandList.add(command);
            commandList.add(command.substring(0, 1) + "authme:" + command.substring(1));
        }
        return Collections.unmodifiableList(commandList);
    }
}
