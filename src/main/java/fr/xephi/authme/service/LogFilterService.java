package fr.xephi.authme.service;

import fr.xephi.authme.command.CommandMapper;
import fr.xephi.authme.command.FoundCommandResult;

import javax.inject.Inject;
import java.util.Arrays;
import java.util.List;

/**
 * Service class for the log filters.
 */
public class LogFilterService {

    private static final String ISSUED_COMMAND_PREFIX_TEXT = "issued server command: /";

    @Inject
    private CommandMapper commandMapper;

    /**
     * Validate a message and return whether the message contains a sensitive AuthMe command.
     *
     * @param message The message to verify
     *
     * @return True if it is a sensitive AuthMe command, false otherwise
     */
    public boolean isSensitiveAuthMeCommand(String message) {
        if (message == null || !message.contains(ISSUED_COMMAND_PREFIX_TEXT)) {
            return false;
        }
        String rawCommand = message.substring(message.indexOf("/"));
        List<String> components = Arrays.asList(rawCommand.split(" "));
        FoundCommandResult command = commandMapper.mapPartsToCommand(null, components);
        switch (command.getResultStatus()) {
            case UNKNOWN_LABEL:
            case MISSING_BASE_COMMAND:
                return false;
            default:
                return command.getCommandDescription().isSensitive();
        }
    }
}
