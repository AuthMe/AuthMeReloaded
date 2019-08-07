package fr.xephi.authme.data.limbo;

import ch.jalu.injector.testing.DelayedInjectionRunner;
import ch.jalu.injector.testing.InjectDelayed;
import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.limbo.persistence.LimboPersistence;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.SpawnLoader;
import fr.xephi.authme.settings.properties.LimboSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Test for {@link LimboService}, and {@link LimboServiceHelper}.
 */
@RunWith(DelayedInjectionRunner.class)
public class LimboServiceTest {

    @InjectDelayed
    private LimboService limboService;

    @InjectDelayed
    private LimboServiceHelper limboServiceHelper;

    @Mock
    private SpawnLoader spawnLoader;

    @Mock
    private PermissionsManager permissionsManager;

    @Mock
    private Settings settings;

    @Mock
    private LimboPlayerTaskManager taskManager;

    @Mock
    private LimboPersistence limboPersistence;

    @Mock
    private AuthGroupHandler authGroupHandler;

    @BeforeClass
    public static void initLogger() {
        TestHelper.setupLogger();
    }

    @Before
    public void mockSettings() {
        given(settings.getProperty(RestrictionSettings.ALLOW_UNAUTHED_MOVEMENT)).willReturn(false);
    }

    @Test
    public void shouldCreateLimboPlayer() {
        // given
        Player player = newPlayer("Bobby", true, false);
        Location playerLoc = mock(Location.class);
        given(spawnLoader.getPlayerLocationOrSpawn(player)).willReturn(playerLoc);
        given(permissionsManager.hasGroupSupport()).willReturn(true);
        given(permissionsManager.getGroups(player)).willReturn(Collections.singletonList("permgrwp"));
        given(settings.getProperty(LimboSettings.RESTORE_ALLOW_FLIGHT)).willReturn(AllowFlightRestoreType.ENABLE);

        // when
        limboService.createLimboPlayer(player, true);

        // then
        verify(taskManager).registerMessageTask(eq(player), any(LimboPlayer.class), eq(LimboMessageType.LOG_IN));
        verify(taskManager).registerTimeoutTask(eq(player), any(LimboPlayer.class));
        verify(player).setAllowFlight(false);

        assertThat(limboService.hasLimboPlayer("Bobby"), equalTo(true));
        LimboPlayer limbo = limboService.getLimboPlayer("Bobby");
        verify(authGroupHandler).setGroup(player, limbo, AuthGroupType.REGISTERED_UNAUTHENTICATED);
        assertThat(limbo, not(nullValue()));
        assertThat(limbo.isOperator(), equalTo(true));
        assertThat(limbo.isCanFly(), equalTo(false));
        assertThat(limbo.getLocation(), equalTo(playerLoc));
        assertThat(limbo.getGroups(), equalTo(Collections.singletonList("permgrwp")));
    }

    @Test
    public void shouldNotKeepOpStatusForUnregisteredPlayer() {
        // given
        Player player = newPlayer("CharleS", true, true);
        Location playerLoc = mock(Location.class);
        given(spawnLoader.getPlayerLocationOrSpawn(player)).willReturn(playerLoc);
        given(permissionsManager.hasGroupSupport()).willReturn(false);
        given(settings.getProperty(LimboSettings.RESTORE_ALLOW_FLIGHT)).willReturn(AllowFlightRestoreType.RESTORE);

        // when
        limboService.createLimboPlayer(player, false);

        // then
        verify(taskManager).registerMessageTask(eq(player), any(LimboPlayer.class), eq(LimboMessageType.REGISTER));
        verify(taskManager).registerTimeoutTask(eq(player), any(LimboPlayer.class));
        verify(permissionsManager, only()).hasGroupSupport();
        verify(player).setAllowFlight(false);

        LimboPlayer limbo = limboService.getLimboPlayer("charles");
        verify(authGroupHandler).setGroup(player, limbo, AuthGroupType.UNREGISTERED);
        assertThat(limbo, not(nullValue()));
        assertThat(limbo.isOperator(), equalTo(false));
        assertThat(limbo.isCanFly(), equalTo(true));
        assertThat(limbo.getLocation(), equalTo(playerLoc));
        assertThat(limbo.getGroups(), equalTo(Collections.emptyList()));
    }

    @Test
    public void shouldClearTasksOnAlreadyExistingLimbo() {
        // given
        LimboPlayer existingLimbo = mock(LimboPlayer.class);
        getLimboMap().put("carlos", existingLimbo);
        Player player = newPlayer("Carlos");
        given(settings.getProperty(LimboSettings.RESTORE_ALLOW_FLIGHT)).willReturn(AllowFlightRestoreType.ENABLE);

        // when
        limboService.createLimboPlayer(player, false);

        // then
        verify(existingLimbo).clearTasks();
        LimboPlayer newLimbo = limboService.getLimboPlayer("Carlos");
        verify(authGroupHandler).setGroup(player, newLimbo, AuthGroupType.UNREGISTERED);
        assertThat(newLimbo, not(nullValue()));
        assertThat(newLimbo, not(sameInstance(existingLimbo)));
    }

    @Test
    public void shouldRestoreData() {
        // given
        LimboPlayer limbo = Mockito.spy(convertToLimboPlayer(
            newPlayer("John", true, false), null, Collections.emptyList()));
        getLimboMap().put("john", limbo);
        Player player = newPlayer("John", false, false);

        given(settings.getProperty(LimboSettings.RESTORE_ALLOW_FLIGHT)).willReturn(AllowFlightRestoreType.ENABLE);

        // when
        limboService.restoreData(player);

        // then
        verify(player).setOp(true);
        verify(player).setAllowFlight(true);
        verify(limbo).clearTasks();
        verify(authGroupHandler).setGroup(player, limbo, AuthGroupType.LOGGED_IN);
        assertThat(limboService.hasLimboPlayer("John"), equalTo(false));
    }

    @Test
    public void shouldHandleMissingLimboPlayerWhileRestoring() {
        // given
        Player player = newPlayer("Test");

        // when
        limboService.restoreData(player);

        // then
        verify(player, only()).getName();
        verify(authGroupHandler).setGroup(player, null, AuthGroupType.LOGGED_IN);
    }

    @Test
    public void shouldReplaceTasks() {
        // given
        LimboPlayer limbo = mock(LimboPlayer.class);
        getLimboMap().put("jeff", limbo);
        Player player = newPlayer("JEFF");


        // when
        limboService.replaceTasksAfterRegistration(player);

        // then
        verify(taskManager).registerTimeoutTask(player, limbo);
        verify(taskManager).registerMessageTask(player, limbo, LimboMessageType.LOG_IN);
        verify(authGroupHandler).setGroup(player, limbo, AuthGroupType.REGISTERED_UNAUTHENTICATED);
    }

    @Test
    public void shouldHandleMissingLimboForReplaceTasks() {
        // given
        Player player = newPlayer("ghost");

        // when
        limboService.replaceTasksAfterRegistration(player);

        // then
        verifyZeroInteractions(taskManager);
        verify(authGroupHandler).setGroup(player, null, AuthGroupType.REGISTERED_UNAUTHENTICATED);
    }

    private static Player newPlayer(String name) {
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        return player;
    }

    private static Player newPlayer(String name, boolean isOp, boolean canFly) {
        Player player = newPlayer(name);
        given(player.isOp()).willReturn(isOp);
        given(player.getAllowFlight()).willReturn(canFly);
        return player;
    }

    private static LimboPlayer convertToLimboPlayer(Player player, Location location, Collection<String> groups) {
        return new LimboPlayer(location, player.isOp(), groups, player.getAllowFlight());
    }

    private Map<String, LimboPlayer> getLimboMap() {
        return ReflectionTestUtils.getFieldValue(LimboService.class, limboService, "entries");
    }
}
