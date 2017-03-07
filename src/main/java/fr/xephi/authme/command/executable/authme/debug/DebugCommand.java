package fr.xephi.authme.command.executable.authme.debug;

import com.google.common.collect.ImmutableSet;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.initialization.factory.Factory;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Debug command main.
 */
public class DebugCommand implements ExecutableCommand {

    @Inject
    private Factory<DebugSection> debugSectionFactory;

    private Set<Class<? extends DebugSection>> sectionClasses =
        ImmutableSet.of(PermissionGroups.class, TestEmailSender.class, PlayerAuthViewer.class, LimboPlayerViewer.class);

    private Map<String, DebugSection> sections;

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments) {
        DebugSection debugSection = getDebugSection(arguments);
        if (debugSection == null) {
            sender.sendMessage("Available sections:");
            getSections().values()
                .forEach(e -> sender.sendMessage("- " + e.getName() + ": " + e.getDescription()));
        } else {
            debugSection.execute(sender, arguments.subList(1, arguments.size()));
        }
    }

    private DebugSection getDebugSection(List<String> arguments) {
        if (arguments.isEmpty()) {
            return null;
        }
        return getSections().get(arguments.get(0).toLowerCase());
    }

    // Lazy getter
    private Map<String, DebugSection> getSections() {
        if (sections == null) {
            Map<String, DebugSection> sections = new HashMap<>();
            for (Class<? extends DebugSection> sectionClass : sectionClasses) {
                DebugSection section = debugSectionFactory.newInstance(sectionClass);
                sections.put(section.getName(), section);
            }
            this.sections = sections;
        }
        return sections;
    }
}
