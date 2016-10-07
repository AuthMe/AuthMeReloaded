package fr.xephi.authme.service;

import ch.jalu.injector.testing.BeforeInjecting;
import ch.jalu.injector.testing.DelayedInjectionRunner;
import ch.jalu.injector.testing.InjectDelayed;
import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.permission.AdminPermission;
import fr.xephi.authme.permission.PermissionsManager;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.ProtectionSettings;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

import static fr.xephi.authme.TestHelper.runSyncDelayedTaskWithDelay;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Test for {@link AntiBotService}.
 */
@RunWith(DelayedInjectionRunner.class)
public class AntiBotServiceTest {

    @InjectDelayed
    private AntiBotService antiBotService;

    @Mock
    private Settings settings;
    @Mock
    private Messages messages;
    @Mock
    private PermissionsManager permissionsManager;
    @Mock
    private BukkitService bukkitService;

    @BeforeInjecting
    public void initSettings() {
        given(settings.getProperty(ProtectionSettings.ANTIBOT_DURATION)).willReturn(10);
        given(settings.getProperty(ProtectionSettings.ANTIBOT_SENSIBILITY)).willReturn(5);
        given(settings.getProperty(ProtectionSettings.ENABLE_ANTIBOT)).willReturn(true);
        given(settings.getProperty(ProtectionSettings.ANTIBOT_DELAY)).willReturn(8);
    }

    @Test
    public void shouldStartListenerOnStartup() {
        // given / when
        runSyncDelayedTaskWithDelay(bukkitService);

        // then
        assertThat(antiBotService.getAntiBotStatus(), equalTo(AntiBotService.AntiBotStatus.LISTENING));
    }

    @Test
    public void shouldNotListenForDisabledSetting() {
        // given
        reset(bukkitService);
        given(settings.getProperty(ProtectionSettings.ENABLE_ANTIBOT)).willReturn(false);

        // when
        AntiBotService antiBotService = new AntiBotService(settings, messages, permissionsManager, bukkitService);

        // then
        assertThat(antiBotService.getAntiBotStatus(), equalTo(AntiBotService.AntiBotStatus.DISABLED));
        verifyZeroInteractions(bukkitService);
    }

    @Test
    public void shouldActivateAntibot() {
        // given - listening antibot
        runSyncDelayedTaskWithDelay(bukkitService);

        // when
        antiBotService.overrideAntiBotStatus(true);

        // then
        assertThat(antiBotService.getAntiBotStatus(), equalTo(AntiBotService.AntiBotStatus.ACTIVE));
        // Check that a task is scheduled to disable again
        runSyncDelayedTaskWithDelay(bukkitService);
        assertThat(antiBotService.getAntiBotStatus(), equalTo(AntiBotService.AntiBotStatus.LISTENING));
    }

    @Test
    public void shouldNotActivateAntibotForDisabledSetting() {
        // given - disabled antibot
        reset(bukkitService);
        assertThat(antiBotService.getAntiBotStatus(), equalTo(AntiBotService.AntiBotStatus.DISABLED));
        given(settings.getProperty(ProtectionSettings.ENABLE_ANTIBOT)).willReturn(false);

        // when
        antiBotService.overrideAntiBotStatus(true);

        // then
        assertThat(antiBotService.getAntiBotStatus(), equalTo(AntiBotService.AntiBotStatus.DISABLED));
        verifyZeroInteractions(bukkitService);
    }

    @Test
    public void shouldKeepTrackOfKickedPlayers() {
        // given
        String name = "eratic";
        antiBotService.addPlayerKick(name);

        // when
        boolean result1 = antiBotService.wasPlayerKicked(name);
        boolean result2 = antiBotService.wasPlayerKicked("other");

        // then
        assertThat(result1, equalTo(true));
        assertThat(result2, equalTo(false));
    }

    @Test
    public void shouldAcceptPlayerToJoin() {
        // given - listening antibot
        runSyncDelayedTaskWithDelay(bukkitService);

        // when
        boolean result = antiBotService.shouldKick(false);

        // then
        assertThat(result, equalTo(false));
    }

    @Test
    public void shouldRejectPlayerWithoutAuth() {
        // given - active antibot
        runSyncDelayedTaskWithDelay(bukkitService);
        antiBotService.overrideAntiBotStatus(true);

        // when
        boolean kickWithoutAuth = antiBotService.shouldKick(false);
        boolean kickWithAuth = antiBotService.shouldKick(true);

        // then
        assertThat(kickWithoutAuth, equalTo(true));
        assertThat(kickWithAuth, equalTo(false));
    }

    @Test
    public void shouldIncreaseCountAndDecreaseAfterDelay() {
        // given - listening antibot
        runSyncDelayedTaskWithDelay(bukkitService);
        reset(bukkitService);
        assertThat(getAntiBotCount(antiBotService), equalTo(0));

        // when
        antiBotService.handlePlayerJoin();

        // then
        assertThat(getAntiBotCount(antiBotService), equalTo(1));
        runSyncDelayedTaskWithDelay(bukkitService);
        assertThat(getAntiBotCount(antiBotService), equalTo(0));
    }

    @Test
    public void shouldActivateAntibotAfterThreshold() {
        // given
        int sensitivity = 10;
        given(settings.getProperty(ProtectionSettings.ANTIBOT_SENSIBILITY)).willReturn(sensitivity);
        reset(bukkitService);
        AntiBotService antiBotService = new AntiBotService(settings, messages, permissionsManager, bukkitService);
        runSyncDelayedTaskWithDelay(bukkitService);

        for (int i = 0; i < sensitivity; ++i) {
            antiBotService.handlePlayerJoin();
        }
        assertThat(antiBotService.getAntiBotStatus(), equalTo(AntiBotService.AntiBotStatus.LISTENING));

        // when
        antiBotService.handlePlayerJoin();

        // then
        assertThat(antiBotService.getAntiBotStatus(), equalTo(AntiBotService.AntiBotStatus.ACTIVE));
    }

    @Test
    @SuppressWarnings("unchecked")
    public void shouldInformPlayersOnActivation() {
        // given - listening antibot
        runSyncDelayedTaskWithDelay(bukkitService);
        List<Player> players = Arrays.asList(mock(Player.class), mock(Player.class));
        given(bukkitService.getOnlinePlayers()).willReturn((List) players);
        given(permissionsManager.hasPermission(players.get(0), AdminPermission.ANTIBOT_MESSAGES)).willReturn(false);
        given(permissionsManager.hasPermission(players.get(1), AdminPermission.ANTIBOT_MESSAGES)).willReturn(true);

        // when
        antiBotService.overrideAntiBotStatus(true);

        // then
        verify(permissionsManager).hasPermission(players.get(0), AdminPermission.ANTIBOT_MESSAGES);
        verify(permissionsManager).hasPermission(players.get(1), AdminPermission.ANTIBOT_MESSAGES);
        verify(messages, only()).send(players.get(1), MessageKey.ANTIBOT_AUTO_ENABLED_MESSAGE);
    }

    @Test
    public void shouldImmediatelyStartAfterFirstStartup() {
        // given - listening antibot
        runSyncDelayedTaskWithDelay(bukkitService);
        given(bukkitService.runTaskLater(any(Runnable.class), anyLong())).willReturn(mock(BukkitTask.class));
        antiBotService.overrideAntiBotStatus(true);

        // when
        antiBotService.reload(settings);

        // then
        assertThat(antiBotService.getAntiBotStatus(), equalTo(AntiBotService.AntiBotStatus.LISTENING));
    }

    private static int getAntiBotCount(AntiBotService antiBotService) {
        return ReflectionTestUtils.getFieldValue(AntiBotService.class, antiBotService, "antibotPlayers");
    }
}
