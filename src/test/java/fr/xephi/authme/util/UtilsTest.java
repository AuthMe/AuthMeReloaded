package fr.xephi.authme.util;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.AuthMeMockUtil;
import fr.xephi.authme.permission.PermissionsManager;
import org.bukkit.GameMode;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.Before;
import org.junit.Test;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test for the {@link Utils} class.
 */
public class UtilsTest {

    private AuthMe authMeMock;
    private PermissionsManager permissionsManagerMock;

    @Before
    public void setUpMocks() {
        AuthMeMockUtil.mockAuthMeInstance();
        authMeMock = AuthMe.getInstance();

        permissionsManagerMock = mock(PermissionsManager.class);
        when(authMeMock.getPermissionsManager()).thenReturn(permissionsManagerMock);

        Server serverMock = mock(Server.class);
        when(authMeMock.getGameServer()).thenReturn(serverMock);

        BukkitScheduler schedulerMock = mock(BukkitScheduler.class);
        when(serverMock.getScheduler()).thenReturn(schedulerMock);
        when(schedulerMock.runTaskAsynchronously(any(Plugin.class), any(Runnable.class)))
                .thenReturn(mock(BukkitTask.class));
    }

    @Test
    public void shouldForceSurvivalGameMode() {
        // given
        Player player = mock(Player.class);
        given(permissionsManagerMock.hasPermission(player, "authme.bypassforcesurvival")).willReturn(false);

        // when
        Utils.forceGM(player);

        // then
        verify(player).setGameMode(GameMode.SURVIVAL);
    }

    @Test
    public void shouldNotForceGameModeForUserWithBypassPermission() {
        // given
        Player player = mock(Player.class);
        given(permissionsManagerMock.hasPermission(player, "authme.bypassforcesurvival")).willReturn(true);

        // when
        Utils.forceGM(player);

        // then
        verify(player, never()).setGameMode(GameMode.SURVIVAL);
    }

}
