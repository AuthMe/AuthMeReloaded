package fr.xephi.authme.command.executable.authme.debug;

import fr.xephi.authme.command.ExecutableCommand;
import org.bukkit.command.CommandSender;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Debug command main.
 */
public class DebugCommand implements ExecutableCommand {

    @Inject
    private PermissionGroups permissionGroups;

    private Map<String, DebugSection> sections;

    @PostConstruct
    private void collectSections() {
        Map<String, DebugSection> sections = Stream.of(permissionGroups)
            .collect(Collectors.toMap(DebugSection::getName, Function.identity()));
        this.sections = sections;
    }

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments) {
        if (arguments.isEmpty()) {
            sender.sendMessage("Available sections:");
            sections.values()
                .forEach(e -> sender.sendMessage("- " + e.getName() + ": " + e.getDescription()));
        } else {
            DebugSection debugSection = sections.get(arguments.get(0).toLowerCase());
            if (debugSection == null) {
                sender.sendMessage("Unknown subcommand");
            } else {
                debugSection.execute(sender, arguments.subList(1, arguments.size()));
            }
        }
    }
}
