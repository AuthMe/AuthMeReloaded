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

import java.util.Collection;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;

public class CommandPageCreater implements ToolTask {

    private static final String OUTPUT_FILE = ToolsConstants.DOCS_FOLDER + "commands.md";

    @Override
    public String getTaskName() {
        return "createCommandPage";
    }

    @Override
    public void execute(Scanner scanner) {
        final Set<CommandDescription> baseCommands = CommandInitializer.getBaseCommands();
        final String template = FileUtils.readFromFile(ToolsConstants.TOOLS_SOURCE_ROOT
            + "commands/command_entry.tpl.md");

        StringBuilder commandsResult = new StringBuilder();
        addCommandsInfo(commandsResult, baseCommands, template);

        FileUtils.generateFileFromTemplate(
            ToolsConstants.TOOLS_SOURCE_ROOT + "commands/commands.tpl.md",
            OUTPUT_FILE,
            ANewMap.with("commands", commandsResult.toString()).build());
        System.out.println("Wrote to '" + OUTPUT_FILE + "' with " + baseCommands.size() + " base commands.");
    }

    private static void addCommandsInfo(StringBuilder sb, Collection<CommandDescription> commands,
                                        final String template) {
        for (CommandDescription command : commands) {
            Map<String, String> tags = ANewMap
                .with("command", CommandUtils.constructCommandPath(command))
                .and("description", command.getDetailedDescription())
                .and("arguments", formatArguments(command.getArguments()))
                .and("permissions", formatPermissions(command.getCommandPermissions()))
                .build();
            sb.append(TagReplacer.applyReplacements(template, tags));

            if (!command.getChildren().isEmpty()) {
                addCommandsInfo(sb, command.getChildren(), template);
            }
        }
    }

    private static String formatPermissions(CommandPermissions permissions) {
        if (permissions == null) {
            return "";
        }
        String result = "";
        for (PermissionNode node : permissions.getPermissionNodes()) {
            result += node.getNode() + " ";
        }
        return result.trim();
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
