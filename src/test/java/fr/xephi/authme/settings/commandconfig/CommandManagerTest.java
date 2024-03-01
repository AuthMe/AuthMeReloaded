package fr.xephi.authme.settings.commandconfig;

import com.google.common.io.Files;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.BukkitServiceTestHelper;
import fr.xephi.authme.service.GeoIpService;
import fr.xephi.authme.settings.SettingsMigrationService;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
@ExtendWith(MockitoExtension.class)
class CommandManagerTest {

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

    @TempDir
    File temporaryFolder;

    @BeforeEach
    void setup() {
        player = mock(Player.class);
        given(player.getName()).willReturn("Bobby");
    }

    @Nested
    class TestsWithPlayerIp {

        @BeforeEach
        void setup() {
            TestHelper.mockIpAddressToPlayer(player, "127.0.0.3");
        }

        @Test
        void shouldExecuteCommandsOnLoginWithTwentyFiveAlts() {
            // given
            copyJarFileAsCommandsYml(TEST_FILES_FOLDER + "commands.complete.yml");
            initManager();
            BukkitServiceTestHelper.setBukkitServiceToScheduleSyncDelayedTaskWithDelay(bukkitService);

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
        void shouldExecuteCommandsOnLogin() {
            // given
            copyJarFileAsCommandsYml(TEST_FILES_FOLDER + "commands.complete.yml");
            initManager();
            BukkitServiceTestHelper.setBukkitServiceToScheduleSyncDelayedTaskWithDelay(bukkitService);

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
        void shouldExecuteCommandsOnRegister() {
            // given
            copyJarFileAsCommandsYml(TEST_FILES_FOLDER + "commands.complete.yml");
            initManager();
            BukkitServiceTestHelper.setBukkitServiceToScheduleSyncDelayedTaskWithDelay(bukkitService);
            given(geoIpService.getCountryName("127.0.0.3")).willReturn("Syldavia");

            // when
            manager.runCommandsOnRegister(player);

            // then
            verify(bukkitService).dispatchCommand(any(Player.class), eq("me I just registered"));
            verify(bukkitService).dispatchConsoleCommand("log Bobby (127.0.0.3, Syldavia) registered");
            verify(bukkitService, times(2)).scheduleSyncDelayedTask(any(Runnable.class), eq(100L));
            verifyNoMoreInteractions(bukkitService);
        }

        @Test
        void shouldExecuteCommandsOnLoginWithTwoAlts() {
            // given
            copyJarFileAsCommandsYml(TEST_FILES_FOLDER + "commands.complete.yml");
            initManager();
            BukkitServiceTestHelper.setBukkitServiceToScheduleSyncDelayedTaskWithDelay(bukkitService);

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
        void shouldExecuteCommandsOnLoginWithFifteenAlts() {
            // given
            copyJarFileAsCommandsYml(TEST_FILES_FOLDER + "commands.complete.yml");
            initManager();
            BukkitServiceTestHelper.setBukkitServiceToScheduleSyncDelayedTaskWithDelay(bukkitService);

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
        void shouldExecuteCommandOnLogout() {
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
    }

    @Test
    void shouldExecuteCommandsOnLoginWithIncompleteConfig() {
        // given
        copyJarFileAsCommandsYml(TEST_FILES_FOLDER + "commands.incomplete.yml");
        initManager();
        BukkitServiceTestHelper.setBukkitServiceToScheduleSyncDelayedTaskWithDelay(bukkitService);
        given(player.getDisplayName()).willReturn("bob");

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
    void shouldExecuteCommandsOnSessionLogin() {
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
    void shouldExecuteCommandsOnFirstLogin() {
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
    void shouldNotExecuteFirstLoginCommandWhoseThresholdIsNotMet() {
        // given
        copyJarFileAsCommandsYml(TEST_FILES_FOLDER + "commands.complete.yml");
        initManager();

        // when
        manager.runCommandsOnFirstLogin(player, Arrays.asList("u", "wot", "m8"));

        // then
        verifyNoInteractions(bukkitService, geoIpService);
    }

    @Test
    void shouldExecuteCommandsOnJoin() {
        // given
        player.getName(); // Prevent UnnecessaryStubbingException as the name is not needed
        copyJarFileAsCommandsYml(TEST_FILES_FOLDER + "commands.complete.yml");
        initManager();
        given(player.getDisplayName()).willReturn("bob");

        // when
        manager.runCommandsOnJoin(player);

        // then
        verify(bukkitService, only()).dispatchConsoleCommand("broadcast bob has joined");
        verifyNoInteractions(geoIpService);
    }

    @Test
    void shouldExecuteCommandsOnJoinWithIncompleteConfig() {
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
    void shouldExecuteCommandsOnRegisterWithIncompleteConfig() {
        // given
        player.getName(); // Prevent UnnecessaryStubbingException as the name is not needed
        copyJarFileAsCommandsYml(TEST_FILES_FOLDER + "commands.incomplete.yml");
        initManager();

        // when
        manager.runCommandsOnRegister(player);

        // then
        verifyNoInteractions(bukkitService, geoIpService);
    }

    @Test
    void shouldExecuteCommandOnUnregister() {
        // given
        copyJarFileAsCommandsYml(TEST_FILES_FOLDER + "commands.incomplete.yml");
        initManager();

        // when
        manager.runCommandsOnUnregister(player);

        // then
        verify(bukkitService).dispatchConsoleCommand("msg Bobby sad to see you go!");
    }

    private void initManager() {
        manager = new CommandManager(temporaryFolder, bukkitService, geoIpService, commandMigrationService);
    }

    private void copyJarFileAsCommandsYml(String path) {
        File source = TestHelper.getJarFile(path);
        File destination = new File(temporaryFolder, "commands.yml");
        try {
            Files.copy(source, destination);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
