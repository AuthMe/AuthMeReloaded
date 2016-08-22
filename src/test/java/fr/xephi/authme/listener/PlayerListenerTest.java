package fr.xephi.authme.listener;

import fr.xephi.authme.AntiBot;
import fr.xephi.authme.cache.auth.PlayerAuth;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.SpawnLoader;
import fr.xephi.authme.settings.properties.HooksSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.util.BukkitService;
import fr.xephi.authme.util.TeleportationService;
import fr.xephi.authme.util.ValidationService;
import org.bukkit.Location;
import org.bukkit.Server;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;

import static fr.xephi.authme.listener.ListenerTestUtils.checkEventIsCanceledForUnauthed;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.verifyZeroInteractions;

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
    private AntiBot antiBot;
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
        verifyZeroInteractions(player, management, antiBot);
    }

    @Test
    public void shouldNotCancelKick() {
        // given
        given(settings.getProperty(RestrictionSettings.FORCE_SINGLE_SESSION)).willReturn(false);
        String name = "Bobby";
        Player player = mockPlayerWithName(name);
        PlayerKickEvent event = new PlayerKickEvent(player, "You logged in from another location", "");
        given(antiBot.wasPlayerKicked(name)).willReturn(false);

        // when
        listener.onPlayerKick(event);

        // then
        assertThat(event.isCancelled(), equalTo(false));
        verify(antiBot).wasPlayerKicked(name);
        verify(management).performQuit(player);
    }

    @Test
    public void shouldNotCancelOrdinaryKick() {
        // given
        given(settings.getProperty(RestrictionSettings.FORCE_SINGLE_SESSION)).willReturn(true);
        String name = "Bobby";
        Player player = mockPlayerWithName(name);
        PlayerKickEvent event = new PlayerKickEvent(player, "No longer desired here!", "");
        given(antiBot.wasPlayerKicked(name)).willReturn(true);

        // when
        listener.onPlayerKick(event);

        // then
        assertThat(event.isCancelled(), equalTo(false));
        verify(antiBot).wasPlayerKicked(name);
        verifyZeroInteractions(management);
    }

    @Test
    public void shouldHandleSimpleCancelableEvents() {
        checkEventIsCanceledForUnauthed(listener, listenerService, PlayerShearEntityEvent.class);
        checkEventIsCanceledForUnauthed(listener, listenerService, PlayerFishEvent.class);
        checkEventIsCanceledForUnauthed(listener, listenerService, PlayerBedEnterEvent.class);
        checkEventIsCanceledForUnauthed(listener, listenerService, PlayerDropItemEvent.class);
        checkEventIsCanceledForUnauthed(listener, listenerService, EntityDamageByEntityEvent.class);
        checkEventIsCanceledForUnauthed(listener, listenerService, PlayerItemConsumeEvent.class);
        checkEventIsCanceledForUnauthed(listener, listenerService, PlayerInteractEvent.class);
        checkEventIsCanceledForUnauthed(listener, listenerService, PlayerPickupItemEvent.class);
        checkEventIsCanceledForUnauthed(listener, listenerService, PlayerInteractEntityEvent.class);
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
        verifyZeroInteractions(listenerService, messages);
    }

    @Test
    public void shouldNotStopAllowedCommand() {
        // given
        given(settings.getProperty(HooksSettings.USE_ESSENTIALS_MOTD)).willReturn(true);
        given(settings.getProperty(RestrictionSettings.ALLOW_COMMANDS))
            .willReturn(Arrays.asList("/plugins", "/mail", "/msg"));
        PlayerCommandPreprocessEvent event = mockCommandEvent("/Mail send test Test");

        // when
        listener.onPlayerCommandPreprocess(event);

        // then
        verify(event, only()).getMessage();
        verifyZeroInteractions(listenerService, messages);
    }

    @Test
    public void shouldNotCancelEventForAuthenticatedPlayer() {
        // given
        given(settings.getProperty(HooksSettings.USE_ESSENTIALS_MOTD)).willReturn(false);
        given(settings.getProperty(RestrictionSettings.ALLOW_COMMANDS)).willReturn(Collections.<String>emptyList());
        Player player = playerWithMockedServer();
        // PlayerCommandPreprocessEvent#getPlayer is final, so create a spy instead of a mock
        PlayerCommandPreprocessEvent event = spy(new PlayerCommandPreprocessEvent(player, "/hub"));
        given(listenerService.shouldCancelEvent(player)).willReturn(false);

        // when
        listener.onPlayerCommandPreprocess(event);

        // then
        verify(event).getMessage();
        verifyNoMoreInteractions(event);
        verify(listenerService).shouldCancelEvent(player);
        verifyZeroInteractions(messages);
    }

    @Test
    public void shouldCancelCommandEvent() {
        // given
        given(settings.getProperty(HooksSettings.USE_ESSENTIALS_MOTD)).willReturn(false);
        given(settings.getProperty(RestrictionSettings.ALLOW_COMMANDS)).willReturn(Arrays.asList("/spawn", "/help"));
        Player player = playerWithMockedServer();
        PlayerCommandPreprocessEvent event = spy(new PlayerCommandPreprocessEvent(player, "/hub"));
        given(listenerService.shouldCancelEvent(player)).willReturn(true);

        // when
        listener.onPlayerCommandPreprocess(event);

        // then
        verify(listenerService).shouldCancelEvent(player);
        verify(event).setCancelled(true);
        verify(messages).send(player, MessageKey.DENIED_COMMAND);
    }

    @Test
    public void shouldAllowChat() {
        // given
        given(settings.getProperty(RestrictionSettings.ALLOW_CHAT)).willReturn(true);
        AsyncPlayerChatEvent event = mock(AsyncPlayerChatEvent.class);

        // when
        listener.onPlayerChat(event);

        // then
        verifyZeroInteractions(event, listenerService, messages);
    }

    @Test
    public void shouldCancelChatForUnauthedPlayer() {
        // given
        given(settings.getProperty(RestrictionSettings.ALLOW_CHAT)).willReturn(false);
        AsyncPlayerChatEvent event = newAsyncChatEvent();
        given(listenerService.shouldCancelEvent(event.getPlayer())).willReturn(true);

        // when
        listener.onPlayerChat(event);

        // then
        verify(listenerService).shouldCancelEvent(event.getPlayer());
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
        verifyZeroInteractions(event, messages);
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
        assertThat(event.getRecipients(), contains(recipients.get(1), recipients.get(2)));
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
        verifyZeroInteractions(event);
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
        given(settings.getProperty(RestrictionSettings.REMOVE_SPEED)).willReturn(false);

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
    public void shouldNotInterfereWithUnrestrictedUser() {
        // given
        String name = "Player01";
        Player player = mockPlayerWithName(name);
        PlayerLoginEvent event = spy(new PlayerLoginEvent(player, "", null));
        given(validationService.isUnrestricted(name)).willReturn(true);

        // when
        listener.onPlayerLogin(event);

        // then
        verify(validationService).isUnrestricted(name);
        verifyNoModifyingCalls(event);
        verifyZeroInteractions(onJoinVerifier);
    }

    @Test
    public void shouldStopHandlingForFullServer() {
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
        verify(onJoinVerifier, only()).refusePlayerForFullServer(event);
        verifyNoModifyingCalls(event);
    }

    @Test
    public void shouldStopHandlingEventForBadResult() {
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
        verify(onJoinVerifier, only()).refusePlayerForFullServer(event);
        verifyNoModifyingCalls(event);
    }

    @Test
    public void shouldPerformAllJoinVerificationsSuccessfully() throws FailedVerificationException {
        // given
        String name = "someone";
        Player player = mockPlayerWithName(name);
        String ip = "12.34.56.78";
        PlayerLoginEvent event = spy(new PlayerLoginEvent(player, "", mockAddrWithIp(ip)));
        given(validationService.isUnrestricted(name)).willReturn(false);
        given(onJoinVerifier.refusePlayerForFullServer(event)).willReturn(false);
        PlayerAuth auth = PlayerAuth.builder().name(name).build();
        given(dataSource.getAuth(name)).willReturn(auth);

        // when
        listener.onPlayerLogin(event);

        // then
        verify(validationService).isUnrestricted(name);
        verify(onJoinVerifier).refusePlayerForFullServer(event);
        verify(onJoinVerifier).checkSingleSession(name);
        verify(onJoinVerifier).checkIsValidName(name);
        verify(onJoinVerifier).checkAntibot(name, true);
        verify(onJoinVerifier).checkKickNonRegistered(true);
        verify(onJoinVerifier).checkNameCasing(player, auth);
        verify(onJoinVerifier).checkPlayerCountry(true, ip);
        verify(antiBot).handlePlayerJoin(player);
        verify(teleportationService).teleportOnJoin(player);
        verifyNoModifyingCalls(event);
    }

    @Test
    public void shouldAbortPlayerJoinForInvalidName() throws FailedVerificationException {
        // given
        String name = "inval!dName";
        Player player = mockPlayerWithName(name);
        PlayerLoginEvent event = spy(new PlayerLoginEvent(player, "", null));
        given(validationService.isUnrestricted(name)).willReturn(false);
        given(onJoinVerifier.refusePlayerForFullServer(event)).willReturn(false);
        FailedVerificationException exception = new FailedVerificationException(
            MessageKey.INVALID_NAME_CHARACTERS, "[a-z]");
        doThrow(exception).when(onJoinVerifier).checkIsValidName(name);
        String message = "Invalid characters!";
        given(messages.retrieveSingle(exception.getReason(), exception.getArgs())).willReturn(message);

        // when
        listener.onPlayerLogin(event);

        // then
        verify(validationService).isUnrestricted(name);
        verify(onJoinVerifier).refusePlayerForFullServer(event);
        verify(onJoinVerifier).checkSingleSession(name);
        verify(onJoinVerifier).checkIsValidName(name);
        // Check that we don't talk with the data source before performing checks that don't require it
        verifyZeroInteractions(dataSource);
        verify(event).setKickMessage(message);
        verify(event).setResult(PlayerLoginEvent.Result.KICK_OTHER);
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

    private static InetAddress mockAddrWithIp(String ip) {
        InetAddress addr = mock(InetAddress.class);
        given(addr.getHostAddress()).willReturn(ip);
        return addr;
    }

}
