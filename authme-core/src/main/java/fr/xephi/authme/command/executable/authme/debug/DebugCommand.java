package fr.xephi.authme.command.executable.authme.debug;

import ch.jalu.injector.factory.Factory;
import com.google.common.collect.ImmutableSet;
import fr.xephi.authme.command.ExecutableCommand;
import fr.xephi.authme.permission.PermissionsManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;

import javax.inject.Inject;
import java.util.List;
import java.util.Locale;
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
        SpawnLocationViewer.class, MySqlDefaultChanger.class);

    @Inject
    private Factory<DebugSection> debugSectionFactory;

    @Inject
    private PermissionsManager permissionsManager;

    private Map<String, DebugSection> sections;

    @Override
    public void executeCommand(CommandSender sender, List<String> arguments) {
        DebugSection debugSection = findDebugSection(arguments);
        if (debugSection == null) {
            sendAvailableSections(sender);
        } else {
            executeSection(debugSection, sender, arguments);
        }
    }

    private DebugSection findDebugSection(List<String> arguments) {
        if (arguments.isEmpty()) {
            return null;
        }
        return getSections().get(arguments.get(0).toLowerCase(Locale.ROOT));
    }

    private void sendAvailableSections(CommandSender sender) {
        sender.sendMessage(ChatColor.BLUE + "AuthMe debug utils");
        sender.sendMessage("Sections available to you:");
        long availableSections = getSections().values().stream()
            .filter(section -> permissionsManager.hasPermission(sender, section.getRequiredPermission()))
            .peek(e -> sender.sendMessage("- " + e.getName() + ": " + e.getDescription()))
            .count();

        if (availableSections == 0) {
            sender.sendMessage(ChatColor.RED + "You don't have permission to view any debug section");
        }
    }

    private void executeSection(DebugSection section, CommandSender sender, List<String> arguments) {
        if (permissionsManager.hasPermission(sender, section.getRequiredPermission())) {
            section.execute(sender, arguments.subList(1, arguments.size()));
        } else {
            sender.sendMessage(ChatColor.RED + "You don't have permission for this section. See /authme debug");
        }
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
