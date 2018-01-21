package fr.xephi.authme.command.help;

import com.google.common.base.CaseFormat;
import fr.xephi.authme.command.CommandArgumentDescription;
import fr.xephi.authme.command.CommandDescription;
import fr.xephi.authme.command.CommandUtils;
import fr.xephi.authme.message.HelpMessagesFileHandler;
import fr.xephi.authme.permission.DefaultPermission;

import javax.inject.Inject;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Manages translatable help messages.
 */
public class HelpMessagesService {

    private static final String COMMAND_PREFIX = "commands.";
    private static final String DESCRIPTION_SUFFIX = ".description";
    private static final String DETAILED_DESCRIPTION_SUFFIX = ".detailedDescription";
    private static final String DEFAULT_PERMISSIONS_PATH = "common.defaultPermissions.";

    private final HelpMessagesFileHandler helpMessagesFileHandler;

    @Inject
    HelpMessagesService(HelpMessagesFileHandler helpMessagesFileHandler) {
        this.helpMessagesFileHandler = helpMessagesFileHandler;
    }

    /**
     * Creates a copy of the supplied command description with localized messages where present.
     *
     * @param command the command to build a localized version of
     * @return the localized description
     */
    public CommandDescription buildLocalizedDescription(CommandDescription command) {
        final String path = COMMAND_PREFIX + getCommandSubPath(command);
        if (!helpMessagesFileHandler.hasSection(path)) {
            // Messages file does not have a section for this command - return the provided command
            return command;
        }

        CommandDescription.CommandBuilder builder = CommandDescription.builder()
            .description(getText(path + DESCRIPTION_SUFFIX, command::getDescription))
            .detailedDescription(getText(path + DETAILED_DESCRIPTION_SUFFIX, command::getDetailedDescription))
            .executableCommand(command.getExecutableCommand())
            .parent(command.getParent())
            .labels(command.getLabels())
            .permission(command.getPermission());

        int i = 1;
        for (CommandArgumentDescription argument : command.getArguments()) {
            String argPath = path + ".arg" + i;
            String label = getText(argPath + ".label", argument::getName);
            String description = getText(argPath + ".description", argument::getDescription);
            builder.withArgument(label, description, argument.isOptional());
            ++i;
        }

        CommandDescription localCommand = builder.build();
        localCommand.getChildren().addAll(command.getChildren());
        return localCommand;
    }

    public String getDescription(CommandDescription command) {
        return getText(COMMAND_PREFIX + getCommandSubPath(command) + DESCRIPTION_SUFFIX, command::getDescription);
    }

    public String getMessage(HelpMessage message) {
        return helpMessagesFileHandler.getMessage(message.getKey());
    }

    public String getMessage(HelpSection section) {
        return helpMessagesFileHandler.getMessage(section.getKey());
    }

    public String getMessage(DefaultPermission defaultPermission) {
        // e.g. {default_permissions_path}.opOnly for DefaultPermission.OP_ONLY
        String path = DEFAULT_PERMISSIONS_PATH + getDefaultPermissionsSubPath(defaultPermission);
        return helpMessagesFileHandler.getMessage(path);
    }

    public static String getDefaultPermissionsSubPath(DefaultPermission defaultPermission) {
        return CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, defaultPermission.name());
    }

    private String getText(String path, Supplier<String> defaultTextGetter) {
        String message = helpMessagesFileHandler.getMessageIfExists(path);
        return message == null
            ? defaultTextGetter.get()
            : message;
    }


    /**
     * Triggers a reload of the help messages file. Note that this method is not needed
     * to be called for /authme reload.
     */
    public void reloadMessagesFile() {
        helpMessagesFileHandler.reload();
    }

    /**
     * Returns the command subpath for the given command (i.e. the path to the translations for the given
     * command under "commands").
     *
     * @param command the command to process
     * @return the subpath for the command's texts
     */
    public static String getCommandSubPath(CommandDescription command) {
        return CommandUtils.constructParentList(command)
            .stream()
            .map(cmd -> cmd.getLabels().get(0))
            .collect(Collectors.joining("."));
    }
}
