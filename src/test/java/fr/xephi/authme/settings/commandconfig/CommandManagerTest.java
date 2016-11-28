package fr.xephi.authme.settings.commandconfig;

import com.google.common.io.Files;
import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.settings.SettingsMigrationService;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.util.function.BiConsumer;

import static fr.xephi.authme.settings.commandconfig.CommandConfigTestHelper.isCommand;
import static java.lang.String.format;
import static org.hamcrest.Matchers.anEmptyMap;
import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Test for {@link CommandManager}.
 */
@RunWith(MockitoJUnitRunner.class)
public class CommandManagerTest {

    private static final String TEST_FILES_FOLDER = "/fr/xephi/authme/settings/commandconfig/";

    private CommandManager manager;
    @InjectMocks
    private CommandMigrationService commandMigrationService;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    @Mock
    private BukkitService bukkitService;
    @Mock
    private SettingsMigrationService settingsMigrationService;

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
    public void shouldLoadIncompleteFile() {
        // given
        copyJarFileAsCommandsYml(TEST_FILES_FOLDER + "commands.incomplete.yml");

        // when
        initManager();

        // then
        CommandConfig commandConfig = ReflectionTestUtils.getFieldValue(CommandManager.class, manager, "commandConfig");
        assertThat(commandConfig.getOnJoin().values(), contains(isCommand("broadcast %p has joined", Executor.CONSOLE)));
        assertThat(commandConfig.getOnLogin().values(), contains(
            isCommand("msg %p Welcome back", Executor.CONSOLE),
            isCommand("list", Executor.PLAYER)));
        assertThat(commandConfig.getOnRegister(), anEmptyMap());
    }

    @Test
    public void shouldExecuteCommandsOnJoin() {
        // given
        String name = "Bobby1";

        // when
        testCommandExecution(name, CommandManager::runCommandsOnJoin);

        // then
        verify(bukkitService, only()).dispatchConsoleCommand(format("broadcast %s has joined", name));
    }

    @Test
    public void shouldExecuteCommandsOnRegister() {
        // given
        String name = "luis";

        // when
        testCommandExecution(name, CommandManager::runCommandsOnRegister);

        // then
        verify(bukkitService).dispatchCommand(any(Player.class), eq("me I just registered"));
        verify(bukkitService).dispatchConsoleCommand(format("log %s registered", name));
        verifyNoMoreInteractions(bukkitService);
    }

    @Test
    public void shouldExecuteCommandsOnLogin() {
        // given
        String name = "plaYer01";

        // when
        testCommandExecution(name, CommandManager::runCommandsOnLogin);

        // then
        verify(bukkitService).dispatchConsoleCommand(format("msg %s Welcome back", name));
        verify(bukkitService).dispatchCommand(any(Player.class), eq("motd"));
        verify(bukkitService).dispatchCommand(any(Player.class), eq("list"));
        verifyNoMoreInteractions(bukkitService);
    }

    @Test
    public void shouldHaveHiddenConstructorInSettingsHolderClass() {
        // given / when / then
        TestHelper.validateHasOnlyPrivateEmptyConstructor(CommandSettingsHolder.class);
    }


    private void testCommandExecution(String playerName, BiConsumer<CommandManager, Player> testMethod) {
        copyJarFileAsCommandsYml(TEST_FILES_FOLDER + "commands.complete.yml");
        initManager();
        Player player = mock(Player.class);
        given(player.getName()).willReturn(playerName);

        testMethod.accept(manager, player);
    }

    private void initManager() {
        manager = new CommandManager(testFolder, bukkitService, commandMigrationService);
    }

    private void copyJarFileAsCommandsYml(String path) {
        File source = TestHelper.getJarFile(path);
        File destination = new File(testFolder, "commands.yml");
        try {
            Files.copy(source, destination);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }

}
