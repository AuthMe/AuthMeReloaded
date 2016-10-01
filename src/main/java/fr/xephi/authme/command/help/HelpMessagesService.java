package fr.xephi.authme.command.help;

import com.google.common.base.CaseFormat;
import fr.xephi.authme.command.CommandArgumentDescription;
import fr.xephi.authme.command.CommandDescription;
import fr.xephi.authme.command.CommandUtils;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.initialization.Reloadable;
import fr.xephi.authme.permission.DefaultPermission;
import fr.xephi.authme.util.FileUtils;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.inject.Inject;
import java.io.File;
import java.util.function.Supplier;
import java.util.stream.Collectors;

/**
 * Manages translatable help messages.
 */
public class HelpMessagesService implements Reloadable {

    private static final String COMMAND_PREFIX = "command.";
    private static final String DESCRIPTION_SUFFIX = ".description";
    private static final String DETAILED_DESCRIPTION_SUFFIX = ".detailedDescription";
    private static final String DEFAULT_PERMISSIONS_PATH = "common.defaultPermissions.";

    private final File dataFolder;
    // FIXME: Make configurable
    private String file = "messages/help_en.yml";
    private FileConfiguration fileConfiguration;

    @Inject
    HelpMessagesService(@DataFolder File dataFolder) {
        File messagesFile = new File(dataFolder, "messages/help_en.yml");
        if (!FileUtils.copyFileFromResource(messagesFile, file)) {
            throw new IllegalStateException("Could not copy help message");
        }
        this.dataFolder = dataFolder;
        fileConfiguration = YamlConfiguration.loadConfiguration(messagesFile);
    }

    /**
     * Creates a copy of the supplied command description with localized messages where present.
     *
     * @param command the command to build a localized version of
     * @return the localized description
     */
    public CommandDescription buildLocalizedDescription(CommandDescription command) {
        final String path = getCommandPath(command);
        if (fileConfiguration.get(path) == null) {
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

        return builder.build();
    }

    public String getMessage(HelpMessageKey key) {
        String message = fileConfiguration.getString(key.getKey());
        return message == null
            ? key.getFallback()
            : message;
    }

    public String getMessage(DefaultPermission defaultPermission) {
        // e.g. {default_permissions_path}.opOnly for DefaultPermission.OP_ONLY
        String path = DEFAULT_PERMISSIONS_PATH +
            CaseFormat.UPPER_UNDERSCORE.to(CaseFormat.LOWER_CAMEL, defaultPermission.name());
        String message = fileConfiguration.getString(path);
        if (message != null) {
            return message;
        }
        return defaultPermission.name(); // FIXME: Default message
    }

    @Override
    public void reload() {
        fileConfiguration = YamlConfiguration.loadConfiguration(new File(dataFolder, "messages/help_en.yml"));
    }

    private String getText(String path, Supplier<String> defaultTextGetter) {
        String message = fileConfiguration.getString(path);
        return message == null
            ? defaultTextGetter.get()
            : message;
    }

    private static String getCommandPath(CommandDescription command) {
        return COMMAND_PREFIX + CommandUtils.constructParentList(command)
            .stream()
            .map(cmd -> cmd.getLabels().get(0))
            .collect(Collectors.joining("."));
    }
}
