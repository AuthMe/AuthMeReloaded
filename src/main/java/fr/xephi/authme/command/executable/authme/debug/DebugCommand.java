package fr.xephi.authme.command.executable.authme.debug;

import com.google.common.collect.ImmutableSet;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.initialization.factory.Factory;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Debug command main.
 */
public class DebugCommand implements ExecutableCommand {

    private static final Set<Class<? extends DebugSection>> SECTION_CLASSES = ImmutableSet.of(
        PermissionGroups.class, DataStatistics.class, CountryLookup.class, PlayerAuthViewer.class, InputValidator.class,
        LimboPlayerViewer.class, CountryLookup.class, HasPermissionChecker.class, TestEmailSender.class,
        SpawnLocationViewer.class);

    @Inject
    private Factory<DebugSection> debugSectionFactory;

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
            Map<String, DebugSection> sections = new TreeMap<>();
            for (Class<? extends DebugSection> sectionClass : SECTION_CLASSES) {
                DebugSection section = debugSectionFactory.newInstance(sectionClass);
                sections.put(section.getName(), section);
            }
            this.sections = sections;
        }
        return sections;
    }
}
