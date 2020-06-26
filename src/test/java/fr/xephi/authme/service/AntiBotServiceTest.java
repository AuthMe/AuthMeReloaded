package fr.xephi.authme.service;

import ch.jalu.injector.testing.BeforeInjecting;
import ch.jalu.injector.testing.DelayedInjectionRunner;
import ch.jalu.injector.testing.InjectDelayed;
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
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;

import java.util.Arrays;
import java.util.List;

import static fr.xephi.authme.service.BukkitServiceTestHelper.setBukkitServiceToScheduleSyncDelayedTaskWithDelay;
import static fr.xephi.authme.service.BukkitServiceTestHelper.setBukkitServiceToScheduleSyncTaskFromOptionallyAsyncTask;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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
        given(settings.getProperty(ProtectionSettings.ANTIBOT_INTERVAL)).willReturn(5);
        given(settings.getProperty(ProtectionSettings.ANTIBOT_SENSIBILITY)).willReturn(5);
        given(settings.getProperty(ProtectionSettings.ENABLE_ANTIBOT)).willReturn(true);
        given(settings.getProperty(ProtectionSettings.ANTIBOT_DELAY)).willReturn(8);
        setBukkitServiceToScheduleSyncDelayedTaskWithDelay(bukkitService);
    }

    @Test
    public void shouldStartListenerOnStartup() {
        // given / when / then
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
        verifyNoInteractions(bukkitService);
    }

    @Test
    public void shouldActivateAntibot() {
        // given - listening antibot
        BukkitTask task = mock(BukkitTask.class);
        given(bukkitService.runTaskLater(any(Runnable.class), anyLong())).willReturn(task);

        // when
        antiBotService.overrideAntiBotStatus(true);

        // then
        assertThat(antiBotService.getAntiBotStatus(), equalTo(AntiBotService.AntiBotStatus.ACTIVE));
        // Check that a task is scheduled to disable again
        ArgumentCaptor<Runnable> runnableCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(bukkitService).runTaskLater(runnableCaptor.capture(), anyLong());
        runnableCaptor.getValue().run();
        assertThat(antiBotService.getAntiBotStatus(), equalTo(AntiBotService.AntiBotStatus.LISTENING));
    }

    @Test
    public void shouldNotActivateAntibotForDisabledSetting() {
        // given - disabled antibot
        given(settings.getProperty(ProtectionSettings.ENABLE_ANTIBOT)).willReturn(false);
        AntiBotService antiBotService = new AntiBotService(settings, messages, permissionsManager, bukkitService);

        // when
        antiBotService.overrideAntiBotStatus(true);

        // then
        assertThat(antiBotService.getAntiBotStatus(), equalTo(AntiBotService.AntiBotStatus.DISABLED));
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
        // given / when
        boolean result = antiBotService.shouldKick();

        // then
        assertThat(result, equalTo(false));
    }

    @Test
    public void shouldActivateAntibotAfterThreshold() {
        // given
        int sensitivity = 10;
        given(settings.getProperty(ProtectionSettings.ANTIBOT_SENSIBILITY)).willReturn(sensitivity);
        AntiBotService antiBotService = new AntiBotService(settings, messages, permissionsManager, bukkitService);

        for (int i = 0; i < sensitivity; ++i) {
            antiBotService.shouldKick();
        }
        assertThat(antiBotService.getAntiBotStatus(), equalTo(AntiBotService.AntiBotStatus.LISTENING));

        // when
        antiBotService.shouldKick();

        // then
        assertThat(antiBotService.getAntiBotStatus(), equalTo(AntiBotService.AntiBotStatus.ACTIVE));
    }

    @Test
    public void shouldInformPlayersOnActivation() {
        // given - listening antibot
        List<Player> players = Arrays.asList(mock(Player.class), mock(Player.class));
        given(bukkitService.getOnlinePlayers()).willReturn(players);
        given(permissionsManager.hasPermission(players.get(0), AdminPermission.ANTIBOT_MESSAGES)).willReturn(false);
        given(permissionsManager.hasPermission(players.get(1), AdminPermission.ANTIBOT_MESSAGES)).willReturn(true);
        setBukkitServiceToScheduleSyncTaskFromOptionallyAsyncTask(bukkitService);

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
        given(bukkitService.runTaskLater(any(Runnable.class), anyLong())).willReturn(mock(BukkitTask.class));
        antiBotService.overrideAntiBotStatus(true);

        // when
        antiBotService.reload(settings);

        // then
        assertThat(antiBotService.getAntiBotStatus(), equalTo(AntiBotService.AntiBotStatus.LISTENING));
    }

}
