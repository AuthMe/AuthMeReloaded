package fr.xephi.authme.command;

import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static fr.xephi.authme.TestHelper.getJarFile;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Checks that the commands declared in plugin.yml correspond
 * to the ones built by the {@link CommandInitializer}.
 */
public class CommandConsistencyTest {

    @Test
    public void shouldHaveEqualDefinitions() {
        // given
        Collection<List<String>> initializedCommands = initializeCommands();
        Map<String, List<String>> pluginFileLabels = getLabelsFromPluginFile();

        // when / then
        assertThat("number of base commands are equal in plugin.yml and CommandInitializer",
            initializedCommands.size(), equalTo(pluginFileLabels.size()));
        for (List<String> commandLabels : initializedCommands) {
            List<String> pluginYmlLabels = pluginFileLabels.get(commandLabels.get(0));
            // NB: the first label in CommandDescription needs to correspond to the key in plugin.yml
            assertThat("plugin.yml contains definition for command '" + commandLabels.get(0) + "'",
                pluginYmlLabels, not(nullValue()));
            assertThat("plugin.yml and CommandDescription have same alternative labels for /" + commandLabels.get(0),
                pluginYmlLabels, containsInAnyOrder(commandLabels.subList(1, commandLabels.size()).toArray()));
        }
    }

    /**
     * Gets the command definitions from CommandInitializer and returns the
     * labels of all base commands.
     *
     * @return collection of all base command labels
     */
    private static Collection<List<String>> initializeCommands() {
        CommandInitializer initializer = new CommandInitializer();
        Collection<List<String>> commandLabels = new ArrayList<>();
        for (CommandDescription baseCommand : initializer.getCommands()) {
            commandLabels.add(baseCommand.getLabels());
        }
        return commandLabels;
    }

    /**
     * Reads plugin.yml and returns the defined commands by main label and aliases.
     *
     * @return collection of all labels and their aliases
     */
    @SuppressWarnings("unchecked")
    private static Map<String, List<String>> getLabelsFromPluginFile() {
        FileConfiguration pluginFile = YamlConfiguration.loadConfiguration(getJarFile("/plugin.yml"));
        MemorySection commandList = (MemorySection) pluginFile.get("commands");
        Map<String, Object> commandDefinitions = commandList.getValues(false);

        Map<String, List<String>> commandLabels = new HashMap<>();
        for (Map.Entry<String, Object> commandDefinition : commandDefinitions.entrySet()) {
            MemorySection definition = (MemorySection) commandDefinition.getValue();
            List<String> alternativeLabels = definition.get("aliases") == null
                ? Collections.EMPTY_LIST
                : (List<String>) definition.get("aliases");
            commandLabels.put(commandDefinition.getKey(), alternativeLabels);
        }
        return commandLabels;
    }

}
