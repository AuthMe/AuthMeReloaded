package fr.xephi.authme.command;

import fr.xephi.authme.util.CollectionUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Class responsible for mapping incoming arguments to a {@link CommandDescription}.
 */
public class CommandMapper {

    /**
     * Map incoming command parts to an actual command.
     *
     * @param parts The parts to process
     * @return The generated result
     */
    public FoundCommandResult mapPartsToCommand(final List<String> parts) {
        if (CollectionUtils.isEmpty(parts)) {
            return null; // TODO pass on the information that the base could not be mapped
        }

        CommandDescription base = getBaseCommand(parts.get(0));
        if (base == null) {
            return null; // TODO Pass on the information that base could not be mapped
        }

        //List<String> labels = CollectionUtils.getRange(parts, 0, 1);
        List<String> remaining = parts.subList(1, parts.size());

        // Prefer labels: /register help goes to "Help command", not "Register command" with 'help'
        CommandDescription childCommand = returnSuitableChild(base, remaining);
        if (childCommand != null) {
            // return childcommand: it's valid...
        }

        // No child command found, check if parent is suitable
        if (isSuitableArgumentCount(base, remaining.size())) {
            // return base... it's valid
        }

        // TODO: We don't have a suitable command for the given parts, so find the most similar one
        return null;

    }

    private static boolean isSuitableArgumentCount(CommandDescription command, int argumentCount) {
        int minArgs = CommandUtils.getMinNumberOfArguments(command);
        int maxArgs = CommandUtils.getMaxNumberOfArguments(command);

        return argumentCount >= minArgs && argumentCount <= maxArgs;
    }

    // Is the given command a suitable match for the given parts? parts is for example [changepassword, newpw, newpw]
    public CommandDescription returnSuitableChild(CommandDescription baseCommand, List<String> parts) {
        // TODO: Validate list + make case-insensitive
        final String label = parts.get(0).toLowerCase();
        final int argumentCount = parts.size() - 1;

        List<String> args = parts.subList(1, parts.size());
        for (CommandDescription child : baseCommand.getChildren()) {
            if (child.getLabels().contains(label) && isSuitableArgumentCount(child, argumentCount)) {
                return child;
            }
        }
        return null;
    }

    public CommandDescription getBaseCommand(String label) {
        String baseLabel = label.toLowerCase();
        for (CommandDescription command : CommandInitializer.getBaseCommands()) {
            if (command.getLabels().contains(baseLabel)) {
                return command;
            }
        }
        return null;
    }

}
