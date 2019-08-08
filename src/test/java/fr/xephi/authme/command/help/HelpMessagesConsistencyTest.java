package fr.xephi.authme.command.help;

import ch.jalu.configme.resource.PropertyReader;
import ch.jalu.configme.resource.YamlFileReader;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.command.CommandDescription;
import fr.xephi.authme.command.CommandInitializer;
import fr.xephi.authme.message.MessagePathHelper;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Test;

import java.io.File;
import java.util.Collection;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.junit.Assert.assertThat;

/**
 * Tests that /messages/help_en.yml contains texts that correspond
 * to the texts provided in the CommandDescription.
 */
public class HelpMessagesConsistencyTest {

    private static final File DEFAULT_MESSAGES_FILE =
        TestHelper.getJarFile("/" + MessagePathHelper.createHelpMessageFilePath(MessagePathHelper.DEFAULT_LANGUAGE));

    @Test
    public void shouldHaveIdenticalTexts() {
        // given
        CommandDescription description = getAuthMeRegisterDescription();
        FileConfiguration configuration = YamlConfiguration.loadConfiguration(DEFAULT_MESSAGES_FILE);
        final String path = "commands.authme.register.";

        // when / then
        assertThat(configuration.get(path + "description"), equalTo(description.getDescription()));
        assertThat(configuration.get(path + "detailedDescription"), equalTo(description.getDetailedDescription()));
        assertThat(configuration.get(path + "arg1.label"), equalTo(description.getArguments().get(0).getName()));
        assertThat(configuration.get(path + "arg1.description"), equalTo(description.getArguments().get(0).getDescription()));
        assertThat(configuration.get(path + "arg2.label"), equalTo(description.getArguments().get(1).getName()));
        assertThat(configuration.get(path + "arg2.description"), equalTo(description.getArguments().get(1).getDescription()));
    }

    /**
     * Since CommandInitializer contains all descriptions for commands in English, the help_en.yml file
     * only contains an entry for one command as to provide an example.
     */
    @Test
    public void shouldOnlyHaveDescriptionForOneCommand() {
        // given
        FileConfiguration configuration = YamlConfiguration.loadConfiguration(DEFAULT_MESSAGES_FILE);

        // when
        Object commands = configuration.get("commands");

        // then
        assertThat(commands, instanceOf(MemorySection.class));
        assertThat(((MemorySection) commands).getKeys(false), contains("authme"));
    }

    @Test
    public void shouldHaveEntryForEachHelpMessageKey() {
        // given
        PropertyReader reader = new YamlFileReader(DEFAULT_MESSAGES_FILE);

        // when / then
        for (HelpMessage message : HelpMessage.values()) {
            assertThat("Default configuration should have entry for message '" + message + "'",
                reader.contains(message.getKey()), equalTo(true));
        }
        for (HelpSection section : HelpSection.values()) {
            assertThat("Default configuration should have entry for section '" + section + "'",
                reader.contains(section.getKey()), equalTo(true));
        }
    }

    /**
     * @return the CommandDescription object for the {@code /authme register} command.
     */
    private static CommandDescription getAuthMeRegisterDescription() {
        Collection<CommandDescription> commands = new CommandInitializer().getCommands();

        List<CommandDescription> children = commands.stream()
            .filter(command -> command.getLabels().contains("authme"))
            .map(CommandDescription::getChildren)
            .findFirst().get();

        return children
            .stream()
            .filter(child -> child.getLabels().contains("register"))
            .findFirst().get();
    }
}
