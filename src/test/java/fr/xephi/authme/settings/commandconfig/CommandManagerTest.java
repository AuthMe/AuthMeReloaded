package fr.xephi.authme.settings.commandconfig;

import com.google.common.io.Files;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.BukkitServiceTestHelper;
import fr.xephi.authme.service.GeoIpService;
import fr.xephi.authme.settings.SettingsMigrationService;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Test for {@link CommandManager}.
 */
@RunWith(MockitoJUnitRunner.class)
public class CommandManagerTest {

    private static final String TEST_FILES_FOLDER = "/fr/xephi/authme/settings/commandconfig/";

    private CommandManager manager;
    private Player player;

    @InjectMocks
    private CommandMigrationService commandMigrationService;

    @Mock
    private BukkitService bukkitService;
    @Mock
    private GeoIpService geoIpService;
    @Mock
    private SettingsMigrationService settingsMigrationService;

    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();

    private File testFolder;

    @Before
    public void setup() throws IOException {
        testFolder = temporaryFolder.newFolder();
        player = mockPlayer();
        BukkitServiceTestHelper.setBukkitServiceToScheduleSyncDelayedTaskWithDelay(bukkitService);
    }

    @Test
    public void shouldExecuteCommandsOnLogin() {
        // given
        copyJarFileAsCommandsYml(TEST_FILES_FOLDER + "commands.complete.yml");
        initManager();

        // when
        manager.runCommandsOnLogin(player, Collections.emptyList());

        // then
        verify(bukkitService).dispatchConsoleCommand("msg Bobby Welcome back");
        verify(bukkitService).dispatchCommand(any(Player.class), eq("motd"));
        verify(bukkitService).dispatchCommand(any(Player.class), eq("list"));
        verify(bukkitService).scheduleSyncDelayedTask(any(Runnable.class), eq(60L));
        verify(bukkitService).scheduleSyncDelayedTask(any(Runnable.class), eq(120L));
        verifyNoMoreInteractions(bukkitService);
        verifyNoInteractions(geoIpService);
    }

    @Test
    public void shouldExecuteCommandsOnLoginWithTwoAlts() {
        // given
        copyJarFileAsCommandsYml(TEST_FILES_FOLDER + "commands.complete.yml");
        initManager();

        // when
        manager.runCommandsOnLogin(player, Arrays.asList("willy", "nilly", "billy", "silly"));

        // then
        verify(bukkitService).dispatchConsoleCommand("msg Bobby Welcome back");
        verify(bukkitService).dispatchCommand(player, "motd");
        verify(bukkitService).dispatchCommand(player, "list");
        verify(bukkitService).dispatchConsoleCommand("helpop Player Bobby has more than 1 account");
        verify(bukkitService).scheduleSyncDelayedTask(any(Runnable.class), eq(60L));
        verify(bukkitService).scheduleSyncDelayedTask(any(Runnable.class), eq(120L));
        verify(bukkitService).scheduleSyncDelayedTask(any(Runnable.class), eq(180L));
        verifyNoMoreInteractions(bukkitService);
        verifyNoInteractions(geoIpService);
    }

    @Test
    public void shouldExecuteCommandsOnLoginWithFifteenAlts() {
        // given
        copyJarFileAsCommandsYml(TEST_FILES_FOLDER + "commands.complete.yml");
        initManager();

        // when
        manager.runCommandsOnLogin(player, Collections.nCopies(15, "swag"));

        // then
        verify(bukkitService).dispatchConsoleCommand("msg Bobby Welcome back");
        verify(bukkitService).dispatchCommand(player, "motd");
        verify(bukkitService).dispatchCommand(player, "list");
        verify(bukkitService).dispatchConsoleCommand("helpop Player Bobby has more than 1 account");
        verify(bukkitService).dispatchConsoleCommand("log Bobby 127.0.0.3 many accounts");
        verify(bukkitService).scheduleSyncDelayedTask(any(Runnable.class), eq(60L));
        verify(bukkitService).scheduleSyncDelayedTask(any(Runnable.class), eq(120L));
        verify(bukkitService).scheduleSyncDelayedTask(any(Runnable.class), eq(180L));
        verify(bukkitService).scheduleSyncDelayedTask(any(Runnable.class), eq(240L));
        verifyNoMoreInteractions(bukkitService);
        verifyNoInteractions(geoIpService);
    }

    @Test
    public void shouldExecuteCommandsOnLoginWithTwentyFiveAlts() {
        // given
        copyJarFileAsCommandsYml(TEST_FILES_FOLDER + "commands.complete.yml");
        initManager();

        // when
        manager.runCommandsOnLogin(player, Collections.nCopies(25, "yolo"));

        // then
        verify(bukkitService).dispatchConsoleCommand("msg Bobby Welcome back");
        verify(bukkitService).dispatchCommand(player, "motd");
        verify(bukkitService).dispatchCommand(player, "list");
        verify(bukkitService).dispatchConsoleCommand("helpop Player Bobby has more than 1 account");
        verify(bukkitService).scheduleSyncDelayedTask(any(Runnable.class), eq(60L));
        verify(bukkitService).scheduleSyncDelayedTask(any(Runnable.class), eq(120L));
        verify(bukkitService).scheduleSyncDelayedTask(any(Runnable.class), eq(180L));
        verifyNoMoreInteractions(bukkitService);
        verifyNoInteractions(geoIpService);
    }

    @Test
    public void shouldExecuteCommandsOnLoginWithIncompleteConfig() {
        // given
        copyJarFileAsCommandsYml(TEST_FILES_FOLDER + "commands.incomplete.yml");
        initManager();
        BukkitServiceTestHelper.setBukkitServiceToScheduleSyncDelayedTaskWithDelay(bukkitService);

        // when
        manager.runCommandsOnLogin(player, Collections.emptyList());

        // then
        verify(bukkitService).dispatchConsoleCommand("msg Bobby Welcome back, bob");
        verify(bukkitService).dispatchCommand(any(Player.class), eq("list"));
        verify(bukkitService).scheduleSyncDelayedTask(any(Runnable.class), eq(100L));
        verifyNoMoreInteractions(bukkitService);
        verifyNoInteractions(geoIpService);
    }

    @Test
    public void shouldExecuteCommandsOnSessionLogin() {
        // given
        copyJarFileAsCommandsYml(TEST_FILES_FOLDER + "commands.complete.yml");
        initManager();

        // when
        manager.runCommandsOnSessionLogin(player);

        // then
        verify(bukkitService).dispatchConsoleCommand("msg Bobby Session login!");
        verifyNoMoreInteractions(bukkitService);
        verifyNoInteractions(geoIpService);
    }

    @Test
    public void shouldExecuteCommandsOnFirstLogin() {
        // given
        copyJarFileAsCommandsYml(TEST_FILES_FOLDER + "commands.complete.yml");
        initManager();

        // when
        manager.runCommandsOnFirstLogin(player, Collections.emptyList());

        // then
        verify(bukkitService).dispatchConsoleCommand("pay Bobby 30");
        verifyNoMoreInteractions(bukkitService);
        verifyNoInteractions(geoIpService);
    }

    @Test
    public void shouldNotExecuteFirstLoginCommandWhoseThresholdIsNotMet() {
        // given
        copyJarFileAsCommandsYml(TEST_FILES_FOLDER + "commands.complete.yml");
        initManager();

        // when
        manager.runCommandsOnFirstLogin(player, Arrays.asList("u", "wot", "m8"));

        // then
        verifyNoInteractions(bukkitService, geoIpService);
    }

    @Test
    public void shouldExecuteCommandsOnJoin() {
        // given
        copyJarFileAsCommandsYml(TEST_FILES_FOLDER + "commands.complete.yml");
        initManager();

        // when
        manager.runCommandsOnJoin(player);

        // then
        verify(bukkitService, only()).dispatchConsoleCommand("broadcast bob has joined");
        verifyNoInteractions(geoIpService);
    }

    @Test
    public void shouldExecuteCommandsOnJoinWithIncompleteConfig() {
        // given
        copyJarFileAsCommandsYml(TEST_FILES_FOLDER + "commands.incomplete.yml");
        initManager();

        // when
        manager.runCommandsOnJoin(player);

        // then
        verify(bukkitService, only()).dispatchConsoleCommand("broadcast Bobby has joined");
        verifyNoInteractions(geoIpService);
    }

    @Test
    public void shouldExecuteCommandsOnRegister() {
        // given
        copyJarFileAsCommandsYml(TEST_FILES_FOLDER + "commands.complete.yml");
        initManager();

        // when
        manager.runCommandsOnRegister(player);

        // then
        verify(bukkitService).dispatchCommand(any(Player.class), eq("me I just registered"));
        verify(bukkitService).dispatchConsoleCommand("log Bobby (127.0.0.3, Syldavia) registered");
        verify(bukkitService, times(2)).scheduleSyncDelayedTask(any(Runnable.class), eq(100L));
        verifyNoMoreInteractions(bukkitService);
    }

    @Test
    public void shouldExecuteCommandOnLogout() {
        // given
        copyJarFileAsCommandsYml(TEST_FILES_FOLDER + "commands.complete.yml");
        initManager();

        // when
        manager.runCommandsOnLogout(player);

        // then
        verify(bukkitService).dispatchConsoleCommand("broadcast Bobby (127.0.0.3) logged out");
        verifyNoMoreInteractions(bukkitService);
        verifyNoInteractions(geoIpService);
    }

    @Test
    public void shouldExecuteCommandsOnRegisterWithIncompleteConfig() {
        // given
        copyJarFileAsCommandsYml(TEST_FILES_FOLDER + "commands.incomplete.yml");
        initManager();

        // when
        manager.runCommandsOnRegister(player);

        // then
        verifyNoInteractions(bukkitService, geoIpService);
    }

    @Test
    public void shouldExecuteCommandOnUnregister() {
        // given
        copyJarFileAsCommandsYml(TEST_FILES_FOLDER + "commands.incomplete.yml");
        initManager();

        // when
        manager.runCommandsOnUnregister(player);

        // then
        verify(bukkitService).dispatchConsoleCommand("msg Bobby sad to see you go!");
    }

    private void initManager() {
        manager = new CommandManager(testFolder, bukkitService, geoIpService, commandMigrationService);
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

    private Player mockPlayer() {
        Player player = mock(Player.class);
        given(player.getName()).willReturn("Bobby");
        given(player.getDisplayName()).willReturn("bob");
        String ip = "127.0.0.3";
        TestHelper.mockIpAddressToPlayer(player, ip);
        given(geoIpService.getCountryName(ip)).willReturn("Syldavia");
        return player;
    }
}
