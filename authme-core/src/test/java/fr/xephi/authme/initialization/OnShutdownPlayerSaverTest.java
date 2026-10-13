package fr.xephi.authme.initialization;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.data.limbo.LimboService;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.ValidationService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.SpawnLoader;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link OnShutdownPlayerSaver}.
 */
@ExtendWith(MockitoExtension.class)
class OnShutdownPlayerSaverTest {

    @InjectMocks
    private OnShutdownPlayerSaver onShutdownPlayerSaver;

    @Mock
    private BukkitService bukkitService;
    @Mock
    private Settings settings;
    @Mock
    private ValidationService validationService;
    @Mock
    private DataSource dataSource;
    @Mock
    private SpawnLoader spawnLoader;
    @Mock
    private PlayerCache playerCache;
    @Mock
    private LimboService limboService;

    @BeforeAll
    static void initLogger() {
        TestHelper.setupLogger();
    }

    @Test
    void shouldSaveRemainingPlayersIfOneFails() {
        // given
        Player failing = mockPlayerInLimbo("Failing");
        Player other = mockPlayerInLimbo("Other");
        doReturn(Arrays.asList(failing, other)).when(bukkitService).getOnlinePlayers();
        doThrow(IllegalStateException.class).when(limboService).restoreData(failing);

        // when
        onShutdownPlayerSaver.saveAllPlayers();

        // then
        verify(playerCache).removePlayer("failing");
        verify(limboService).restoreData(other);
        verify(playerCache).removePlayer("other");
    }

    @Test
    void shouldRestoreRevokedOperatorOfPlayerWithoutLimbo() {
        // given
        Player player = mock(Player.class);
        given(player.getName()).willReturn("Bobby");
        doReturn(Collections.singletonList(player)).when(bukkitService).getOnlinePlayers();
        given(settings.getProperty(RestrictionSettings.SAVE_QUIT_LOCATION)).willReturn(false);

        // when
        onShutdownPlayerSaver.saveAllPlayers();

        // then
        verify(limboService).restorePendingOperator(player);
        verify(playerCache).removePlayer("bobby");
    }

    private Player mockPlayerInLimbo(String name) {
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        given(limboService.hasLimboPlayer(player)).willReturn(true);
        return player;
    }
}
