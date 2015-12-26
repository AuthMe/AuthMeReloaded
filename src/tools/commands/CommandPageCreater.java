package commands;

import fr.xephi.authme.command.CommandArgumentDescription;
import fr.xephi.authme.command.CommandDescription;
import fr.xephi.authme.command.CommandInitializer;
import fr.xephi.authme.command.CommandPermissions;
import fr.xephi.authme.command.CommandUtils;
import fr.xephi.authme.permission.PermissionNode;
import utils.ANewMap;
import utils.FileUtils;
import utils.TagReplacer;
import utils.ToolTask;
import utils.ToolsConstants;

import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class CommandPageCreater implements ToolTask {

    @Override
    public String getTaskName() {
        return "createCommandPage";
    }

    @Override
    public void execute(Scanner scanner) {
        final Set<CommandDescription> baseCommands = CommandInitializer.buildCommands();
        final String template = FileUtils.readFromFile(ToolsConstants.TOOLS_SOURCE_ROOT
            + "commands/command_entry.tpl.md");

        StringBuilder commandsResult = new StringBuilder();
        for (CommandDescription command : baseCommands) {
            Map<String, String> tags = ANewMap
                .with("command", CommandUtils.constructCommandPath(command))
                .and("description", command.getDetailedDescription())
                .and("arguments", formatArguments(command.getArguments()))
                .and("permissions", formatPermissions(command.getCommandPermissions()))
                .build();
            commandsResult.append(TagReplacer.applyReplacements(template, tags));
        }

        FileUtils.generateFileFromTemplate(
            ToolsConstants.TOOLS_SOURCE_ROOT + "commands/commands.tpl.md",
            ToolsConstants.DOCS_FOLDER + "commands.md",
            ANewMap.with("commands", commandsResult.toString()).build());
    }

    private static String formatPermissions(CommandPermissions permissions) {
        if (permissions == null) {
            return "";
        }
        String result = "";
        for (PermissionNode node : permissions.getPermissionNodes()) {
            result += node.getNode() + " ";
        }
        return result;
    }

    private static String formatArguments(Iterable<CommandArgumentDescription> arguments) {
        StringBuilder result = new StringBuilder();
        for (CommandArgumentDescription argument : arguments) {
            String argumentName = argument.isOptional()
                ? "[" + argument.getDescription() + "]"
                : "<" + argument.getDescription() + ">";
            result.append(argumentName).append(" ");
        }
        return result.toString();
    }
}
