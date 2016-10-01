package fr.xephi.authme;

import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.permission.AdminPermission;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PlayerStatePermission;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.ProtectionSettings;
import fr.xephi.authme.util.BukkitService;
import org.bukkit.entity.Player;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;

import static fr.xephi.authme.util.BukkitService.TICKS_PER_MINUTE;
import static fr.xephi.authme.util.BukkitService.TICKS_PER_SECOND;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link AntiBot}.
 */
@RunWith(MockitoJUnitRunner.class)
public class AntiBotTest {

    @Mock
    private Settings settings;
    @Mock
    private Messages messages;
    @Mock
    private PermissionsManager permissionsManager;
    @Mock
    private BukkitService bukkitService;

    @Before
    public void setDefaultSettingValues() {
        given(settings.getProperty(ProtectionSettings.ENABLE_ANTIBOT)).willReturn(true);
    }

    @Test
    public void shouldKeepAntiBotDisabled() {
        // given / when
        given(settings.getProperty(ProtectionSettings.ENABLE_ANTIBOT)).willReturn(false);
        AntiBot antiBot = new AntiBot(settings, messages, permissionsManager, bukkitService);

        // then
        verify(bukkitService, never()).scheduleSyncDelayedTask(any(Runnable.class), anyLong());
        assertThat(antiBot.getAntiBotStatus(), equalTo(AntiBot.AntiBotStatus.DISABLED));
    }

    @Test
    public void shouldTransitionToListening() {
        // given / when
        AntiBot antiBot = new AntiBot(settings, messages, permissionsManager, bukkitService);
        TestHelper.runSyncDelayedTaskWithDelay(bukkitService);

        // then
        assertThat(antiBot.getAntiBotStatus(), equalTo(AntiBot.AntiBotStatus.LISTENING));
    }

    @Test
    public void shouldSetStatusToActive() {
        // given
        AntiBot antiBot = createListeningAntiBot();

        // when
        antiBot.overrideAntiBotStatus(true);

        // then
        assertThat(antiBot.getAntiBotStatus(), equalTo(AntiBot.AntiBotStatus.ACTIVE));
    }

    @Test
    public void shouldSetStatusToListening() {
        // given
        AntiBot antiBot = createListeningAntiBot();

        // when
        antiBot.overrideAntiBotStatus(false);

        // then
        assertThat(antiBot.getAntiBotStatus(), equalTo(AntiBot.AntiBotStatus.LISTENING));
    }

    @Test
    public void shouldRemainDisabled() {
        // given
        given(settings.getProperty(ProtectionSettings.ENABLE_ANTIBOT)).willReturn(false);
        AntiBot antiBot = new AntiBot(settings, messages, permissionsManager, bukkitService);

        // when
        antiBot.overrideAntiBotStatus(true);

        // then
        assertThat(antiBot.getAntiBotStatus(), equalTo(AntiBot.AntiBotStatus.DISABLED));
    }

    @Test
    public void shouldActivateAntiBot() {
        // given
        int duration = 300;
        given(settings.getProperty(ProtectionSettings.ANTIBOT_DURATION)).willReturn(duration);
        AntiBot antiBot = createListeningAntiBot();
        List<Player> onlinePlayers = Arrays.asList(mock(Player.class), mock(Player.class), mock(Player.class));
        given(bukkitService.getOnlinePlayers()).willReturn((List) onlinePlayers);
        given(permissionsManager.hasPermission(onlinePlayers.get(0), AdminPermission.ANTIBOT_MESSAGES)).willReturn(true);
        given(permissionsManager.hasPermission(onlinePlayers.get(1), AdminPermission.ANTIBOT_MESSAGES)).willReturn(false);
        given(permissionsManager.hasPermission(onlinePlayers.get(2), AdminPermission.ANTIBOT_MESSAGES)).willReturn(true);

        // when
        antiBot.activateAntiBot();

        // then
        assertThat(antiBot.getAntiBotStatus(), equalTo(AntiBot.AntiBotStatus.ACTIVE));
        verify(bukkitService).getOnlinePlayers();
        verify(permissionsManager, times(3)).hasPermission(any(Player.class), eq(AdminPermission.ANTIBOT_MESSAGES));
        verify(messages).send(onlinePlayers.get(0), MessageKey.ANTIBOT_AUTO_ENABLED_MESSAGE);
        verify(messages, never()).send(onlinePlayers.get(1), MessageKey.ANTIBOT_AUTO_ENABLED_MESSAGE);
        verify(messages).send(onlinePlayers.get(2), MessageKey.ANTIBOT_AUTO_ENABLED_MESSAGE);
        long expectedTicks = duration * TICKS_PER_MINUTE;
        verify(bukkitService).scheduleSyncDelayedTask(any(Runnable.class), eq(expectedTicks));
    }

    @Test
    public void shouldDisableAntiBotAfterSetDuration() {
        // given
        given(messages.retrieve(MessageKey.ANTIBOT_AUTO_ENABLED_MESSAGE)).willReturn(new String[0]);
        given(messages.retrieve(MessageKey.ANTIBOT_AUTO_DISABLED_MESSAGE))
            .willReturn(new String[]{"Disabled...", "Placeholder: %m."});
        given(settings.getProperty(ProtectionSettings.ANTIBOT_DURATION)).willReturn(4);
        AntiBot antiBot = createListeningAntiBot();

        // when
        antiBot.activateAntiBot();
        TestHelper.runSyncDelayedTaskWithDelay(bukkitService);

        // then
        assertThat(antiBot.getAntiBotStatus(), equalTo(AntiBot.AntiBotStatus.LISTENING));
        verify(bukkitService).scheduleSyncDelayedTask(any(Runnable.class), eq((long) 4800));
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(bukkitService, times(2)).broadcastMessage(captor.capture());
        assertThat(captor.getAllValues(), contains("Disabled...", "Placeholder: 4."));
    }

    @Test
    public void shouldCheckPlayerAndRemoveHimLater() {
        // given
        Player player = mock(Player.class);
        given(player.getName()).willReturn("Plaer");
        given(permissionsManager.hasPermission(player, PlayerStatePermission.BYPASS_ANTIBOT)).willReturn(false);
        given(settings.getProperty(ProtectionSettings.ANTIBOT_SENSIBILITY)).willReturn(10);
        AntiBot antiBot = createListeningAntiBot();

        // when
        antiBot.handlePlayerJoin(player);

        // then
        List<String> playerList = ReflectionTestUtils
            .getFieldValue(AntiBot.class, antiBot, "antibotPlayers");
        assertThat(playerList, hasSize(1));
        verify(bukkitService).scheduleSyncDelayedTask(any(Runnable.class), eq((long) 15 * TICKS_PER_SECOND));

        // Follow-up: Check that player will be removed from list again by running the Runnable
        // given (2)
        // Add another player to the list
        playerList.add("other_player");

        // when (2)
        TestHelper.runSyncDelayedTaskWithDelay(bukkitService);

        // then (2)
        assertThat(playerList, contains("other_player"));
    }

    @Test
    public void shouldNotUpdateListForPlayerWithByPassPermission() {
        // given
        Player player = mock(Player.class);
        given(permissionsManager.hasPermission(player, PlayerStatePermission.BYPASS_ANTIBOT)).willReturn(true);
        given(settings.getProperty(ProtectionSettings.ANTIBOT_SENSIBILITY)).willReturn(3);
        AntiBot antiBot = createListeningAntiBot();

        // when
        antiBot.handlePlayerJoin(player);

        // then
        List<?> playerList = ReflectionTestUtils.getFieldValue(AntiBot.class, antiBot, "antibotPlayers");
        assertThat(playerList, empty());
        verify(bukkitService, never()).scheduleSyncDelayedTask(any(Runnable.class), anyLong());
    }

    private AntiBot createListeningAntiBot() {
        AntiBot antiBot = new AntiBot(settings, messages, permissionsManager, bukkitService);
        TestHelper.runSyncDelayedTaskWithDelay(bukkitService);
        // Make BukkitService forget about all interactions up to here
        reset(bukkitService);
        return antiBot;
    }

}
