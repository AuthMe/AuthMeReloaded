package tools.docs.commands;

import fr.xephi.authme.command.CommandArgumentDescription;
import fr.xephi.authme.command.CommandDescription;
import fr.xephi.authme.command.CommandInitializer;
import fr.xephi.authme.command.CommandUtils;
import fr.xephi.authme.permission.PermissionNode;
import tools.utils.AutoToolTask;
import tools.utils.FileIoUtils;
import tools.utils.TagValue.NestedTagValue;
import tools.utils.TagValueHolder;
import tools.utils.ToolsConstants;

import java.util.Collection;

public class CommandPageCreater implements AutoToolTask {

    private static final String OUTPUT_FILE = ToolsConstants.DOCS_FOLDER + "commands.md";

    @Override
    public String getTaskName() {
        return "createCommandPage";
    }

    @Override
    public void executeDefault() {
        CommandInitializer commandInitializer = new CommandInitializer();
        final Collection<CommandDescription> baseCommands = commandInitializer.getCommands();
        NestedTagValue commandTags = new NestedTagValue();
        addCommandsInfo(commandTags, baseCommands);

        FileIoUtils.generateFileFromTemplate(
            ToolsConstants.TOOLS_SOURCE_ROOT + "docs/commands/commands.tpl.md",
            OUTPUT_FILE,
            TagValueHolder.create().put("commands", commandTags));
        System.out.println("Wrote to '" + OUTPUT_FILE + "' with " + baseCommands.size() + " base commands.");
    }

    private static void addCommandsInfo(NestedTagValue commandTags, Collection<CommandDescription> commands) {
        for (CommandDescription command : commands) {
            TagValueHolder tags = TagValueHolder.create()
                .put("command", CommandUtils.constructCommandPath(command))
                .put("description", command.getDetailedDescription())
                .put("arguments", formatArguments(command.getArguments()))
                .put("permissions", formatPermissions(command.getPermission()));
            commandTags.add(tags);

            if (!command.getChildren().isEmpty()) {
                addCommandsInfo(commandTags, command.getChildren());
            }
        }
    }

    private static String formatPermissions(PermissionNode permission) {
        if (permission == null) {
            return "";
        } else {
            return permission.getNode();
        }
    }

    private static String formatArguments(Iterable<CommandArgumentDescription> arguments) {
        StringBuilder result = new StringBuilder();
        for (CommandArgumentDescription argument : arguments) {
            String argumentName = argument.isOptional()
                ? "[" + argument.getName() + "]"
                : "&lt;" + argument.getName() + ">";
            result.append(" ").append(argumentName);
        }
        return result.toString();
    }
}
