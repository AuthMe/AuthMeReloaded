package tools.helptranslation;

import com.google.common.collect.Sets;
import de.bananaco.bpermissions.imp.YamlConfiguration;
import fr.xephi.authme.command.CommandDescription;
import fr.xephi.authme.command.CommandInitializer;
import fr.xephi.authme.command.CommandUtils;
import fr.xephi.authme.command.help.HelpMessage;
import fr.xephi.authme.command.help.HelpSection;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.FileConfiguration;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Verifies a help messages translation.
 */
public class HelpTranslationVerifier {

    private final FileConfiguration configuration;

    // missing and unknown HelpSection and HelpMessage entries
    private final List<String> missingSections = new ArrayList<>();
    private final List<String> unknownSections = new ArrayList<>();
    // missing and unknown command entries
    private final List<String> missingCommands = new ArrayList<>();
    private final List<String> unknownCommands = new ArrayList<>();

    public HelpTranslationVerifier(File translation) {
        this.configuration = YamlConfiguration.loadConfiguration(translation);
        checkFile();
    }

    private void checkFile() {
        checkHelpSections();
        checkCommands();
    }

    public List<String> getMissingSections() {
        return missingSections;
    }

    public List<String> getUnknownSections() {
        return unknownSections;
    }

    public List<String> getMissingCommands() {
        // All entries start with "command.", so remove that
        return missingCommands.stream()
            .map(s -> s.substring(9)).collect(Collectors.toList());
    }

    public List<String> getUnknownCommands() {
        // All entries start with "command.", so remove that
        return unknownCommands.stream()
            .map(s -> s.substring(9)).collect(Collectors.toList());
    }

    /**
     * Verifies that the file has the expected entries for {@link HelpSection} and {@link HelpMessage}.
     */
    private void checkHelpSections() {
        Set<String> knownSections = Arrays.stream(HelpSection.values())
            .map(HelpSection::getKey).collect(Collectors.toSet());
        knownSections.addAll(Arrays.stream(HelpMessage.values()).map(HelpMessage::getKey).collect(Collectors.toSet()));
        knownSections.addAll(Arrays.asList("common.defaultPermissions.notAllowed",
            "common.defaultPermissions.opOnly", "common.defaultPermissions.allowed"));
        Set<String> sectionKeys = getLeafKeys("section");
        sectionKeys.addAll(getLeafKeys("common"));

        if (sectionKeys.isEmpty()) {
            missingSections.addAll(knownSections);
        } else {
            missingSections.addAll(Sets.difference(knownSections, sectionKeys));
            unknownSections.addAll(Sets.difference(sectionKeys, knownSections));
        }
    }

    /**
     * Verifies that the file has the expected entries for AuthMe commands.
     */
    private void checkCommands() {
        Set<String> commandPaths = buildCommandPaths();
        Set<String> existingKeys = getLeafKeys("commands");
        if (existingKeys.isEmpty()) {
            missingCommands.addAll(commandPaths); // commandPaths should be empty in this case
        } else {
            missingCommands.addAll(Sets.difference(commandPaths, existingKeys));
            unknownCommands.addAll(Sets.difference(existingKeys, commandPaths));
        }
    }

    private Set<String> buildCommandPaths() {
        Set<String> commandPaths = new LinkedHashSet<>();
        for (CommandDescription command : new CommandInitializer().getCommands()) {
            commandPaths.addAll(getYamlPaths(command));
            command.getChildren().forEach(child -> commandPaths.addAll(getYamlPaths(child)));
        }
        return commandPaths;
    }

    private List<String> getYamlPaths(CommandDescription command) {
        // e.g. commands.authme.register
        String commandPath = "commands." + CommandUtils.constructParentList(command).stream()
            .map(cmd -> cmd.getLabels().get(0))
            .collect(Collectors.joining("."));
        // The entire command is not present, so just add it as a missing command and don't return any YAML path
        if (!configuration.contains(commandPath)) {
            missingCommands.add(commandPath);
            return Collections.emptyList();
        }

        // Entries each command can have
        List<String> paths = newArrayList(commandPath + ".description", commandPath + ".detailedDescription");

        // Add argument entries that may exist
        for (int argIndex = 1; argIndex <= command.getArguments().size(); ++argIndex) {
            String argPath = String.format("%s.arg%d", commandPath, argIndex);
            paths.add(argPath + ".label");
            paths.add(argPath + ".description");
        }
        return paths;
    }

    /**
     * Returns the leaf keys of the section at the given path of the file configuration.
     *
     * @param path the path whose leaf keys should be retrieved
     * @return leaf keys of the memory section,
     *         empty set if the configuration does not have a memory section at the given path
     */
    private Set<String> getLeafKeys(String path) {
        if (!(configuration.get(path) instanceof MemorySection)) {
            return Collections.emptySet();
        }
        MemorySection memorySection = (MemorySection) configuration.get(path);

        // MemorySection#getKeys(true) returns all keys on all levels, e.g. if the configuration has
        // 'commands.authme.register' then it also has 'commands.authme' and 'commands'. We can traverse each node and
        // build its parents (e.g. for commands.authme.register.description: commands.authme.register, commands.authme,
        // and commands, which we can remove from the collection since we know they are not a leaf.
        Set<String> leafKeys = memorySection.getKeys(true);
        Set<String> allKeys = new HashSet<>(leafKeys);

        for (String key : allKeys) {
            List<String> pathParts = Arrays.asList(key.split("\\."));

            // We perform construction of parents & their removal in reverse order so we can build the lowest-level
            // parent of a node first. As soon as the parent doesn't exist in the set already, we know we can continue
            // with the next node since another node has already removed the concerned parents.
            for (int i = pathParts.size() - 1; i > 0; --i) {
                // e.g. for commands.authme.register -> i = {2, 1} => {commands.authme, commands}
                String parentPath = String.join(".", pathParts.subList(0, i));
                if (!leafKeys.remove(parentPath)) {
                    break;
                }
            }
        }
        return leafKeys.stream().map(leaf -> path + "." + leaf).collect(Collectors.toSet());
    }
}
