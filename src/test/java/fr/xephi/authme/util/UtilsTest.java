package fr.xephi.authme.util;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.AuthMeMockUtil;
import fr.xephi.authme.WrapperMock;
import fr.xephi.authme.permission.PermissionsManager;
import org.bukkit.Server;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.lang.reflect.Field;
import java.util.Collection;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Test for the {@link Utils} class.
 */
@Ignore
// TODO ljacqu 20151123: Fix test setup
public class UtilsTest {

    private AuthMe authMeMock;
    private PermissionsManager permissionsManagerMock;
    private Wrapper wrapperMock;

    @Before
    public void setUpMocks() {
        authMeMock = AuthMeMockUtil.mockAuthMeInstance();

        // We need to create the Wrapper mock before injecting it into Utils because it runs a lot of  code in
        // a static block which needs the proper mocks to be set up.
        wrapperMock = new WrapperMock(authMeMock);
        Server serverMock = wrapperMock.getServer();

        BukkitScheduler schedulerMock = mock(BukkitScheduler.class);
        when(serverMock.getScheduler()).thenReturn(schedulerMock);


        when(schedulerMock.runTaskAsynchronously(any(Plugin.class), any(Runnable.class)))
            .thenReturn(mock(BukkitTask.class));

        System.out.println("Initialized scheduler mock for server mock");
        AuthMeMockUtil.insertMockWrapperInstance(Utils.class, "wrapper", (WrapperMock) wrapperMock);
        System.out.println("Iniadfk");

        permissionsManagerMock = mock(PermissionsManager.class);
        when(authMeMock.getPermissionsManager()).thenReturn(permissionsManagerMock);
    }

    // TODO ljacques 20151122: The tests for Utils.forceGM somehow can't be set up with the mocks correctly
    /*@Test
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
        verify(authMeMock).getPermissionsManager();
        verify(permissionsManagerMock).hasPermission(player, "authme.bypassforcesurvival");
        verify(player, never()).setGameMode(any(GameMode.class));
    }*/

    @Test
    // Note ljacqu 20151122: This is a heavy test setup with Reflections... If it causes trouble, skip it with @Ignore
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

    public static Player[] onlinePlayersImpl() {
        return new Player[]{
            mock(Player.class), mock(Player.class)
        };
    }

}
