package fr.xephi.authme.command.help;

import com.google.common.base.CaseFormat;
import fr.xephi.authme.command.CommandArgumentDescription;
import fr.xephi.authme.command.CommandDescription;
import fr.xephi.authme.command.CommandUtils;
import fr.xephi.authme.initialization.Reloadable;
import fr.xephi.authme.message.MessageFileCopier;
import fr.xephi.authme.message.MessageFileCopier.MessageFileData;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.permission.DefaultPermission;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import javax.inject.Inject;
import java.io.InputStream;
import java.io.InputStreamReader;
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

    private final MessageFileCopier fileCopier;
    private FileConfiguration fileConfiguration;
    private String defaultFile;
    private FileConfiguration defaultConfiguration;

    @Inject
    HelpMessagesService(MessageFileCopier fileCopier) {
        this.fileCopier = fileCopier;
        reload();
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
            ? getDefault(key.getKey())
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
        return getDefault(path);
    }

    @Override
    public void reload() {
        MessageFileData fileData = fileCopier.initializeData(lang -> "messages/help_" + lang + ".yml");
        this.fileConfiguration = YamlConfiguration.loadConfiguration(fileData.getFile());
        this.defaultFile = fileData.getDefaultFile();
    }

    private String getDefault(String code) {
        if (defaultFile == null) {
            return getDefaultErrorMessage(code);
        }

        if (defaultConfiguration == null) {
            InputStream stream = Messages.class.getResourceAsStream(defaultFile);
            defaultConfiguration = YamlConfiguration.loadConfiguration(new InputStreamReader(stream));
        }
        String message = defaultConfiguration.getString(code);
        return message == null ? getDefaultErrorMessage(code) : message;
    }

    private static String getDefaultErrorMessage(String code) {
        return "Error retrieving message '" + code + "'";
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
