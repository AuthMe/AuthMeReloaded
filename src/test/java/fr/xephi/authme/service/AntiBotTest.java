package fr.xephi.authme.service;

import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.permission.AdminPermission;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.permission.PlayerStatePermission;
import fr.xephi.authme.service.AntiBotService;
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
 * Test for {@link AntiBotService}.
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
        AntiBotService antiBot = new AntiBotService(settings, messages, permissionsManager, bukkitService);

        // then
        verify(bukkitService, never()).scheduleSyncDelayedTask(any(Runnable.class), anyLong());
        assertThat(antiBot.getAntiBotStatus(), equalTo(AntiBotService.AntiBotStatus.DISABLED));
    }

    @Test
    public void shouldTransitionToListening() {
        // given / when
        AntiBotService antiBot = new AntiBotService(settings, messages, permissionsManager, bukkitService);
        TestHelper.runSyncDelayedTaskWithDelay(bukkitService);

        // then
        assertThat(antiBot.getAntiBotStatus(), equalTo(AntiBotService.AntiBotStatus.LISTENING));
    }

    @Test
    public void shouldSetStatusToActive() {
        // given
        AntiBotService antiBot = createListeningAntiBot();

        // when
        antiBot.overrideAntiBotStatus(true);

        // then
        assertThat(antiBot.getAntiBotStatus(), equalTo(AntiBotService.AntiBotStatus.ACTIVE));
    }

    @Test
    public void shouldSetStatusToListening() {
        // given
        AntiBotService antiBot = createListeningAntiBot();

        // when
        antiBot.overrideAntiBotStatus(false);

        // then
        assertThat(antiBot.getAntiBotStatus(), equalTo(AntiBotService.AntiBotStatus.LISTENING));
    }

    @Test
    public void shouldRemainDisabled() {
        // given
        given(settings.getProperty(ProtectionSettings.ENABLE_ANTIBOT)).willReturn(false);
        AntiBotService antiBot = new AntiBotService(settings, messages, permissionsManager, bukkitService);

        // when
        antiBot.overrideAntiBotStatus(true);

        // then
        assertThat(antiBot.getAntiBotStatus(), equalTo(AntiBotService.AntiBotStatus.DISABLED));
    }

    @Test
    public void shouldActivateAntiBot() {
        // given
        int duration = 300;
        given(settings.getProperty(ProtectionSettings.ANTIBOT_DURATION)).willReturn(duration);
        AntiBotService antiBot = createListeningAntiBot();
        List<Player> onlinePlayers = Arrays.asList(mock(Player.class), mock(Player.class), mock(Player.class));
        given(bukkitService.getOnlinePlayers()).willReturn((List) onlinePlayers);
        given(permissionsManager.hasPermission(onlinePlayers.get(0), AdminPermission.ANTIBOT_MESSAGES)).willReturn(true);
        given(permissionsManager.hasPermission(onlinePlayers.get(1), AdminPermission.ANTIBOT_MESSAGES)).willReturn(false);
        given(permissionsManager.hasPermission(onlinePlayers.get(2), AdminPermission.ANTIBOT_MESSAGES)).willReturn(true);

        // when
        antiBot.startProtection();

        // then
        assertThat(antiBot.getAntiBotStatus(), equalTo(AntiBotService.AntiBotStatus.ACTIVE));
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
        AntiBotService antiBot = createListeningAntiBot();

        // when
        antiBot.startProtection();
        TestHelper.runSyncDelayedTaskWithDelay(bukkitService);

        // then
        assertThat(antiBot.getAntiBotStatus(), equalTo(AntiBotService.AntiBotStatus.LISTENING));
        verify(bukkitService).scheduleSyncDelayedTask(any(Runnable.class), eq((long) 4800));
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        verify(bukkitService, times(2)).broadcastMessage(captor.capture());
        assertThat(captor.getAllValues(), contains("Disabled...", "Placeholder: 4."));
    }

    private AntiBotService createListeningAntiBot() {
        AntiBotService antiBot = new AntiBotService(settings, messages, permissionsManager, bukkitService);
        TestHelper.runSyncDelayedTaskWithDelay(bukkitService);
        // Make BukkitService forget about all interactions up to here
        reset(bukkitService);
        return antiBot;
    }

}
