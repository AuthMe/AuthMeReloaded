package fr.xephi.authme.service;

import ch.jalu.injector.testing.BeforeInjecting;
import ch.jalu.injector.testing.DelayedInjectionRunner;
import ch.jalu.injector.testing.InjectDelayed;
import com.google.common.io.Files;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.command.CommandInitializer;
import fr.xephi.authme.command.help.HelpMessage;
import fr.xephi.authme.command.help.HelpMessagesService;
import fr.xephi.authme.command.help.HelpSection;
import fr.xephi.authme.initialization.DataFolder;
import fr.xephi.authme.message.HelpMessagesFileHandler;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PluginSettings;
import org.bukkit.configuration.MemorySection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.io.File;
import java.io.IOException;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.BDDMockito.given;

/**
 * Integration test for {@link HelpTranslationGenerator}.
 */
@RunWith(DelayedInjectionRunner.class)
public class HelpTranslationGeneratorIntegrationTest {

    @InjectDelayed
    private HelpTranslationGenerator helpTranslationGenerator;
    @InjectDelayed
    private HelpMessagesService helpMessagesService;
    @InjectDelayed
    private HelpMessagesFileHandler helpMessagesFileHandler;
    @InjectDelayed
    private CommandInitializer commandInitializer;

    @DataFolder
    private File dataFolder;
    private File helpMessagesFile;

    @Mock
    private Settings settings;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @BeforeClass
    public static void setUpLogger() {
        TestHelper.setupLogger();
    }

    @BeforeInjecting
    public void setUpClasses() throws IOException {
        dataFolder = temporaryFolder.newFolder();
        File messagesFolder = new File(dataFolder, "messages");
        messagesFolder.mkdir();
        helpMessagesFile = new File(messagesFolder, "help_test.yml");
        Files.copy(TestHelper.getJarFile(TestHelper.PROJECT_ROOT + "message/help_test.yml"), helpMessagesFile);
        given(settings.getProperty(PluginSettings.MESSAGES_LANGUAGE)).willReturn("test");
    }

    @Test
    public void shouldUpdateCurrentHelpFile() throws IOException {
        // given / when
        helpTranslationGenerator.updateHelpFile();

        // then
        FileConfiguration configuration = YamlConfiguration.loadConfiguration(helpMessagesFile);
        checkCommonEntries(configuration);
        checkSections(configuration);
        checkCommands(configuration);
    }

    private void checkCommonEntries(FileConfiguration configuration) {
        // Entries that were already present
        assertThat(configuration.getString(HelpMessage.HEADER.getKey()), equalTo("My custom help header"));
        assertThat(configuration.getString(HelpMessage.OPTIONAL.getKey()), equalTo("t-opt"));
        assertThat(configuration.getString("common.defaultPermissions.notAllowed"), equalTo("t-noperm"));
        assertThat(configuration.getString("common.defaultPermissions.allowed"), equalTo("t-allperm"));

        // Entries that were added from the default
        assertThat(configuration.getString(HelpMessage.DEFAULT.getKey()), equalTo("Default"));
        assertThat(configuration.getString("common.defaultPermissions.opOnly"), equalTo("OP's only"));
    }

    private void checkSections(FileConfiguration configuration) {
        // Entries that were already present
        assertThat(configuration.getString(HelpSection.COMMAND.getKey()), equalTo("my command translation"));
        assertThat(configuration.getString(HelpSection.SHORT_DESCRIPTION.getKey()), equalTo(""));
        assertThat(configuration.getString(HelpSection.CHILDREN.getKey()), equalTo(""));

        // Entries that were added from the default
        assertThat(configuration.getString(HelpSection.DETAILED_DESCRIPTION.getKey()), equalTo("Detailed description"));
        assertThat(configuration.getString(HelpSection.ARGUMENTS.getKey()), equalTo("Arguments"));
        assertThat(configuration.getString(HelpSection.COMMAND.getKey()), equalTo("my command translation"));
    }

    private void checkCommands(FileConfiguration configuration) {
        // Check /authme and /authme register entries: full text was available
        checkDescription(configuration.get("commands.authme"), "test auth desc", "test auth long desc");
        checkDescription(configuration.get("commands.authme.register"), "test reg desc", "test reg long desc");
        checkArgs(configuration.get("commands.authme.register"),
            arg("test reg arg1", "test reg arg1 text"), arg("test reg arg2", "test reg arg2 text"));

        // Check /unregister: only had detailed description
        checkDescription(configuration.get("commands.authme.unregister"), "Unregister a player", "Detailed description for unregister.");

        // Check /email add
        checkDescription(configuration.get("commands.email.add"), "email add desc", "email add long desc");
        checkArgs(configuration.get("commands.email.add"),
            arg("add arg1", "add arg1 text"), arg("add arg2", "add arg2 text"));

        // Check /login
        checkDescription(configuration.get("commands.login"), "Login command", "/login detailed desc.");
        checkArgs(configuration.get("commands.login"), arg("loginArg", "Login password"));

        // Check /unregister
        checkDescription(configuration.get("commands.unregister"), "unreg_desc", "unreg_detail_desc");
        checkArgs(configuration.get("commands.unregister"), arg("unreg_arg_label", "unreg_arg_desc"));

        // Check /changepassword: had tons of invalid stuff, just expect it to be taken from defaults
        checkDescription(configuration.get("commands.changepassword"), "[a list, instead of text]", "1337");
        assertThat(configuration.get("commands.changepassword.arg1.label"), equalTo("true"));
        assertThat(configuration.get("commands.changepassword.arg1.description"), equalTo("[]"));
        // We have a whole object as arg2.label, for which the toString() is generated. Not very useful, so just test a portion...
        assertThat((String) configuration.get("commands.changepassword.arg2.label"), containsString("MemorySection"));
        assertThat(configuration.get("commands.changepassword.arg2.description"), equalTo("New password"));
        assertThat(configuration.get("commands.changepassword.arg3"), nullValue());

        // Check /captcha, with empty arg text
        checkArgs(configuration.get("commands.captcha"), arg("", ""));
    }

    private static void checkDescription(Object memorySection, String description, String detailedDescription) {
        if (memorySection instanceof MemorySection) {
            MemorySection memSection = (MemorySection) memorySection;
            assertThat(memSection.getString("description"), equalTo(description));
            assertThat(memSection.getString("detailedDescription"), equalTo(detailedDescription));
        } else {
            fail("Expected MemorySection, got '" + memorySection + "'");
        }
    }

    private static void checkArgs(Object memorySection, Argument... arguments) {
        if (memorySection instanceof MemorySection) {
            MemorySection memSection = (MemorySection) memorySection;
            int i = 1;
            for (Argument arg : arguments) {
                assertThat(memSection.getString("arg" + i + ".label"), equalTo(arg.label));
                assertThat(memSection.getString("arg" + i + ".description"), equalTo(arg.description));
                ++i;
            }
            assertThat(memSection.get("arg" + i), nullValue());
        } else {
            fail("Expected MemorySection, got '" + memorySection + "'");
        }
    }

    private static Argument arg(String label, String description) {
        return new Argument(label, description);
    }

    private static final class Argument {
        final String label;
        final String description;

        Argument(String label, String description) {
            this.label = label;
            this.description = description;
        }
    }
}
