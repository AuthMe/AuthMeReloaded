package fr.xephi.authme.data.limbo;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.data.captcha.RegistrationCaptchaManager;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.RegistrationSettings;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import fr.xephi.authme.task.MessageTask;
import fr.xephi.authme.task.TimeoutTask;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitTask;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Collections;

import static fr.xephi.authme.service.BukkitService.TICKS_PER_SECOND;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.Matchers.sameInstance;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

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
    private PlayerCache playerCache;

    @Mock
    private RegistrationCaptchaManager registrationCaptchaManager;

    @BeforeClass
    public static void setupLogger() {
        TestHelper.setupLogger();
    }

    @Test
    public void shouldRegisterMessageTask() {
        // given
        Player player = mock(Player.class);
        LimboPlayer limboPlayer = mock(LimboPlayer.class);
        MessageKey key = MessageKey.REGISTER_MESSAGE;
        given(messages.retrieveSingle(player, key)).willReturn("Please register!");
        int interval = 12;
        given(settings.getProperty(RegistrationSettings.MESSAGE_INTERVAL)).willReturn(interval);

        // when
        limboPlayerTaskManager.registerMessageTask(player, limboPlayer, LimboMessageType.REGISTER);

        // then
        verify(limboPlayer).setMessageTask(any(MessageTask.class));
        verify(messages).retrieveSingle(player, key);
        verify(bukkitService).runTaskTimer(
            any(MessageTask.class), eq(2L * TICKS_PER_SECOND), eq((long) interval * TICKS_PER_SECOND));
    }

    @Test
    public void shouldNotScheduleTaskForZeroAsInterval() {
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
    public void shouldCancelExistingMessageTask() {
        // given
        String name = "rats";
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        LimboPlayer limboPlayer = new LimboPlayer(null, true, Collections.singletonList(new UserGroup("grp")), false, 0.1f, 0.0f);
        MessageTask existingMessageTask = mock(MessageTask.class);
        limboPlayer.setMessageTask(existingMessageTask);
        given(settings.getProperty(RegistrationSettings.MESSAGE_INTERVAL)).willReturn(8);
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
    public void shouldInitializeMessageTaskWithCaptchaMessage() {
        // given
        String name = "race";
        Player player = mock(Player.class);
        given(player.getName()).willReturn(name);
        LimboPlayer limboPlayer = new LimboPlayer(null, true, Collections.singletonList(new UserGroup("grp")), false, 0.1f, 0.0f);
        given(settings.getProperty(RegistrationSettings.MESSAGE_INTERVAL)).willReturn(12);
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
    public void shouldRegisterTimeoutTask() {
        // given
        Player player = mock(Player.class);
        LimboPlayer limboPlayer = mock(LimboPlayer.class);
        given(settings.getProperty(RestrictionSettings.TIMEOUT)).willReturn(30);
        BukkitTask bukkitTask = mock(BukkitTask.class);
        given(bukkitService.runTaskLater(any(TimeoutTask.class), anyLong())).willReturn(bukkitTask);

        // when
        limboPlayerTaskManager.registerTimeoutTask(player, limboPlayer);

        // then
        verify(limboPlayer).setTimeoutTask(bukkitTask);
        verify(bukkitService).runTaskLater(any(TimeoutTask.class), eq(600L)); // 30 * TICKS_PER_SECOND
        verify(messages).retrieveSingle(player, MessageKey.LOGIN_TIMEOUT_ERROR);
    }

    @Test
    public void shouldNotRegisterTimeoutTaskForZeroTimeout() {
        // given
        Player player = mock(Player.class);
        LimboPlayer limboPlayer = mock(LimboPlayer.class);
        given(settings.getProperty(RestrictionSettings.TIMEOUT)).willReturn(0);

        // when
        limboPlayerTaskManager.registerTimeoutTask(player, limboPlayer);

        // then
        verifyNoInteractions(limboPlayer, bukkitService);
    }

    @Test
    public void shouldCancelExistingTimeoutTask() {
        // given
        Player player = mock(Player.class);
        LimboPlayer limboPlayer = new LimboPlayer(null, false, Collections.emptyList(), true, 0.3f, 0.1f);
        BukkitTask existingTask = mock(BukkitTask.class);
        limboPlayer.setTimeoutTask(existingTask);
        given(settings.getProperty(RestrictionSettings.TIMEOUT)).willReturn(18);
        BukkitTask bukkitTask = mock(BukkitTask.class);
        given(bukkitService.runTaskLater(any(TimeoutTask.class), anyLong())).willReturn(bukkitTask);

        // when
        limboPlayerTaskManager.registerTimeoutTask(player, limboPlayer);

        // then
        verify(existingTask).cancel();
        assertThat(limboPlayer.getTimeoutTask(), equalTo(bukkitTask));
        verify(bukkitService).runTaskLater(any(TimeoutTask.class), eq(360L)); // 18 * TICKS_PER_SECOND
        verify(messages).retrieveSingle(player, MessageKey.LOGIN_TIMEOUT_ERROR);
    }

}
