package fr.xephi.authme.task;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.cache.limbo.LimboCache;
import fr.xephi.authme.cache.limbo.PlayerData;
import fr.xephi.authme.output.MessageKey;
import fr.xephi.authme.output.Messages;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.util.BukkitService;
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
 * Test for {@link PlayerDataTaskManager}.
 */
@RunWith(MockitoJUnitRunner.class)
public class PlayerDataTaskManagerTest {

    @InjectMocks
    private PlayerDataTaskManager playerDataTaskManager;

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
        PlayerData playerData = mock(PlayerData.class);
        given(limboCache.getPlayerData(name)).willReturn(playerData);
        MessageKey key = MessageKey.REGISTER_EMAIL_MESSAGE;
        given(messages.retrieve(key)).willReturn(new String[]{"Please register!"});
        BukkitTask bukkiTask = mock(BukkitTask.class);
        given(bukkitService.runTask(any(MessageTask.class))).willReturn(bukkiTask);
        given(settings.getProperty(RegistrationSettings.MESSAGE_INTERVAL)).willReturn(12);
        given(settings.getProperty(RegistrationSettings.USE_EMAIL_REGISTRATION)).willReturn(true);

        // when
        playerDataTaskManager.registerMessageTask(name, false);

        // then
        verify(playerData).setMessageTask(bukkiTask);
        verify(messages).retrieve(key);
    }

    @Test
    public void shouldNotScheduleTaskForMissingLimboPlayer() {
        // given
        String name = "ghost";
        given(limboCache.getPlayerData(name)).willReturn(null);
        given(settings.getProperty(RegistrationSettings.MESSAGE_INTERVAL)).willReturn(5);

        // when
        playerDataTaskManager.registerMessageTask(name, true);

        // then
        verify(limboCache).getPlayerData(name);
        verifyZeroInteractions(bukkitService);
        verifyZeroInteractions(messages);
    }

    @Test
    public void shouldNotScheduleTaskForZeroAsInterval() {
        // given
        String name = "Tester1";
        PlayerData playerData = mock(PlayerData.class);
        given(limboCache.getPlayerData(name)).willReturn(playerData);
        BukkitTask bukkiTask = mock(BukkitTask.class);
        given(bukkitService.runTask(any(MessageTask.class))).willReturn(bukkiTask);
        given(settings.getProperty(RegistrationSettings.MESSAGE_INTERVAL)).willReturn(0);

        // when
        playerDataTaskManager.registerMessageTask(name, true);

        // then
        verifyZeroInteractions(playerData, bukkitService);
    }

    @Test
    public void shouldCancelExistingMessageTask() {
        // given
        PlayerData playerData = mock(PlayerData.class);
        BukkitTask existingMessageTask = mock(BukkitTask.class);
        given(playerData.getMessageTask()).willReturn(existingMessageTask);

        String name = "bobby";
        given(limboCache.getPlayerData(name)).willReturn(playerData);
        given(messages.retrieve(MessageKey.REGISTER_EMAIL_MESSAGE))
            .willReturn(new String[]{"Please register", "Use /register"});

        BukkitTask bukkiTask = mock(BukkitTask.class);
        given(bukkitService.runTask(any(MessageTask.class))).willReturn(bukkiTask);
        given(settings.getProperty(RegistrationSettings.MESSAGE_INTERVAL)).willReturn(8);
        given(settings.getProperty(RegistrationSettings.USE_EMAIL_REGISTRATION)).willReturn(true);

        // when
        playerDataTaskManager.registerMessageTask(name, false);

        // then
        verify(playerData).setMessageTask(bukkiTask);
        verify(messages).retrieve(MessageKey.REGISTER_EMAIL_MESSAGE);
        verify(existingMessageTask).cancel();
    }

    @Test
    public void shouldRegisterTimeoutTask() {
        // given
        String name = "l33tPlayer";
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        PlayerData playerData = mock(PlayerData.class);
        given(limboCache.getPlayerData(name)).willReturn(playerData);
        given(settings.getProperty(RestrictionSettings.TIMEOUT)).willReturn(30);
        BukkitTask bukkitTask = mock(BukkitTask.class);
        given(bukkitService.runTaskLater(any(TimeoutTask.class), anyLong())).willReturn(bukkitTask);

        // when
        playerDataTaskManager.registerTimeoutTask(player);

        // then
        verify(playerData).setTimeoutTask(bukkitTask);
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
        playerDataTaskManager.registerTimeoutTask(player);

        // then
        verifyZeroInteractions(bukkitService, messages);
    }

    @Test
    public void shouldNotRegisterTimeoutTaskForZeroTimeout() {
        // given
        String name = "snail";
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        PlayerData playerData = mock(PlayerData.class);
        given(limboCache.getPlayerData(name)).willReturn(playerData);
        given(settings.getProperty(RestrictionSettings.TIMEOUT)).willReturn(0);

        // when
        playerDataTaskManager.registerTimeoutTask(player);

        // then
        verifyZeroInteractions(playerData, bukkitService);
    }

    @Test
    public void shouldCancelExistingTimeoutTask() {
        // given
        String name = "l33tPlayer";
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        PlayerData playerData = mock(PlayerData.class);
        BukkitTask existingTask = mock(BukkitTask.class);
        given(playerData.getTimeoutTask()).willReturn(existingTask);
        given(limboCache.getPlayerData(name)).willReturn(playerData);
        given(settings.getProperty(RestrictionSettings.TIMEOUT)).willReturn(18);
        BukkitTask bukkitTask = mock(BukkitTask.class);
        given(bukkitService.runTaskLater(any(TimeoutTask.class), anyLong())).willReturn(bukkitTask);

        // when
        playerDataTaskManager.registerTimeoutTask(player);

        // then
        verify(existingTask).cancel();
        verify(playerData).setTimeoutTask(bukkitTask);
        verify(bukkitService).runTaskLater(any(TimeoutTask.class), eq(360L)); // 18 * TICKS_PER_SECOND
        verify(messages).retrieveSingle(MessageKey.LOGIN_TIMEOUT_ERROR);
    }

}
