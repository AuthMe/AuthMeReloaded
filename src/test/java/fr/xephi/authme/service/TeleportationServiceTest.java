package fr.xephi.authme.service;

import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.data.limbo.LimboPlayer;
import fr.xephi.authme.events.FirstSpawnTeleportEvent;
import fr.xephi.authme.events.SpawnTeleportEvent;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.SpawnLoader;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Arrays;

import static fr.xephi.authme.service.BukkitServiceTestHelper.setBukkitServiceToScheduleSyncTaskFromOptionallyAsyncTask;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Test for {@link TeleportationService}.
 */
@RunWith(MockitoJUnitRunner.class)
public class TeleportationServiceTest {

    @InjectMocks
    private TeleportationService teleportationService;

    @Mock
    private Settings settings;

    @Mock
    private BukkitService bukkitService;

    @Mock
    private SpawnLoader spawnLoader;

    @Mock
    private PlayerCache playerCache;

    @Before
    public void setUpForcedWorlds() {
        given(settings.getProperty(RestrictionSettings.FORCE_SPAWN_ON_WORLDS))
            .willReturn(Arrays.asList("forced1", "OtherForced"));
        teleportationService.reload();

        given(settings.getProperty(RestrictionSettings.NO_TELEPORT)).willReturn(false);
    }

    // -----------
    // JOINING
    // -----------
    @Test
    public void shouldNotTeleportPlayerOnJoin() {
        // given
        given(settings.getProperty(RestrictionSettings.NO_TELEPORT)).willReturn(true);
        Player player = mock(Player.class);

        // when
        teleportationService.teleportOnJoin(player);

        // then
        verifyNoInteractions(player);
        verifyNoInteractions(bukkitService);
    }

    @Test
    public void shouldTeleportPlayerToFirstSpawn() {
        // given
        Player player = mock(Player.class);
        given(player.hasPlayedBefore()).willReturn(false);
        given(player.isOnline()).willReturn(true);
        Location firstSpawn = mockLocation();
        given(spawnLoader.getFirstSpawn()).willReturn(firstSpawn);
        setBukkitServiceToScheduleSyncTaskFromOptionallyAsyncTask(bukkitService);

        // when
        teleportationService.teleportNewPlayerToFirstSpawn(player);

        // then
        verify(player).teleport(firstSpawn);
        verify(bukkitService).callEvent(any(FirstSpawnTeleportEvent.class));
        verify(spawnLoader).getFirstSpawn();
        verify(spawnLoader, never()).getSpawnLocation(any(Player.class));
    }

    @Test
    public void shouldTeleportPlayerToSpawn() {
        // given
        given(settings.getProperty(RestrictionSettings.TELEPORT_UNAUTHED_TO_SPAWN)).willReturn(true);
        Player player = mock(Player.class);
        given(player.isOnline()).willReturn(true);
        Location spawn = mockLocation();
        given(spawnLoader.getSpawnLocation(player)).willReturn(spawn);
        setBukkitServiceToScheduleSyncTaskFromOptionallyAsyncTask(bukkitService);

        // when
        teleportationService.teleportOnJoin(player);

        // then
        verify(player).teleport(spawn);
        verify(bukkitService).callEvent(any(SpawnTeleportEvent.class));
        verify(spawnLoader).getSpawnLocation(player);
    }

    @Test
    // No first spawn defined, no teleport settings enabled
    public void shouldNotTeleportNewPlayer() {
        // given
        Player player = mock(Player.class);
        given(spawnLoader.getFirstSpawn()).willReturn(null);

        // when
        teleportationService.teleportNewPlayerToFirstSpawn(player);

        // then
        verify(player, never()).teleport(any(Location.class));
        verify(spawnLoader).getFirstSpawn();
        verify(spawnLoader, never()).getSpawnLocation(any(Player.class));
        verifyNoInteractions(bukkitService);
    }

    @Test
    public void shouldNotTeleportPlayerToFirstSpawnIfNoTeleportEnabled() {
        // given
        Player player = mock(Player.class);
        given(settings.getProperty(RestrictionSettings.NO_TELEPORT)).willReturn(true);

        // when
        teleportationService.teleportNewPlayerToFirstSpawn(player);

        // then
        verify(player, never()).teleport(any(Location.class));
        verifyNoInteractions(bukkitService);
    }

    @Test
    public void shouldNotTeleportNotNewPlayerToFirstSpawn() {
        // given
        Player player = mock(Player.class);
        given(settings.getProperty(RestrictionSettings.NO_TELEPORT)).willReturn(false);

        // when
        teleportationService.teleportNewPlayerToFirstSpawn(player);

        // then
        verify(player, never()).teleport(any(Location.class));
        verifyNoInteractions(bukkitService);
    }

    @Test
    public void shouldNotTeleportPlayerForRemovedLocationInEvent() {
        // given
        final Player player = mock(Player.class);
        Location spawn = mockLocation();
        given(spawnLoader.getSpawnLocation(player)).willReturn(spawn);
        given(settings.getProperty(RestrictionSettings.TELEPORT_UNAUTHED_TO_SPAWN)).willReturn(true);
        doAnswer(invocation -> {
            SpawnTeleportEvent event = (SpawnTeleportEvent) invocation.getArguments()[0];
            assertThat(event.getPlayer(), equalTo(player));
            event.setTo(null);
            return null;
        }).when(bukkitService).callEvent(any(SpawnTeleportEvent.class));
        setBukkitServiceToScheduleSyncTaskFromOptionallyAsyncTask(bukkitService);

        // when
        teleportationService.teleportOnJoin(player);

        // then
        verify(bukkitService).callEvent(any(SpawnTeleportEvent.class));
        verify(player, never()).teleport(any(Location.class));
    }

    @Test
    public void shouldNotTeleportPlayerForCanceledEvent() {
        // given
        final Player player = mock(Player.class);
        Location spawn = mockLocation();
        given(spawnLoader.getSpawnLocation(player)).willReturn(spawn);
        given(settings.getProperty(RestrictionSettings.TELEPORT_UNAUTHED_TO_SPAWN)).willReturn(true);
        doAnswer(invocation -> {
            SpawnTeleportEvent event = (SpawnTeleportEvent) invocation.getArguments()[0];
            assertThat(event.getPlayer(), equalTo(player));
            event.setCancelled(true);
            return null;
        }).when(bukkitService).callEvent(any(SpawnTeleportEvent.class));
        setBukkitServiceToScheduleSyncTaskFromOptionallyAsyncTask(bukkitService);

        // when
        teleportationService.teleportOnJoin(player);

        // then
        verify(bukkitService).callEvent(any(SpawnTeleportEvent.class));
        verify(player, never()).teleport(any(Location.class));
    }

    // ---------
    // LOGIN
    // ---------
    @Test
    public void shouldNotTeleportUponLogin() {
        // given
        given(settings.getProperty(RestrictionSettings.NO_TELEPORT)).willReturn(true);
        Player player = mock(Player.class);
        PlayerAuth auth = mock(PlayerAuth.class);
        LimboPlayer limbo = mock(LimboPlayer.class);

        // when
        teleportationService.teleportOnLogin(player, auth, limbo);

        // then
        verifyNoInteractions(player, auth, limbo, bukkitService, spawnLoader);
    }

    @Test
    public void shouldTeleportPlayerToSpawnAfterLogin() {
        // given
        given(settings.getProperty(RestrictionSettings.FORCE_SPAWN_LOCATION_AFTER_LOGIN)).willReturn(true);
        Player player = mock(Player.class);
        given(player.isOnline()).willReturn(true);
        Location spawn = mockLocation();
        given(spawnLoader.getSpawnLocation(player)).willReturn(spawn);
        PlayerAuth auth = mock(PlayerAuth.class);
        LimboPlayer limbo = mock(LimboPlayer.class);
        Location limboLocation = mockLocation();
        given(limboLocation.getWorld().getName()).willReturn("forced1");
        given(limbo.getLocation()).willReturn(limboLocation);
        setBukkitServiceToScheduleSyncTaskFromOptionallyAsyncTask(bukkitService);

        // when
        teleportationService.teleportOnLogin(player, auth, limbo);

        // then
        verify(player).teleport(spawn);
    }

    @Test
    // Check that the worlds for "force spawn loc after login" are case-sensitive
    public void shouldNotTeleportToSpawnForOtherCaseInWorld() {
        // given
        given(settings.getProperty(RestrictionSettings.FORCE_SPAWN_LOCATION_AFTER_LOGIN)).willReturn(true);
        given(settings.getProperty(RestrictionSettings.TELEPORT_UNAUTHED_TO_SPAWN)).willReturn(false);
        Player player = mock(Player.class);
        Location spawn = mockLocation();
        PlayerAuth auth = mock(PlayerAuth.class);
        LimboPlayer limbo = mock(LimboPlayer.class);
        Location limboLocation = mockLocation();
        given(limboLocation.getWorld().getName()).willReturn("Forced1"); // different case
        given(limbo.getLocation()).willReturn(limboLocation);

        // when
        teleportationService.teleportOnLogin(player, auth, limbo);

        // then
        verify(player, never()).teleport(spawn);
        verifyNoInteractions(bukkitService, spawnLoader);
    }

    @Test
    public void shouldTeleportBackToPlayerAuthLocation() {
        // given
        given(settings.getProperty(RestrictionSettings.TELEPORT_UNAUTHED_TO_SPAWN)).willReturn(true);
        given(settings.getProperty(RestrictionSettings.SAVE_QUIT_LOCATION)).willReturn(true);

        PlayerAuth auth = createAuthWithLocation();
        auth.setWorld("myWorld");
        World world = mock(World.class);
        given(bukkitService.getWorld("myWorld")).willReturn(world);

        Player player = mock(Player.class);
        given(player.isOnline()).willReturn(true);
        LimboPlayer limbo = mock(LimboPlayer.class);
        Location limboLocation = mockLocation();
        given(limbo.getLocation()).willReturn(limboLocation);
        setBukkitServiceToScheduleSyncTaskFromOptionallyAsyncTask(bukkitService);

        // when
        teleportationService.teleportOnLogin(player, auth, limbo);

        // then
        ArgumentCaptor<Location> locationCaptor = ArgumentCaptor.forClass(Location.class);
        verify(player).teleport(locationCaptor.capture());
        assertCorrectLocation(locationCaptor.getValue(), auth, world);
    }

    @Test
    public void shouldTeleportAccordingToPlayerAuthAndPlayerWorldAsFallback() {
        // given
        given(settings.getProperty(RestrictionSettings.TELEPORT_UNAUTHED_TO_SPAWN)).willReturn(true);
        given(settings.getProperty(RestrictionSettings.SAVE_QUIT_LOCATION)).willReturn(true);

        PlayerAuth auth = createAuthWithLocation();
        auth.setWorld("myWorld");
        given(bukkitService.getWorld("myWorld")).willReturn(null);

        Player player = mock(Player.class);
        given(player.isOnline()).willReturn(true);
        World world = mock(World.class);
        given(player.getWorld()).willReturn(world);
        LimboPlayer limbo = mock(LimboPlayer.class);
        Location limboLocation = mockLocation();
        given(limbo.getLocation()).willReturn(limboLocation);
        setBukkitServiceToScheduleSyncTaskFromOptionallyAsyncTask(bukkitService);

        // when
        teleportationService.teleportOnLogin(player, auth, limbo);

        // then
        ArgumentCaptor<Location> locationCaptor = ArgumentCaptor.forClass(Location.class);
        verify(player).teleport(locationCaptor.capture());
        assertCorrectLocation(locationCaptor.getValue(), auth, world);
    }

    @Test
    public void shouldTeleportWithLimboPlayerIfAuthYCoordIsNotSet() {
        // given
        given(settings.getProperty(RestrictionSettings.TELEPORT_UNAUTHED_TO_SPAWN)).willReturn(true);
        given(settings.getProperty(RestrictionSettings.SAVE_QUIT_LOCATION)).willReturn(true);

        PlayerAuth auth = createAuthWithLocation();
        auth.setQuitLocY(0.0);
        auth.setWorld("authWorld");
        Player player = mock(Player.class);
        given(player.isOnline()).willReturn(true);
        LimboPlayer limbo = mock(LimboPlayer.class);
        Location location = mockLocation();
        given(limbo.getLocation()).willReturn(location);
        setBukkitServiceToScheduleSyncTaskFromOptionallyAsyncTask(bukkitService);

        // when
        teleportationService.teleportOnLogin(player, auth, limbo);

        // then
        verify(player).teleport(location);
        verify(bukkitService, never()).getWorld(anyString());
    }

    @Test
    public void shouldTeleportWithLimboPlayerIfSaveQuitLocIsDisabled() {
        // given
        given(settings.getProperty(RestrictionSettings.TELEPORT_UNAUTHED_TO_SPAWN)).willReturn(true);
        given(settings.getProperty(RestrictionSettings.SAVE_QUIT_LOCATION)).willReturn(false);

        PlayerAuth auth = createAuthWithLocation();
        Player player = mock(Player.class);
        given(player.isOnline()).willReturn(true);
        LimboPlayer limbo = mock(LimboPlayer.class);
        Location location = mockLocation();
        given(limbo.getLocation()).willReturn(location);
        setBukkitServiceToScheduleSyncTaskFromOptionallyAsyncTask(bukkitService);

        // when
        teleportationService.teleportOnLogin(player, auth, limbo);

        // then
        verify(player).teleport(location);
    }

    @Test
    public void shouldNotTeleportForNullLocationInLimboPlayer() {
        // given
        given(settings.getProperty(RestrictionSettings.SAVE_QUIT_LOCATION)).willReturn(false);
        given(settings.getProperty(RestrictionSettings.TELEPORT_UNAUTHED_TO_SPAWN)).willReturn(true);

        PlayerAuth auth = PlayerAuth.builder().name("bobby").build();
        Player player = mock(Player.class);
        LimboPlayer limbo = mock(LimboPlayer.class);

        // when
        teleportationService.teleportOnLogin(player, auth, limbo);

        // then
        verifyNoInteractions(player);
        verify(limbo, times(2)).getLocation();
    }

    private static void assertCorrectLocation(Location location, PlayerAuth auth, World world) {
        assertThat(location.getX(), equalTo(auth.getQuitLocX()));
        assertThat(location.getY(), equalTo(auth.getQuitLocY()));
        assertThat(location.getZ(), equalTo(auth.getQuitLocZ()));
        assertThat(location.getWorld(), equalTo(world));
    }

    // We check that the World in Location is set, this method creates a mock World in Location for us
    private static Location mockLocation() {
        Location location = mock(Location.class);
        given(location.getWorld()).willReturn(mock(World.class));
        return location;
    }

    private static PlayerAuth createAuthWithLocation() {
        return PlayerAuth.builder()
            .name("bobby")
            .locX(123.45).locY(23.4).locZ(-4.567)
            .build();
    }

}
