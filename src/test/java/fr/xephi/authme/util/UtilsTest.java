package fr.xephi.authme.util;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.AuthMeMockUtil;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.settings.Settings;
import org.bukkit.GameMode;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.mockito.Mockito;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.logging.Logger;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * Test for the {@link Utils} class.
 */
@Ignore
// TODO ljacqu 20151126: Fix tests
public class UtilsTest {

    private AuthMe authMeMock;
    private PermissionsManager permissionsManagerMock;

    @Before
    public void setUpMocks() {
        WrapperMock w = WrapperMock.getInstance();
        authMeMock = w.getAuthMe();
        permissionsManagerMock = Mockito.mock(PermissionsManager.class);
        when(authMeMock.getPermissionsManager()).thenReturn(permissionsManagerMock);
    }

    @Test
    public void shouldForceSurvivalGameMode() {
        // given
        Player player = mock(Player.class);
        given(permissionsManagerMock.hasPermission(player, "authme.bypassforcesurvival")).willReturn(false);

        // when
        Utils.forceGM(player);

        // then
        verify(authMeMock).getPermissionsManager();
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
        verify(authMeMock).getPermissionsManager();
        verify(permissionsManagerMock).hasPermission(player, "authme.bypassforcesurvival");
        verify(player, never()).setGameMode(any(GameMode.class));
    }

    @Test
    public void shouldNotAddToNormalGroupIfPermissionsAreDisabled() {
        // given
        Settings.isPermissionCheckEnabled = false;
        Player player = mock(Player.class);

        // when
        boolean result = Utils.addNormal(player, "test_group");

        // then
        assertThat(result, equalTo(false));
        verify(authMeMock, never()).getPermissionsManager();
    }

    @Test
    public void shouldNotAddToNormalGroupIfPermManagerIsNull() {
        // given
        Settings.isPermissionCheckEnabled = true;
        given(authMeMock.getPermissionsManager()).willReturn(null);
        Player player = mock(Player.class);
        AuthMeMockUtil.mockSingletonForClass(ConsoleLogger.class, "wrapper", Wrapper.getInstance());

        // when
        boolean result = Utils.addNormal(player, "test_group");

        // then
        assertThat(result, equalTo(false));
        verify(authMeMock).getPermissionsManager();
    }

    @Test
    // Note ljacqu 20151122: This is a heavy test setup with reflections... If it causes trouble, skip it with @Ignore
    public void shouldRetrieveListOfOnlinePlayersFromReflectedMethod() {
        // given
        setField("getOnlinePlayersIsCollection", false);
        try {
            setField("getOnlinePlayers", UtilsTest.class.getDeclaredMethod("onlinePlayersImpl"));
        } catch (NoSuchMethodException e) {
            throw new RuntimeException("Cannot initialize test with custom test method", e);
        }

        // when
        Collection<? extends Player> players = Utils.getOnlinePlayers();

        // then
        assertThat(players, hasSize(2));
    }

    private static void setField(String name, Object value) {
        try {
            Field field = Utils.class.getDeclaredField(name);
            field.setAccessible(true);
            field.set(null, value);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Could not set field '" + name + "'", e);
        }
    }

    // Note: This method is used through reflections
    public static Player[] onlinePlayersImpl() {
        return new Player[]{
            mock(Player.class), mock(Player.class)
        };
    }

}
