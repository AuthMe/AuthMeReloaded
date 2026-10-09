package fr.xephi.authme.data.limbo;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.data.captcha.RegistrationCaptchaManager;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.CancellableTask;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.task.MessageTask;
import fr.xephi.authme.task.TimeoutTask;
import org.bukkit.entity.Player;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static fr.xephi.authme.service.BukkitService.TICKS_PER_SECOND;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Test for {@link LimboPlayerTaskManager}.
 */
@ExtendWith(MockitoExtension.class)
class LimboPlayerTaskManagerTest {

    @InjectMocks
    private LimboPlayerTaskManager limboPlayerTaskManager;

    @Mock
    private Messages messages;

    @Mock
    private Settings settings;

    @Mock
    private BukkitService bukkitService;

    @Mock
    private PlayerCache playerCache;

    @Mock
    private RegistrationCaptchaManager registrationCaptchaManager;

    @Mock
    private DataSource dataSource;

    @BeforeAll
    static void setupLogger() {
        TestHelper.setupLogger();
    }

    @Test
    void shouldRegisterMessageTask() {
        // given
        Player player = mock(Player.class);
        LimboPlayer limboPlayer = mock(LimboPlayer.class);
        MessageKey key = MessageKey.REGISTER_MESSAGE;
        given(messages.retrieveSingle(player, key)).willReturn("Please register!");
        int interval = 12;
        given(settings.getProperty(RegistrationSettings.MESSAGE_INTERVAL)).willReturn(interval);
        given(settings.getProperty(RegistrationSettings.REGISTER_MESSAGE_DELAY)).willReturn(1);
        given(bukkitService.runTaskLater(eq(player), any(Runnable.class), anyLong()))
            .willReturn(mock(CancellableTask.class));

        // when
        limboPlayerTaskManager.registerMessageTask(player, limboPlayer, LimboMessageType.REGISTER);

        // then
        verify(limboPlayer).setMessageTask(any(MessageTask.class), any(CancellableTask.class));
        verify(messages).retrieveSingle(player, key);
        verify(bukkitService).runTaskLater(eq(player), any(Runnable.class), eq(1L * TICKS_PER_SECOND));
    }

    @Test
    void shouldSuppressDelayedRegistrationMessageWhenPlayerWasRegistered() {
        // given
        String name = "registered_during_delay";
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        given(player.isOnline()).willReturn(true);
        LimboPlayer limboPlayer = mock(LimboPlayer.class);
        given(settings.getProperty(RegistrationSettings.MESSAGE_INTERVAL)).willReturn(5);
        given(settings.getProperty(RegistrationSettings.REGISTER_MESSAGE_DELAY)).willReturn(1);
        given(messages.retrieveSingle(player, MessageKey.REGISTER_MESSAGE)).willReturn("Please register!");
        CancellableTask delayedTask = mock(CancellableTask.class);
        given(bukkitService.runTaskLater(eq(player), any(Runnable.class), eq(1L * TICKS_PER_SECOND)))
            .willReturn(delayedTask);
        CancellableTask lookupTask = mock(CancellableTask.class);
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return lookupTask;
        }).when(bukkitService).runTaskAsynchronously(any(Runnable.class));
        given(dataSource.isAuthAvailable(name)).willReturn(true);
        ArgumentCaptor<Runnable> delayedAction = ArgumentCaptor.forClass(Runnable.class);

        // when
        limboPlayerTaskManager.registerMessageTask(player, limboPlayer, LimboMessageType.REGISTER);
        verify(bukkitService).runTaskLater(eq(player), delayedAction.capture(), eq(1L * TICKS_PER_SECOND));
        delayedAction.getValue().run();

        // then
        verify(dataSource).isAuthAvailable(name);
        verify(player, never()).sendMessage(any(String[].class));
        verify(bukkitService, never()).runTaskTimer(eq(player), any(MessageTask.class), anyLong(), anyLong());
    }

    @Test
    void shouldSendDelayedRegistrationMessageWhenPlayerIsStillUnregistered() {
        // given
        String name = "still_unregistered";
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        given(player.isOnline()).willReturn(true);
        LimboPlayer limboPlayer = mock(LimboPlayer.class);
        int interval = 5;
        given(settings.getProperty(RegistrationSettings.MESSAGE_INTERVAL)).willReturn(interval);
        given(settings.getProperty(RegistrationSettings.REGISTER_MESSAGE_DELAY)).willReturn(1);
        given(messages.retrieveSingle(player, MessageKey.REGISTER_MESSAGE)).willReturn("Please register!");
        given(bukkitService.runTaskLater(eq(player), any(Runnable.class), eq(1L * TICKS_PER_SECOND)))
            .willReturn(mock(CancellableTask.class));
        CancellableTask lookupTask = mock(CancellableTask.class);
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(0)).run();
            return lookupTask;
        }).when(bukkitService).runTaskAsynchronously(any(Runnable.class));
        doAnswer(invocation -> {
            ((Runnable) invocation.getArgument(1)).run();
            return null;
        }).when(bukkitService).scheduleSyncTaskFromOptionallyAsyncTask(eq(player), any(Runnable.class));
        given(dataSource.isAuthAvailable(name)).willReturn(false);
        given(bukkitService.runTaskTimer(eq(player), any(MessageTask.class), anyLong(), anyLong()))
            .willReturn(mock(CancellableTask.class));
        ArgumentCaptor<Runnable> delayedAction = ArgumentCaptor.forClass(Runnable.class);

        // when
        limboPlayerTaskManager.registerMessageTask(player, limboPlayer, LimboMessageType.REGISTER);
        verify(bukkitService).runTaskLater(eq(player), delayedAction.capture(), eq(1L * TICKS_PER_SECOND));
        delayedAction.getValue().run();

        // then
        verify(bukkitService).runTaskTimer(eq(player), any(MessageTask.class),
            eq((long) interval * TICKS_PER_SECOND), eq((long) interval * TICKS_PER_SECOND));
    }

    @Test
    void shouldSuppressDelayedRegistrationMessageWhenPlayerWasForceLoggedIn() {
        // given
        String name = "force_logged_in";
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        given(player.isOnline()).willReturn(true);
        LimboPlayer limboPlayer = mock(LimboPlayer.class);
        given(settings.getProperty(RegistrationSettings.MESSAGE_INTERVAL)).willReturn(5);
        given(settings.getProperty(RegistrationSettings.REGISTER_MESSAGE_DELAY)).willReturn(1);
        given(messages.retrieveSingle(player, MessageKey.REGISTER_MESSAGE)).willReturn("Please register!");
        given(bukkitService.runTaskLater(eq(player), any(Runnable.class), eq(1L * TICKS_PER_SECOND)))
            .willReturn(mock(CancellableTask.class));
        given(playerCache.isAuthenticated(name)).willReturn(true);
        ArgumentCaptor<Runnable> delayedAction = ArgumentCaptor.forClass(Runnable.class);

        // when
        limboPlayerTaskManager.registerMessageTask(player, limboPlayer, LimboMessageType.REGISTER);
        verify(bukkitService).runTaskLater(eq(player), delayedAction.capture(), eq(1L * TICKS_PER_SECOND));
        delayedAction.getValue().run();

        // then
        verifyNoInteractions(dataSource);
        verify(player, never()).sendMessage(any(String[].class));
    }

    @Test
    void shouldNotScheduleTaskForZeroAsInterval() {
        // given
        Player player = mock(Player.class);
        LimboPlayer limboPlayer = mock(LimboPlayer.class);

        given(settings.getProperty(RegistrationSettings.MESSAGE_INTERVAL)).willReturn(0);

        // when
        limboPlayerTaskManager.registerMessageTask(player, limboPlayer, LimboMessageType.LOG_IN);

        // then
        verifyNoInteractions(limboPlayer, bukkitService);
    }

    @Test
    void shouldCancelExistingMessageTask() {
        // given
        String name = "rats";
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        LimboPlayer limboPlayer = new LimboPlayer(null, true, Collections.singletonList(new UserGroup("grp")), false, 0.1f, 0.0f);
        MessageTask existingMessageTask = mock(MessageTask.class);
        limboPlayer.setMessageTask(existingMessageTask);
        given(settings.getProperty(RegistrationSettings.MESSAGE_INTERVAL)).willReturn(8);
        given(settings.getProperty(RegistrationSettings.REGISTER_MESSAGE_DELAY)).willReturn(1);
        given(bukkitService.runTaskLater(eq(player), any(Runnable.class), eq(1L * TICKS_PER_SECOND)))
            .willReturn(mock(CancellableTask.class));
        given(messages.retrieveSingle(player, MessageKey.REGISTER_MESSAGE)).willReturn("Please register!");

        // when
        limboPlayerTaskManager.registerMessageTask(player, limboPlayer, LimboMessageType.REGISTER);

        // then
        assertThat(limboPlayer.getMessageTask(), not(nullValue()));
        assertThat(limboPlayer.getMessageTask(), not(sameInstance(existingMessageTask)));
        verify(registrationCaptchaManager).isCaptchaRequired(name);
        verify(messages).retrieveSingle(player, MessageKey.REGISTER_MESSAGE);
        verify(existingMessageTask).cancel();
    }

    @Test
    void shouldInitializeMessageTaskWithCaptchaMessage() {
        // given
        String name = "race";
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        LimboPlayer limboPlayer = new LimboPlayer(null, true, Collections.singletonList(new UserGroup("grp")), false, 0.1f, 0.0f);
        given(settings.getProperty(RegistrationSettings.MESSAGE_INTERVAL)).willReturn(12);
        given(settings.getProperty(RegistrationSettings.REGISTER_MESSAGE_DELAY)).willReturn(1);
        given(bukkitService.runTaskLater(eq(player), any(Runnable.class), eq(1L * TICKS_PER_SECOND)))
            .willReturn(mock(CancellableTask.class));
        given(registrationCaptchaManager.isCaptchaRequired(name)).willReturn(true);
        String captcha = "M032";
        given(registrationCaptchaManager.getCaptchaCodeOrGenerateNew(name)).willReturn(captcha);
        given(messages.retrieveSingle(player, MessageKey.CAPTCHA_FOR_REGISTRATION_REQUIRED, captcha)).willReturn("Need to use captcha");

        // when
        limboPlayerTaskManager.registerMessageTask(player, limboPlayer, LimboMessageType.REGISTER);

        // then
        assertThat(limboPlayer.getMessageTask(), not(nullValue()));
        verify(messages).retrieveSingle(player, MessageKey.CAPTCHA_FOR_REGISTRATION_REQUIRED, captcha);
    }

    @Test
    void shouldRegisterTimeoutTask() {
        // given
        Player player = mock(Player.class);
        LimboPlayer limboPlayer = mock(LimboPlayer.class);
        given(settings.getProperty(RestrictionSettings.LOGIN_TIMEOUT)).willReturn(30);
        CancellableTask bukkitTask = mock(CancellableTask.class);
        given(bukkitService.runTaskLater(eq(player), any(TimeoutTask.class), anyLong())).willReturn(bukkitTask);

        // when
        limboPlayerTaskManager.registerTimeoutTask(player, limboPlayer, LimboMessageType.LOG_IN);

        // then
        verify(limboPlayer).setTimeoutTask(bukkitTask);
        verify(bukkitService).runTaskLater(eq(player), any(TimeoutTask.class), eq(600L)); // 30 * TICKS_PER_SECOND
        verify(messages).retrieveSingle(player, MessageKey.LOGIN_TIMEOUT_ERROR);
    }

    @Test
    void shouldNotRegisterTimeoutTaskForZeroTimeout() {
        // given
        Player player = mock(Player.class);
        LimboPlayer limboPlayer = mock(LimboPlayer.class);
        given(settings.getProperty(RestrictionSettings.REGISTER_TIMEOUT)).willReturn(60);
        CancellableTask bukkitTask = mock(CancellableTask.class);
        given(bukkitService.runTaskLater(eq(player), any(TimeoutTask.class), anyLong())).willReturn(bukkitTask);

        // when
        limboPlayerTaskManager.registerTimeoutTask(player, limboPlayer, LimboMessageType.REGISTER);

        // then
        verify(limboPlayer).setTimeoutTask(bukkitTask);
        verify(bukkitService).runTaskLater(eq(player), any(TimeoutTask.class), eq(1200L)); // 60 * TICKS_PER_SECOND
        verify(messages).retrieveSingle(player, MessageKey.LOGIN_TIMEOUT_ERROR);
    }

    @Test
    void shouldNotRegisterTimeoutTaskForZeroLoginTimeout() {
        // given
        Player player = mock(Player.class);
        LimboPlayer limboPlayer = mock(LimboPlayer.class);
        given(settings.getProperty(RestrictionSettings.LOGIN_TIMEOUT)).willReturn(0);

        // when
        limboPlayerTaskManager.registerTimeoutTask(player, limboPlayer, LimboMessageType.LOG_IN);

        // then
        verifyNoInteractions(limboPlayer, bukkitService);
    }

    @Test
    void shouldNotRegisterTimeoutTaskForZeroRegisterTimeout() {
        // given
        Player player = mock(Player.class);
        LimboPlayer limboPlayer = mock(LimboPlayer.class);
        given(settings.getProperty(RestrictionSettings.REGISTER_TIMEOUT)).willReturn(0);

        // when
        limboPlayerTaskManager.registerTimeoutTask(player, limboPlayer, LimboMessageType.REGISTER);

        // then
        verifyNoInteractions(limboPlayer, bukkitService);
    }

    @Test
    void shouldCancelExistingTimeoutTask() {
        // given
        Player player = mock(Player.class);
        LimboPlayer limboPlayer = new LimboPlayer(null, false, Collections.emptyList(), true, 0.3f, 0.1f);
        CancellableTask existingTask = mock(CancellableTask.class);
        limboPlayer.setTimeoutTask(existingTask);
        given(settings.getProperty(RestrictionSettings.LOGIN_TIMEOUT)).willReturn(18);
        CancellableTask bukkitTask = mock(CancellableTask.class);
        given(bukkitService.runTaskLater(eq(player), any(TimeoutTask.class), anyLong())).willReturn(bukkitTask);

        // when
        limboPlayerTaskManager.registerTimeoutTask(player, limboPlayer, LimboMessageType.LOG_IN);

        // then
        verify(existingTask).cancel();
        assertThat(limboPlayer.getTimeoutTask(), equalTo(bukkitTask));
        verify(bukkitService).runTaskLater(eq(player), any(TimeoutTask.class), eq(360L)); // 18 * TICKS_PER_SECOND
        verify(messages).retrieveSingle(player, MessageKey.LOGIN_TIMEOUT_ERROR);
    }

}
