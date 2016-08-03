package fr.xephi.authme.listener;

import fr.xephi.authme.AntiBot;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.SpawnLoader;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.util.BukkitService;
import fr.xephi.authme.util.TeleportationService;
import fr.xephi.authme.util.ValidationService;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.player.PlayerKickEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerShearEntityEvent;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static fr.xephi.authme.listener.ListenerTestUtils.checkEventIsCanceledForUnauthed;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
    private Messages m;
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

    private static Player mockPlayerWithName(String name) {
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        return player;
    }

}
