package fr.xephi.authme.listener;

import fr.xephi.authme.data.QuickCommandsProtectionManager;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PlayerStatePermission;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.service.AntiBotService;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.JoinMessageService;
import fr.xephi.authme.service.TeleportationService;
import fr.xephi.authme.service.ValidationService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.SpawnLoader;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.block.SignChangeEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.AsyncPlayerPreLoginEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerEditBookEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractAtEntityEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.bukkit.inventory.InventoryView;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.UUID;

import static com.google.common.collect.Sets.newHashSet;
import static fr.xephi.authme.listener.EventCancelVerifier.withServiceMock;
import static fr.xephi.authme.service.BukkitServiceTestHelper.setBukkitServiceToScheduleSyncDelayedTaskWithDelay;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.verifyNoMoreInteractions;

/**
 * Test for {@link PlayerListener}.
 */
@RunWith(MockitoJUnitRunner.class)
public class PlayerListenerTest {

    @InjectMocks
    private PlayerListener listener;

    @Mock
    private Settings settings;
    @Mock
    private Messages messages;
    @Mock
    private DataSource dataSource;
    @Mock
    private AntiBotService antiBotService;
    @Mock
    private Management management;
    @Mock
    private BukkitService bukkitService;
    @Mock
    private SpawnLoader spawnLoader;
    @Mock
    private OnJoinVerifier onJoinVerifier;
    @Mock
    private ListenerService listenerService;
    @Mock
    private TeleportationService teleportationService;
    @Mock
    private ValidationService validationService;
    @Mock
    private JoinMessageService joinMessageService;
    @Mock
    private QuickCommandsProtectionManager quickCommandsProtectionManager;
    @Mock
    private PermissionsManager permissionsManager;

    /**
     * #831: If a player is kicked because of "logged in from another location", the kick
     * should be CANCELED when single session is enabled.
     */
    @Test
    public void shouldCancelKick() {
        // given
        given(settings.getProperty(RestrictionSettings.FORCE_SINGLE_SESSION)).willReturn(true);
        Player player = mock(Player.class);
        PlayerKickEvent event = new PlayerKickEvent(player, "You logged in from another location", "");

        // when
        listener.onPlayerKick(event);

        // then
        assertThat(event.isCancelled(), equalTo(true));
        verifyNoInteractions(player, management, antiBotService);
    }

    @Test
    public void shouldNotCancelKick() {
        // given
        given(settings.getProperty(RestrictionSettings.FORCE_SINGLE_SESSION)).willReturn(false);
        String name = "Bobby";
        Player player = mockPlayerWithName(name);
        PlayerKickEvent event = new PlayerKickEvent(player, "You logged in from another location", "");
        given(antiBotService.wasPlayerKicked(name)).willReturn(false);

        // when
        listener.onPlayerKick(event);

        // then
        assertThat(event.isCancelled(), equalTo(false));
        verify(antiBotService).wasPlayerKicked(name);
        verify(management).performQuit(player);
    }

    @Test
    public void shouldNotCancelOrdinaryKick() {
        // given
        given(settings.getProperty(RestrictionSettings.FORCE_SINGLE_SESSION)).willReturn(true);
        String name = "Bobby";
        Player player = mockPlayerWithName(name);
        PlayerKickEvent event = new PlayerKickEvent(player, "No longer desired here!", "");
        given(antiBotService.wasPlayerKicked(name)).willReturn(true);

        // when
        listener.onPlayerKick(event);

        // then
        assertThat(event.isCancelled(), equalTo(false));
        verify(antiBotService).wasPlayerKicked(name);
        verifyNoInteractions(management);
    }

    @Test
    public void shouldHandleSimpleCancelableEvents() {
        withServiceMock(listenerService)
            .check(listener::onPlayerShear, PlayerShearEntityEvent.class)
            .check(listener::onPlayerFish, PlayerFishEvent.class)
            .check(listener::onPlayerBedEnter, PlayerBedEnterEvent.class)
            .check(listener::onPlayerDropItem, PlayerDropItemEvent.class)
            .check(listener::onPlayerHitPlayerEvent, EntityDamageByEntityEvent.class)
            .check(listener::onPlayerConsumeItem, PlayerItemConsumeEvent.class)
            .check(listener::onPlayerInteract, PlayerInteractEvent.class)
            .check(listener::onPlayerPickupItem, PlayerPickupItemEvent.class)
            .check(listener::onPlayerInteractEntity, PlayerInteractEntityEvent.class)
            .check(listener::onPlayerHeldItem, PlayerItemHeldEvent.class);
    }

    @Test
    public void shouldAllowEssentialsMotd() {
        // given
        given(settings.getProperty(HooksSettings.USE_ESSENTIALS_MOTD)).willReturn(true);
        PlayerCommandPreprocessEvent event = mockCommandEvent("/MOTD");

        // when
        listener.onPlayerCommandPreprocess(event);

        // then
        verify(event, only()).getMessage();
        verifyNoInteractions(listenerService, messages);
    }

    @Test
    public void shouldNotStopAllowedCommand() {
        // given
        given(settings.getProperty(HooksSettings.USE_ESSENTIALS_MOTD)).willReturn(true);
        given(settings.getProperty(RestrictionSettings.ALLOW_COMMANDS)).willReturn(newHashSet("/plugins", "/mail", "/msg"));
        PlayerCommandPreprocessEvent event = mockCommandEvent("/Mail send test Test");

        // when
        listener.onPlayerCommandPreprocess(event);

        // then
        verify(event, only()).getMessage();
        verifyNoInteractions(listenerService, messages);
    }

    @Test
    public void shouldNotCancelEventForAuthenticatedPlayer() {
        // given
        given(settings.getProperty(HooksSettings.USE_ESSENTIALS_MOTD)).willReturn(false);
        given(settings.getProperty(RestrictionSettings.ALLOW_COMMANDS)).willReturn(Collections.emptySet());
        Player player = playerWithMockedServer();
        // PlayerCommandPreprocessEvent#getPlayer is final, so create a spy instead of a mock
        PlayerCommandPreprocessEvent event = spy(new PlayerCommandPreprocessEvent(player, "/hub"));
        given(listenerService.shouldCancelEvent(player)).willReturn(false);
        given(quickCommandsProtectionManager.isAllowed(player.getName())).willReturn(true);

        // when
        listener.onPlayerCommandPreprocess(event);

        // then
        verify(event).getMessage();
        verifyNoMoreInteractions(event);
        verify(listenerService).shouldCancelEvent(player);
        verifyNoInteractions(messages);
    }

    @Test
    public void shouldCancelCommandEvent() {
        // given
        given(settings.getProperty(HooksSettings.USE_ESSENTIALS_MOTD)).willReturn(false);
        given(settings.getProperty(RestrictionSettings.ALLOW_COMMANDS)).willReturn(newHashSet("/spawn", "/help"));
        Player player = playerWithMockedServer();
        PlayerCommandPreprocessEvent event = spy(new PlayerCommandPreprocessEvent(player, "/hub"));
        given(listenerService.shouldCancelEvent(player)).willReturn(true);
        given(quickCommandsProtectionManager.isAllowed(player.getName())).willReturn(true);

        // when
        listener.onPlayerCommandPreprocess(event);

        // then
        verify(listenerService).shouldCancelEvent(player);
        verify(event).setCancelled(true);
        verify(messages).send(player, MessageKey.DENIED_COMMAND);
    }

    @Test
    public void shouldCancelFastCommandEvent() {
        // given
        given(settings.getProperty(HooksSettings.USE_ESSENTIALS_MOTD)).willReturn(false);
        given(settings.getProperty(RestrictionSettings.ALLOW_COMMANDS)).willReturn(newHashSet("/spawn", "/help"));
        Player player = playerWithMockedServer();
        PlayerCommandPreprocessEvent event = spy(new PlayerCommandPreprocessEvent(player, "/hub"));
        given(quickCommandsProtectionManager.isAllowed(player.getName())).willReturn(false);

        // when
        listener.onPlayerCommandPreprocess(event);

        // then
        verify(event).setCancelled(true);
        verify(player).kickPlayer(messages.retrieveSingle(player, MessageKey.QUICK_COMMAND_PROTECTION_KICK));
    }

    @Test
    public void shouldAllowChat() {
        // given
        given(settings.getProperty(RestrictionSettings.ALLOW_CHAT)).willReturn(true);
        AsyncPlayerChatEvent event = mock(AsyncPlayerChatEvent.class);

        // when
        listener.onPlayerChat(event);

        // then
        verifyNoInteractions(event, listenerService, messages);
    }

    @Test
    public void shouldCancelChatForUnauthedPlayer() {
        // given
        given(settings.getProperty(RestrictionSettings.ALLOW_CHAT)).willReturn(false);
        AsyncPlayerChatEvent event = newAsyncChatEvent();
        given(listenerService.shouldCancelEvent(event.getPlayer())).willReturn(true);
        given(permissionsManager.hasPermission(event.getPlayer(), PlayerStatePermission.ALLOW_CHAT_BEFORE_LOGIN)).willReturn(false);

        // when
        listener.onPlayerChat(event);

        // then
        verify(listenerService).shouldCancelEvent(event.getPlayer());
        verify(permissionsManager).hasPermission(event.getPlayer(), PlayerStatePermission.ALLOW_CHAT_BEFORE_LOGIN);
        verify(event).setCancelled(true);
        verify(messages).send(event.getPlayer(), MessageKey.DENIED_CHAT);
    }

    @Test
    public void shouldSendChatToEveryone() {
        // given
        given(settings.getProperty(RestrictionSettings.ALLOW_CHAT)).willReturn(false);
        AsyncPlayerChatEvent event = newAsyncChatEvent();
        given(listenerService.shouldCancelEvent(event.getPlayer())).willReturn(false);
        given(settings.getProperty(RestrictionSettings.HIDE_CHAT)).willReturn(false);

        // when
        listener.onPlayerChat(event);

        // then
        verify(listenerService).shouldCancelEvent(event.getPlayer());
        verifyNoInteractions(event, messages);
    }

    @Test
    public void shouldHideChatFromUnauthed() {
        // given
        given(settings.getProperty(RestrictionSettings.ALLOW_CHAT)).willReturn(false);
        AsyncPlayerChatEvent event = newAsyncChatEvent();
        given(listenerService.shouldCancelEvent(event.getPlayer())).willReturn(false);
        given(settings.getProperty(RestrictionSettings.HIDE_CHAT)).willReturn(true);
        List<Player> recipients = new ArrayList<>(event.getRecipients());
        given(listenerService.shouldCancelEvent(recipients.get(0))).willReturn(true);

        // when
        listener.onPlayerChat(event);

        // then
        verify(listenerService).shouldCancelEvent(event.getPlayer());
        // message sender + 3 recipients = 4
        verify(listenerService, times(4)).shouldCancelEvent(any(Player.class));
        verify(event, never()).setCancelled(anyBoolean());
        assertThat(event.getRecipients(), containsInAnyOrder(recipients.get(1), recipients.get(2)));
    }

    @Test
    public void shouldCancelChatEventForNoRemainingRecipients() {
        // given
        given(settings.getProperty(RestrictionSettings.ALLOW_CHAT)).willReturn(false);
        AsyncPlayerChatEvent event = newAsyncChatEvent();
        given(listenerService.shouldCancelEvent(any(Player.class))).willReturn(true);
        given(listenerService.shouldCancelEvent(event.getPlayer())).willReturn(false);
        given(settings.getProperty(RestrictionSettings.HIDE_CHAT)).willReturn(true);

        // when
        listener.onPlayerChat(event);

        // then
        verify(listenerService).shouldCancelEvent(event.getPlayer());
        // message sender + 3 recipients = 4
        verify(listenerService, times(4)).shouldCancelEvent(any(Player.class));
        verify(event).setCancelled(true);
        assertThat(event.getRecipients(), empty());
    }

    @Test
    public void shouldAllowChatForBypassPermission() {
        // given
        given(settings.getProperty(RestrictionSettings.ALLOW_CHAT)).willReturn(false);
        AsyncPlayerChatEvent event = newAsyncChatEvent();
        given(listenerService.shouldCancelEvent(event.getPlayer())).willReturn(true);
        given(permissionsManager.hasPermission(event.getPlayer(), PlayerStatePermission.ALLOW_CHAT_BEFORE_LOGIN)).willReturn(true);
        given(settings.getProperty(RestrictionSettings.HIDE_CHAT)).willReturn(false);

        // when
        listener.onPlayerChat(event);

        // then
        assertThat(event.isCancelled(), equalTo(false));
        verify(listenerService).shouldCancelEvent(event.getPlayer());
        verify(permissionsManager).hasPermission(event.getPlayer(), PlayerStatePermission.ALLOW_CHAT_BEFORE_LOGIN);
        assertThat(event.getRecipients(), hasSize(3));
    }

    @Test
    public void shouldAllowUnlimitedMovement() {
        // given
        given(settings.getProperty(RestrictionSettings.ALLOW_UNAUTHED_MOVEMENT)).willReturn(true);
        given(settings.getProperty(RestrictionSettings.ALLOWED_MOVEMENT_RADIUS)).willReturn(0);
        Player player = mock(Player.class);
        Location location = mock(Location.class);
        PlayerMoveEvent event = spy(new PlayerMoveEvent(player, location, location));

        // when
        listener.onPlayerMove(event);

        // then
        verifyNoInteractions(event);
    }

    @Test
    public void shouldAllowFalling() {
        // given
        given(settings.getProperty(RestrictionSettings.ALLOW_UNAUTHED_MOVEMENT)).willReturn(false);
        Player player = mock(Player.class);
        Location from = new Location(null, 100, 90, 200);
        Location to = new Location(null, 100, 88, 200);
        PlayerMoveEvent event = spy(new PlayerMoveEvent(player, from, to));

        // when
        listener.onPlayerMove(event);

        // then
        verifyNoModifyingCalls(event);
    }

    @Test
    public void shouldAllowMovementForAuthedPlayer() {
        // given
        given(settings.getProperty(RestrictionSettings.ALLOW_UNAUTHED_MOVEMENT)).willReturn(false);
        Player player = mock(Player.class);
        Location from = new Location(null, 100, 90, 200);
        Location to = new Location(null, 99, 90, 200);
        PlayerMoveEvent event = spy(new PlayerMoveEvent(player, from, to));
        given(listenerService.shouldCancelEvent(player)).willReturn(false);

        // when
        listener.onPlayerMove(event);

        // then
        verify(listenerService).shouldCancelEvent(player);
        verifyNoModifyingCalls(event);
    }

    @Test
    public void shouldCancelEventForDisabledUnauthedMovement() {
        // given
        given(settings.getProperty(RestrictionSettings.ALLOW_UNAUTHED_MOVEMENT)).willReturn(false);
        Player player = mock(Player.class);
        World world = mock(World.class);
        Location from = new Location(world, 200, 70, 200);
        Location to = new Location(world, 199, 70, 199);
        PlayerMoveEvent event = spy(new PlayerMoveEvent(player, from, to));
        given(listenerService.shouldCancelEvent(player)).willReturn(true);

        // when
        listener.onPlayerMove(event);

        // then
        verify(listenerService).shouldCancelEvent(player);
        verify(event).setTo(from);
    }

    @Test
    public void shouldTeleportPlayerInDifferentWorldToSpawn() {
        // given
        given(settings.getProperty(RestrictionSettings.ALLOW_UNAUTHED_MOVEMENT)).willReturn(true);
        given(settings.getProperty(RestrictionSettings.ALLOWED_MOVEMENT_RADIUS)).willReturn(20);
        World playerWorld = mock(World.class);
        Player player = mock(Player.class);
        given(player.getWorld()).willReturn(playerWorld);
        Location from = new Location(null, 200, 70, 200);
        Location to = new Location(null, 199, 70, 199);
        PlayerMoveEvent event = spy(new PlayerMoveEvent(player, from, to));
        given(listenerService.shouldCancelEvent(player)).willReturn(true);
        given(settings.getProperty(RestrictionSettings.NO_TELEPORT)).willReturn(false);
        World world = mock(World.class);
        Location spawn = new Location(world, 0, 90, 0);
        given(spawnLoader.getSpawnLocation(player)).willReturn(spawn);

        // when
        listener.onPlayerMove(event);

        // then
        verify(listenerService).shouldCancelEvent(player);
        verify(player).teleport(spawn);
        verify(spawnLoader).getSpawnLocation(player);
        verifyNoModifyingCalls(event);
    }

    @Test
    public void shouldAllowMovementWithinRadius() {
        // given
        given(settings.getProperty(RestrictionSettings.ALLOW_UNAUTHED_MOVEMENT)).willReturn(true);
        given(settings.getProperty(RestrictionSettings.ALLOWED_MOVEMENT_RADIUS)).willReturn(12);
        World world = mock(World.class);
        Player player = mock(Player.class);
        given(player.getWorld()).willReturn(world);
        Location from = new Location(world, 200, 70, 200);
        Location to = new Location(world, 199, 69, 201);
        PlayerMoveEvent event = spy(new PlayerMoveEvent(player, from, to));
        given(player.getLocation()).willReturn(from);
        given(listenerService.shouldCancelEvent(player)).willReturn(true);
        given(settings.getProperty(RestrictionSettings.NO_TELEPORT)).willReturn(false);
        // sqrt(10^2 + 2^2 + 4^2) = 11 < 12 (allowed movement radius)
        Location spawn = new Location(world, 190, 72, 204);
        given(spawnLoader.getSpawnLocation(player)).willReturn(spawn);

        // when
        listener.onPlayerMove(event);

        // then
        verify(listenerService).shouldCancelEvent(player);
        verify(player, never()).teleport(any(Location.class));
        verify(spawnLoader).getSpawnLocation(player);
        verifyNoModifyingCalls(event);
    }

    @Test
    public void shouldRejectMovementOutsideOfRadius() {
        // given
        given(settings.getProperty(RestrictionSettings.ALLOW_UNAUTHED_MOVEMENT)).willReturn(true);
        given(settings.getProperty(RestrictionSettings.ALLOWED_MOVEMENT_RADIUS)).willReturn(12);
        World world = mock(World.class);
        Player player = mock(Player.class);
        given(player.getWorld()).willReturn(world);
        Location from = new Location(world, 200, 70, 200);
        Location to = new Location(world, 199, 69, 201);
        PlayerMoveEvent event = spy(new PlayerMoveEvent(player, from, to));
        given(player.getLocation()).willReturn(from);
        given(listenerService.shouldCancelEvent(player)).willReturn(true);
        given(settings.getProperty(RestrictionSettings.NO_TELEPORT)).willReturn(false);
        // sqrt(15^2 + 2^2 + 4^2) = 16 > 12 (allowed movement radius)
        Location spawn = new Location(world, 185, 72, 204);
        given(spawnLoader.getSpawnLocation(player)).willReturn(spawn);

        // when
        listener.onPlayerMove(event);

        // then
        verify(listenerService).shouldCancelEvent(player);
        verify(player).teleport(spawn);
        verify(spawnLoader).getSpawnLocation(player);
        verifyNoModifyingCalls(event);
    }

    @Test
    public void shouldIgnorePlayerRespawnWithNoTeleport() {
        // given
        Player player = mock(Player.class);
        Location respawnLocation = mock(Location.class);
        PlayerRespawnEvent event = spy(new PlayerRespawnEvent(player, respawnLocation, false));
        given(settings.getProperty(RestrictionSettings.NO_TELEPORT)).willReturn(true);

        // when
        listener.onPlayerRespawn(event);

        // then
        verifyNoInteractions(listenerService);
        verify(event, never()).setRespawnLocation(any());
    }

    @Test
    public void shouldIgnorePlayerRespawn() {
        // given
        Player player = mock(Player.class);
        Location respawnLocation = mock(Location.class);
        PlayerRespawnEvent event = spy(new PlayerRespawnEvent(player, respawnLocation, false));
        given(settings.getProperty(RestrictionSettings.NO_TELEPORT)).willReturn(false);
        given(listenerService.shouldCancelEvent(event)).willReturn(false);

        // when
        listener.onPlayerRespawn(event);

        // then
        verifyNoInteractions(spawnLoader);
        verify(event, never()).setRespawnLocation(any());
    }

    @Test
    public void shouldHandlePlayerRespawn() {
        // given
        Player player = mock(Player.class);
        Location originalLocation = mock(Location.class);
        Location newLocation = mock(Location.class);
        World world = mock(World.class);
        given(newLocation.getWorld()).willReturn(world);
        PlayerRespawnEvent event = spy(new PlayerRespawnEvent(player, originalLocation, false));
        given(settings.getProperty(RestrictionSettings.NO_TELEPORT)).willReturn(false);
        given(listenerService.shouldCancelEvent(event)).willReturn(true);
        given(spawnLoader.getSpawnLocation(player)).willReturn(newLocation);

        // when
        listener.onPlayerRespawn(event);

        // then
        verify(spawnLoader).getSpawnLocation(player);
        verify(event).setRespawnLocation(newLocation);
    }

    @Test
    public void shouldIgnorePlayerRespawnUnloadedWorld() {
        // given
        Player player = mock(Player.class);
        Location originalLocation = mock(Location.class);
        Location newLocation = mock(Location.class);
        given(newLocation.getWorld()).willReturn(null);
        PlayerRespawnEvent event = spy(new PlayerRespawnEvent(player, originalLocation, false));
        given(settings.getProperty(RestrictionSettings.NO_TELEPORT)).willReturn(false);
        given(listenerService.shouldCancelEvent(event)).willReturn(true);
        given(spawnLoader.getSpawnLocation(player)).willReturn(newLocation);

        // when
        listener.onPlayerRespawn(event);

        // then
        verify(spawnLoader).getSpawnLocation(player);
        verify(event, never()).setRespawnLocation(any());
    }

    @Test
    public void shouldHandlePlayerRespawnNoChanges() {
        // given
        Player player = mock(Player.class);
        Location originalLocation = mock(Location.class);
        PlayerRespawnEvent event = spy(new PlayerRespawnEvent(player, originalLocation, false));
        given(settings.getProperty(RestrictionSettings.NO_TELEPORT)).willReturn(false);
        given(listenerService.shouldCancelEvent(event)).willReturn(true);
        given(spawnLoader.getSpawnLocation(player)).willReturn(null);

        // when
        listener.onPlayerRespawn(event);

        // then
        verify(spawnLoader).getSpawnLocation(player);
        verify(event, never()).setRespawnLocation(any());
    }

    @Test
    public void shouldHandlePlayerJoining() {
        // given
        Player player = mock(Player.class);
        PlayerJoinEvent event = new PlayerJoinEvent(player, "join message");

        // when
        listener.onPlayerJoin(event);

        // then
        verify(teleportationService).teleportNewPlayerToFirstSpawn(player);
        verify(management).performJoin(player);
    }

    @Test
    public void shouldNotInterfereWithUnrestrictedUser() throws FailedVerificationException {
        // given
        String name = "Player01";
        Player player = mockPlayerWithName(name);
        PlayerLoginEvent event = spy(new PlayerLoginEvent(player, "", null));
        given(validationService.isUnrestricted(name)).willReturn(true);

        // when
        listener.onPlayerLogin(event);

        // then
        verify(validationService).isUnrestricted(name);
        verify(onJoinVerifier).checkSingleSession(name);
        verifyNoModifyingCalls(event);
        verifyNoMoreInteractions(onJoinVerifier);
    }

    @Test
    public void shouldStopHandlingForFullServer() throws FailedVerificationException {
        // given
        String name = "someone";
        Player player = mockPlayerWithName(name);
        PlayerLoginEvent event = spy(new PlayerLoginEvent(player, "", null));
        given(validationService.isUnrestricted(name)).willReturn(false);
        given(onJoinVerifier.refusePlayerForFullServer(event)).willReturn(true);

        // when
        listener.onPlayerLogin(event);

        // then
        verify(validationService).isUnrestricted(name);
        verify(onJoinVerifier).checkSingleSession(name);
        verify(onJoinVerifier).refusePlayerForFullServer(event);
        verifyNoMoreInteractions(onJoinVerifier);
        verifyNoModifyingCalls(event);
    }

    @Test
    public void shouldStopHandlingEventForBadResult() throws FailedVerificationException {
        // given
        String name = "someone";
        Player player = mockPlayerWithName(name);
        PlayerLoginEvent event = new PlayerLoginEvent(player, "", null);
        event.setResult(PlayerLoginEvent.Result.KICK_BANNED);
        event = spy(event);
        given(validationService.isUnrestricted(name)).willReturn(false);
        given(onJoinVerifier.refusePlayerForFullServer(event)).willReturn(false);

        // when
        listener.onPlayerLogin(event);

        // then
        verify(validationService).isUnrestricted(name);
        verify(onJoinVerifier).checkSingleSession(name);
        verify(onJoinVerifier).refusePlayerForFullServer(event);
        verifyNoModifyingCalls(event);
    }

    @Test
    public void shouldPerformAllJoinVerificationsSuccessfullyPreLoginLowest() throws FailedVerificationException {
        // given
        String name = "someone";
        UUID uniqueId = UUID.fromString("753493c9-33ba-4a4a-bf61-1bce9d3c9a71");
        String ip = "12.34.56.78";

        AsyncPlayerPreLoginEvent preLoginEvent = spy(new AsyncPlayerPreLoginEvent(name, mockAddrWithIp(ip), uniqueId));
        given(validationService.isUnrestricted(name)).willReturn(false);

        // when
        listener.onAsyncPlayerPreLoginEventLowest(preLoginEvent);

        // then
        verify(validationService).isUnrestricted(name);
        verify(onJoinVerifier).checkIsValidName(name);
        verifyNoInteractions(dataSource);
        verifyNoModifyingCalls(preLoginEvent);
    }

    @Test
    public void shouldKickPreLoginLowestUnresolvedHostname() {
        // given
        String name = "someone";
        UUID uniqueId = UUID.fromString("753493c9-33ba-4a4a-bf61-1bce9d3c9a71");
    
        @SuppressWarnings("ConstantConditions")
        AsyncPlayerPreLoginEvent preLoginEvent = spy(new AsyncPlayerPreLoginEvent(name, null, uniqueId));
        given(messages.retrieveSingle(name, MessageKey.KICK_UNRESOLVED_HOSTNAME)).willReturn("Unresolved hostname");

        // when
        listener.onAsyncPlayerPreLoginEventLowest(preLoginEvent);

        // then
        verify(preLoginEvent).disallow(AsyncPlayerPreLoginEvent.Result.KICK_OTHER, "Unresolved hostname");
        verifyNoMoreInteractions(onJoinVerifier);
    }

    @Test
    public void shouldPerformAllJoinVerificationsSuccessfullyPreLoginHighest() throws FailedVerificationException {
        // given
        String name = "someone";
        UUID uniqueId = UUID.fromString("753493c9-33ba-4a4a-bf61-1bce9d3c9a71");
        String ip = "12.34.56.78";

        AsyncPlayerPreLoginEvent preLoginEvent = spy(new AsyncPlayerPreLoginEvent(name, mockAddrWithIp(ip), uniqueId));
        given(validationService.isUnrestricted(name)).willReturn(false);
        PlayerAuth auth = PlayerAuth.builder().name(name).build();
        given(dataSource.getAuth(name)).willReturn(auth);

        // when
        listener.onAsyncPlayerPreLoginEventHighest(preLoginEvent);

        // then
        verify(validationService).isUnrestricted(name);
        verify(onJoinVerifier).checkKickNonRegistered(true);
        verify(onJoinVerifier).checkAntibot(name, true);
        verify(onJoinVerifier).checkNameCasing(name, auth);
        verify(onJoinVerifier).checkPlayerCountry(name, ip, true);
        verifyNoModifyingCalls(preLoginEvent);
    }

    @Test
    public void shouldPerformAllJoinVerificationsSuccessfullyLogin() {
        // given
        String name = "someone";
        Player player = mockPlayerWithName(name);
        String ip = "12.34.56.78";

        PlayerLoginEvent loginEvent = spy(new PlayerLoginEvent(player, "", mockAddrWithIp(ip)));
        given(validationService.isUnrestricted(name)).willReturn(false);
        given(onJoinVerifier.refusePlayerForFullServer(loginEvent)).willReturn(false);

        // when
        listener.onPlayerLogin(loginEvent);

        // then
        verify(validationService).isUnrestricted(name);
        verify(onJoinVerifier).refusePlayerForFullServer(loginEvent);
        verifyNoInteractions(dataSource);
        verifyNoModifyingCalls(loginEvent);
    }

    @Test
    public void shouldAbortPlayerJoinForInvalidName() throws FailedVerificationException {
        // given
        String name = "inval!dName";
        UUID uniqueId = UUID.fromString("753493c9-33ba-4a4a-bf61-1bce9d3c9a71");
        InetAddress ip = mockAddrWithIp("33.32.33.33");
        AsyncPlayerPreLoginEvent event = spy(new AsyncPlayerPreLoginEvent(name, ip, uniqueId));
        given(validationService.isUnrestricted(name)).willReturn(false);
        FailedVerificationException exception = new FailedVerificationException(
            MessageKey.INVALID_NAME_CHARACTERS, "[a-z]");
        doThrow(exception).when(onJoinVerifier).checkIsValidName(name);
        String message = "Invalid characters!";
        given(messages.retrieveSingle(name, exception.getReason(), exception.getArgs())).willReturn(message);

        // when
        listener.onAsyncPlayerPreLoginEventLowest(event);

        // then
        verify(validationService).isUnrestricted(name);
        verify(onJoinVerifier).checkIsValidName(name);
        // Check that we don't talk with the data source before performing checks that don't require it
        verifyNoInteractions(dataSource);
        verify(event).setKickMessage(message);
        verify(event).setLoginResult(AsyncPlayerPreLoginEvent.Result.KICK_OTHER);
    }

    @Test
    public void shouldRemoveMessageOnQuit() {
        // given
        given(settings.getProperty(RegistrationSettings.REMOVE_LEAVE_MESSAGE)).willReturn(true);
        given(antiBotService.wasPlayerKicked(anyString())).willReturn(false);
        Player player = mockPlayerWithName("Billy");
        PlayerQuitEvent event = new PlayerQuitEvent(player, "Player has quit the server");

        // when
        listener.onPlayerQuit(event);

        // then
        assertThat(event.getQuitMessage(), nullValue());
        verify(antiBotService).wasPlayerKicked("Billy");
        verify(management).performQuit(player);
    }

    @Test
    public void shouldRemoveMessageForUnloggedUser() {
        // given
        given(settings.getProperty(RegistrationSettings.REMOVE_LEAVE_MESSAGE)).willReturn(false);
        given(settings.getProperty(RegistrationSettings.REMOVE_UNLOGGED_LEAVE_MESSAGE)).willReturn(true);
        String name = "Joel";
        given(antiBotService.wasPlayerKicked(name)).willReturn(true);
        Player player = mockPlayerWithName(name);
        PlayerQuitEvent event = new PlayerQuitEvent(player, "Joel exits the party");
        given(listenerService.shouldCancelEvent(event)).willReturn(true);

        // when
        listener.onPlayerQuit(event);

        // then
        assertThat(event.getQuitMessage(), nullValue());
        verify(antiBotService).wasPlayerKicked(name);
        verifyNoInteractions(management);
    }

    @Test
    public void shouldProcessPlayerAndKeepQuitMessage() {
        // given
        String name = "Louis";
        Player player = mockPlayerWithName(name);
        given(settings.getProperty(RegistrationSettings.REMOVE_LEAVE_MESSAGE)).willReturn(false);
        given(settings.getProperty(RegistrationSettings.REMOVE_UNLOGGED_LEAVE_MESSAGE)).willReturn(false);
        given(antiBotService.wasPlayerKicked(name)).willReturn(false);
        String quitMessage = "The player has left the server.";
        PlayerQuitEvent event = new PlayerQuitEvent(player, quitMessage);

        // when
        listener.onPlayerQuit(event);

        // then
        assertThat(event.getQuitMessage(), equalTo(quitMessage));
        verify(antiBotService).wasPlayerKicked(name);
        verify(management).performQuit(player);
    }

    @Test
    public void shouldCancelInventoryClickEvent() {
        // given
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        HumanEntity player = mock(Player.class);
        given(event.getWhoClicked()).willReturn(player);
        given(listenerService.shouldCancelEvent(player)).willReturn(true);

        // when
        listener.onPlayerInventoryClick(event);

        // then
        verify(event).setCancelled(true);
    }

    @Test
    public void shouldAllowInventoryClickEvent() {
        // given
        InventoryClickEvent event = mock(InventoryClickEvent.class);
        HumanEntity player = mock(Player.class);
        given(event.getWhoClicked()).willReturn(player);
        given(listenerService.shouldCancelEvent(player)).willReturn(false);

        // when
        listener.onPlayerInventoryClick(event);

        // then
        verify(event, only()).getWhoClicked();
    }

    @Test
    public void shouldAllowSignChangeEvent() {
        // given
        SignChangeEvent event = mock(SignChangeEvent.class);
        Player player = mock(Player.class);
        given(event.getPlayer()).willReturn(player);
        given(listenerService.shouldCancelEvent(player)).willReturn(false);

        // when
        listener.onSignChange(event);

        // then
        verify(event, only()).getPlayer();
    }

    @Test
    public void shouldCancelSignChangeEvent() {
        // given
        SignChangeEvent event = mock(SignChangeEvent.class);
        Player player = mock(Player.class);
        given(event.getPlayer()).willReturn(player);
        given(listenerService.shouldCancelEvent(player)).willReturn(true);

        // when
        listener.onSignChange(event);

        // then
        verify(event).setCancelled(true);
    }

    @Test
    public void shouldAllowInventoryOpen() {
        // given
        HumanEntity player = mock(Player.class);
        InventoryView transaction = mock(InventoryView.class);
        InventoryOpenEvent event = new InventoryOpenEvent(transaction);
        given(event.getPlayer()).willReturn(player);
        given(listenerService.shouldCancelEvent(player)).willReturn(false);

        // when
        listener.onPlayerInventoryOpen(event);

        // then
        assertThat(event.isCancelled(), equalTo(false));
        verifyNoInteractions(bukkitService);
    }

    @Test
    public void shouldCancelInventoryOpen() {
        // given
        HumanEntity player = mock(Player.class);
        InventoryView transaction = mock(InventoryView.class);
        given(transaction.getTitle()).willReturn("Spawn");
        given(settings.getProperty(RestrictionSettings.UNRESTRICTED_INVENTORIES)).willReturn(Collections.emptySet());
        InventoryOpenEvent event = new InventoryOpenEvent(transaction);
        given(event.getPlayer()).willReturn(player);
        given(listenerService.shouldCancelEvent(player)).willReturn(true);
        setBukkitServiceToScheduleSyncDelayedTaskWithDelay(bukkitService);

        // when
        listener.onPlayerInventoryOpen(event);

        // then
        assertThat(event.isCancelled(), equalTo(true));
        verify(player).closeInventory();
    }

    @Test
    public void shouldNotModifyJoinMessage() {
        // given
        Player player = mock(Player.class);
        String joinMsg = "The player joined";
        PlayerJoinEvent event = new PlayerJoinEvent(player, joinMsg);
        given(settings.getProperty(RegistrationSettings.REMOVE_JOIN_MESSAGE)).willReturn(false);
        given(settings.getProperty(RegistrationSettings.CUSTOM_JOIN_MESSAGE)).willReturn("");
        given(settings.getProperty(RegistrationSettings.DELAY_JOIN_MESSAGE)).willReturn(false);

        // when
        listener.onJoinMessage(event);

        // then
        assertThat(event.getJoinMessage(), equalTo(joinMsg));
        verifyNoInteractions(joinMessageService);
    }

    @Test
    public void shouldRemoveJoinMessage() {
        // given
        Player player = mock(Player.class);
        String joinMsg = "The player joined";
        PlayerJoinEvent event = new PlayerJoinEvent(player, joinMsg);
        given(settings.getProperty(RegistrationSettings.REMOVE_JOIN_MESSAGE)).willReturn(true);

        // when
        listener.onJoinMessage(event);

        // then
        assertThat(event.getJoinMessage(), nullValue());
        verifyNoInteractions(joinMessageService);
    }

    @Test
    public void shouldUseCustomMessage() {
        // given
        Player player = mock(Player.class);
        given(player.getName()).willReturn("doooew");
        given(player.getDisplayName()).willReturn("Displ");
        String joinMsg = "The player joined";
        PlayerJoinEvent event = new PlayerJoinEvent(player, joinMsg);
        given(settings.getProperty(RegistrationSettings.REMOVE_JOIN_MESSAGE)).willReturn(false);
        given(settings.getProperty(RegistrationSettings.CUSTOM_JOIN_MESSAGE))
            .willReturn("Hello {PLAYERNAME} (aka {DISPLAYNAME})");
        given(settings.getProperty(RegistrationSettings.DELAY_JOIN_MESSAGE)).willReturn(false);

        // when
        listener.onJoinMessage(event);

        // then
        assertThat(event.getJoinMessage(), equalTo("Hello doooew (aka Displ)"));
        verifyNoInteractions(joinMessageService);
    }

    @Test
    public void shouldDelayJoinMessage() {
        // given
        Player player = mock(Player.class);
        given(player.getName()).willReturn("thename0");
        given(player.getDisplayName()).willReturn("(not used)");
        String joinMsg = "The player joined";
        PlayerJoinEvent event = new PlayerJoinEvent(player, joinMsg);
        given(settings.getProperty(RegistrationSettings.REMOVE_JOIN_MESSAGE)).willReturn(false);
        given(settings.getProperty(RegistrationSettings.CUSTOM_JOIN_MESSAGE))
            .willReturn("{PLAYERNAME} is joining us");
        given(settings.getProperty(RegistrationSettings.DELAY_JOIN_MESSAGE)).willReturn(true);

        // when
        listener.onJoinMessage(event);

        // then
        assertThat(event.getJoinMessage(), nullValue());
        verify(joinMessageService).putMessage("thename0", "thename0 is joining us");
    }

    @Test
    public void shouldCancelPlayerEditBookEvent() {
        withServiceMock(listenerService)
            .check(listener::onPlayerEditBook, PlayerEditBookEvent.class);
    }

    @Test
    public void shouldCancelPlayerInteractAtEntityEvent() {
        withServiceMock(listenerService)
            .check(listener::onPlayerInteractAtEntity, PlayerInteractAtEntityEvent.class);
    }

    private static Player mockPlayerWithName(String name) {
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        return player;
    }

    /**
     * {@link PlayerCommandPreprocessEvent} gets the list of online players from the player's server.
     * This method creates a Player mock with all necessary mocked behavior.
     *
     * @return Player mock
     */
    @SuppressWarnings("unchecked")
    private static Player playerWithMockedServer() {
        Server server = mock(Server.class);
        given(server.getOnlinePlayers()).willReturn(Collections.EMPTY_LIST);
        Player player = mock(Player.class);
        given(player.getServer()).willReturn(server);
        return player;
    }

    private static PlayerCommandPreprocessEvent mockCommandEvent(String message) {
        PlayerCommandPreprocessEvent commandEvent = mock(PlayerCommandPreprocessEvent.class);
        given(commandEvent.getMessage()).willReturn(message);
        return commandEvent;
    }

    private static AsyncPlayerChatEvent newAsyncChatEvent() {
        Player player = mock(Player.class);
        List<Player> recipients = Arrays.asList(mock(Player.class), mock(Player.class), mock(Player.class));
        return spy(new AsyncPlayerChatEvent(true, player, "Test message", new HashSet<>(recipients)));
    }

    private static void verifyNoModifyingCalls(PlayerMoveEvent event) {
        verify(event, atLeast(0)).getFrom();
        verify(event, atLeast(0)).getTo();
        verifyNoMoreInteractions(event);
    }

    private static void verifyNoModifyingCalls(PlayerLoginEvent event) {
        verify(event, atLeast(0)).getResult();
        verify(event, atLeast(0)).getAddress();
        verifyNoMoreInteractions(event);
    }

    private static void verifyNoModifyingCalls(AsyncPlayerPreLoginEvent event) {
        verify(event, atLeast(0)).getLoginResult();
        verify(event, atLeast(0)).getAddress();
        verify(event, atLeast(0)).getName();
        verifyNoMoreInteractions(event);
    }

    private static InetAddress mockAddrWithIp(String ip) {
        InetAddress addr = mock(InetAddress.class);
        given(addr.getHostAddress()).willReturn(ip);
        return addr;
    }
}
