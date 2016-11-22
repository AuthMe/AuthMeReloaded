package fr.xephi.authme.settings.commandconfig;

import com.google.common.io.Files;
import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.settings.Settings;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;

import static fr.xephi.authme.settings.commandconfig.CommandConfigTestHelper.isCommand;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;

/**
 * Test for {@link CommandManager}.
 */
@RunWith(MockitoJUnitRunner.class)
public class CommandManagerTest {

    private static final String TEST_FILES_FOLDER = "/fr/xephi/authme/settings/commandconfig/";

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private CommandManager manager;
    @Mock
    private BukkitService bukkitService;
    @Mock
    private Settings settings;
    @Mock
    private CommandsMigrater commandsMigrater;
    private File testFolder;

    @Before
    public void setup() throws IOException {
        testFolder = temporaryFolder.newFolder();
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldLoadCompleteFile() {
        // given
        copyJarFileAsCommandsYml(TEST_FILES_FOLDER + "commands.complete.yml");

        // when
        initManager();

        // then
        CommandConfig commandConfig = ReflectionTestUtils.getFieldValue(CommandManager.class, manager, "commandConfig");
        assertThat(commandConfig.getOnJoin().keySet(), contains("broadcast"));
        assertThat(commandConfig.getOnJoin().values(), contains(isCommand("broadcast %p has joined", Executor.CONSOLE)));
        assertThat(commandConfig.getOnRegister().keySet(), contains("announce", "notify"));
        assertThat(commandConfig.getOnRegister().values(), contains(
            isCommand("me I just registered", Executor.PLAYER),
            isCommand("log %p registered", Executor.CONSOLE)));
        assertThat(commandConfig.getOnLogin().keySet(), contains("welcome", "show_motd", "display_list"));
        assertThat(commandConfig.getOnLogin().values(), contains(
            isCommand("msg %p Welcome back", Executor.CONSOLE),
            isCommand("motd", Executor.PLAYER),
            isCommand("list", Executor.PLAYER)));
    }

    @Test
    @Ignore // TODO #411: Implement tested behavior
    public void shouldLoadIncompleteFile() {
        // given
        File configFile = copyJarFileAsCommandsYml(TEST_FILES_FOLDER + "commands.incomplete.yml");

        // when
        initManager();

        // then
        CommandConfig commandConfig = ReflectionTestUtils.getFieldValue(CommandManager.class, manager, "commandConfig");
        assertThat(commandConfig.getOnJoin().values(), contains(isCommand("broadcast %p has joined", Executor.CONSOLE)));
        assertThat(commandConfig.getOnLogin().values(), contains(
            isCommand("msg %p Welcome back", Executor.CONSOLE),
            isCommand("list", Executor.PLAYER)));
        assertThat(commandConfig.getOnRegister(), anEmptyMap());

        // verify that we have rewritten the file with the proper sections
        FileConfiguration configuration = YamlConfiguration.loadConfiguration(configFile);
        assertThat(configuration.contains("onRegister"), equalTo(true));
        assertThat(configuration.contains("doesNotExist"), equalTo(false));
    }

    private void initManager() {
        manager = new CommandManager(testFolder, bukkitService, commandsMigrater, settings);
    }

    private File copyJarFileAsCommandsYml(String path) {
        File source = TestHelper.getJarFile(path);
        File destination = new File(testFolder, "commands.yml");
        try {
            Files.copy(source, destination);
            return destination;
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

}
