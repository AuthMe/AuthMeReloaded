package fr.xephi.authme.task;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.data.limbo.LimboCache;
import fr.xephi.authme.data.limbo.LimboPlayer;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.service.BukkitService;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import static org.mockito.BDDMockito.given;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Test for {@link LimboPlayerTaskManager}.
 */
@RunWith(MockitoJUnitRunner.class)
public class LimboPlayerTaskManagerTest {

    @InjectMocks
    private LimboPlayerTaskManager limboPlayerTaskManager;

    @Mock
    private Messages messages;

    @Mock
    private Settings settings;

    @Mock
    private BukkitService bukkitService;

    @Mock
    private LimboCache limboCache;

    @Mock
    private PlayerCache playerCache;

    @BeforeClass
    public static void setupLogger() {
        TestHelper.setupLogger();
    }

    @Test
    public void shouldRegisterMessageTask() {
        // given
        String name = "bobby";
        LimboPlayer limboPlayer = mock(LimboPlayer.class);
        given(limboCache.getPlayerData(name)).willReturn(limboPlayer);
        MessageKey key = MessageKey.REGISTER_EMAIL_MESSAGE;
        given(messages.retrieve(key)).willReturn(new String[]{"Please register!"});
        given(settings.getProperty(RegistrationSettings.MESSAGE_INTERVAL)).willReturn(12);
        given(settings.getProperty(RegistrationSettings.USE_EMAIL_REGISTRATION)).willReturn(true);

        // when
        limboPlayerTaskManager.registerMessageTask(name, false);

        // then
        MessageTask bukkitTask = mock(MessageTask.class);
        verify(limboPlayer).setMessageTask(bukkitTask);
        verify(messages).retrieve(key);
    }

    @Test
    public void shouldNotScheduleTaskForMissingLimboPlayer() {
        // given
        String name = "ghost";
        given(limboCache.getPlayerData(name)).willReturn(null);
        given(settings.getProperty(RegistrationSettings.MESSAGE_INTERVAL)).willReturn(5);

        // when
        limboPlayerTaskManager.registerMessageTask(name, true);

        // then
        verify(limboCache).getPlayerData(name);
        verifyZeroInteractions(bukkitService);
        verifyZeroInteractions(messages);
    }

    @Test
    public void shouldNotScheduleTaskForZeroAsInterval() {
        // given
        String name = "Tester1";
        LimboPlayer limboPlayer = mock(LimboPlayer.class);
        given(limboCache.getPlayerData(name)).willReturn(limboPlayer);
        BukkitTask bukkiTask = mock(BukkitTask.class);
        given(bukkitService.runTask(any(MessageTask.class))).willReturn(bukkiTask);
        given(settings.getProperty(RegistrationSettings.MESSAGE_INTERVAL)).willReturn(0);

        // when
        limboPlayerTaskManager.registerMessageTask(name, true);

        // then
        verifyZeroInteractions(limboPlayer, bukkitService);
    }

    @Test
    public void shouldCancelExistingMessageTask() {
        // given
        LimboPlayer limboPlayer = mock(LimboPlayer.class);
        MessageTask existingMessageTask = mock(MessageTask.class);
        given(limboPlayer.getMessageTask()).willReturn(existingMessageTask);

        String name = "bobby";
        given(limboCache.getPlayerData(name)).willReturn(limboPlayer);
        given(messages.retrieve(MessageKey.REGISTER_EMAIL_MESSAGE))
            .willReturn(new String[]{"Please register", "Use /register"});

        MessageTask bukkiTask = mock(MessageTask.class);
        given(settings.getProperty(RegistrationSettings.MESSAGE_INTERVAL)).willReturn(8);
        given(settings.getProperty(RegistrationSettings.USE_EMAIL_REGISTRATION)).willReturn(true);

        // when
        limboPlayerTaskManager.registerMessageTask(name, false);

        // then
        verify(limboPlayer).setMessageTask(bukkiTask);
        verify(messages).retrieve(MessageKey.REGISTER_EMAIL_MESSAGE);
        verify(existingMessageTask).cancel();
    }

    @Test
    public void shouldRegisterTimeoutTask() {
        // given
        String name = "l33tPlayer";
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        LimboPlayer limboPlayer = mock(LimboPlayer.class);
        given(limboCache.getPlayerData(name)).willReturn(limboPlayer);
        given(settings.getProperty(RestrictionSettings.TIMEOUT)).willReturn(30);
        BukkitTask bukkitTask = mock(BukkitTask.class);
        given(bukkitService.runTaskLater(any(TimeoutTask.class), anyLong())).willReturn(bukkitTask);

        // when
        limboPlayerTaskManager.registerTimeoutTask(player);

        // then
        verify(limboPlayer).setTimeoutTask(bukkitTask);
        verify(bukkitService).runTaskLater(any(TimeoutTask.class), eq(600L)); // 30 * TICKS_PER_SECOND
        verify(messages).retrieveSingle(MessageKey.LOGIN_TIMEOUT_ERROR);
    }

    @Test
    public void shouldNotRegisterTimeoutTaskForMissingLimboPlayer() {
        // given
        String name = "Phantom_";
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        given(limboCache.getPlayerData(name)).willReturn(null);
        given(settings.getProperty(RestrictionSettings.TIMEOUT)).willReturn(27);

        // when
        limboPlayerTaskManager.registerTimeoutTask(player);

        // then
        verifyZeroInteractions(bukkitService, messages);
    }

    @Test
    public void shouldNotRegisterTimeoutTaskForZeroTimeout() {
        // given
        String name = "snail";
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        LimboPlayer limboPlayer = mock(LimboPlayer.class);
        given(limboCache.getPlayerData(name)).willReturn(limboPlayer);
        given(settings.getProperty(RestrictionSettings.TIMEOUT)).willReturn(0);

        // when
        limboPlayerTaskManager.registerTimeoutTask(player);

        // then
        verifyZeroInteractions(limboPlayer, bukkitService);
    }

    @Test
    public void shouldCancelExistingTimeoutTask() {
        // given
        String name = "l33tPlayer";
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        LimboPlayer limboPlayer = mock(LimboPlayer.class);
        BukkitTask existingTask = mock(BukkitTask.class);
        given(limboPlayer.getTimeoutTask()).willReturn(existingTask);
        given(limboCache.getPlayerData(name)).willReturn(limboPlayer);
        given(settings.getProperty(RestrictionSettings.TIMEOUT)).willReturn(18);
        BukkitTask bukkitTask = mock(BukkitTask.class);
        given(bukkitService.runTaskLater(any(TimeoutTask.class), anyLong())).willReturn(bukkitTask);

        // when
        limboPlayerTaskManager.registerTimeoutTask(player);

        // then
        verify(existingTask).cancel();
        verify(limboPlayer).setTimeoutTask(bukkitTask);
        verify(bukkitService).runTaskLater(any(TimeoutTask.class), eq(360L)); // 18 * TICKS_PER_SECOND
        verify(messages).retrieveSingle(MessageKey.LOGIN_TIMEOUT_ERROR);
    }

}
