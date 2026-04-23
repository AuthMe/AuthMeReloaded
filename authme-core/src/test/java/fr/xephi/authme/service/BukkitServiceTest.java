package fr.xephi.authme.service;

import org.mockito.quality.Strictness;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.events.FailedLoginEvent;
import fr.xephi.authme.platform.SchedulingAdapter;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.PluginSettings;
import org.bukkit.Bukkit;
import org.bukkit.Server;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitRunnable;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitTask;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.Mockito;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.only;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

/**
 * Test for {@link BukkitService}.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.WARN)
public class BukkitServiceTest {

    private BukkitService bukkitService;

    @Mock
    private AuthMe authMe;
    @Mock
    private Settings settings;
    @Mock
    private Server server;
    @Mock
    private BukkitScheduler scheduler;
    @Mock
    private PluginManager pluginManager;
    @Mock
    private SchedulingAdapter schedulingAdapter;

    @BeforeEach
    public void constructBukkitService() {
        ReflectionTestUtils.setField(Bukkit.class, null, "server", server);
        lenient().when(server.getScheduler()).thenReturn(scheduler);
        lenient().when(server.getPluginManager()).thenReturn(pluginManager);
        lenient().when(settings.getProperty(PluginSettings.USE_ASYNC_TASKS)).thenReturn(true);
        bukkitService = new BukkitService(authMe, settings, schedulingAdapter);
    }

    @Test
    public void shouldDispatchCommand() {
        // given
        CommandSender sender = mock(CommandSender.class);
        String command = "help test abc";

        // when
        bukkitService.dispatchCommand(sender, command);

        // then
        verify(server).dispatchCommand(sender, command);
    }

    @Test
    public void shouldDispatchConsoleCommand() {
        // given
        ConsoleCommandSender consoleSender = mock(ConsoleCommandSender.class);
        given(server.getConsoleSender()).willReturn(consoleSender);
        String command = "my command";

        // when
        bukkitService.dispatchConsoleCommand(command);

        // then
        verify(server).dispatchCommand(consoleSender, command);
    }

    @Test
    public void shouldScheduleSyncDelayedTask() {
        // given
        Runnable task = () -> {/* noop */};
        given(scheduler.scheduleSyncDelayedTask(authMe, task)).willReturn(123);

        // when
        int taskId = bukkitService.scheduleSyncDelayedTask(task);

        // then
        verify(scheduler, only()).scheduleSyncDelayedTask(authMe, task);
        assertThat(taskId, equalTo(123));
    }

    @Test
    public void shouldScheduleSyncDelayedTaskWithDelay() {
        // given
        Runnable task = () -> {/* noop */};
        int delay = 3;
        given(scheduler.scheduleSyncDelayedTask(authMe, task, delay)).willReturn(44);

        // when
        int taskId = bukkitService.scheduleSyncDelayedTask(task, delay);

        // then
        verify(scheduler, only()).scheduleSyncDelayedTask(authMe, task, delay);
        assertThat(taskId, equalTo(44));
    }

    @Test
    public void shouldScheduleSyncTask() {
        // given
        Runnable task = mock(Runnable.class);
        given(schedulingAdapter.isGlobalThread()).willReturn(false);

        // when
        bukkitService.scheduleSyncTaskFromOptionallyAsyncTask(task);

        // then
        verify(schedulingAdapter).runOnGlobalThread(authMe, task);
        verifyNoInteractions(task);
    }

    @Test
    public void shouldRunTaskDirectly() {
        // given
        given(schedulingAdapter.isGlobalThread()).willReturn(true);
        bukkitService.reload(settings);
        Runnable task = mock(Runnable.class);

        // when
        bukkitService.scheduleSyncTaskFromOptionallyAsyncTask(task);

        // then
        verify(task).run();
        verify(schedulingAdapter).isGlobalThread();
    }

    @Test
    public void shouldScheduleEntityTask() {
        // given
        Entity entity = mock(Player.class);
        Runnable task = mock(Runnable.class);
        given(schedulingAdapter.isOwnedByCurrentThread(entity)).willReturn(false);

        // when
        bukkitService.scheduleSyncTaskFromOptionallyAsyncTask(entity, task);

        // then
        verify(schedulingAdapter).runOnEntityThread(authMe, entity, task);
        verifyNoInteractions(task);
    }

    @Test
    public void shouldRunEntityTaskDirectly() {
        // given
        Entity entity = mock(Player.class);
        Runnable task = mock(Runnable.class);
        given(schedulingAdapter.isOwnedByCurrentThread(entity)).willReturn(true);

        // when
        bukkitService.scheduleSyncTaskFromOptionallyAsyncTask(entity, task);

        // then
        verify(task).run();
    }

    @Test
    public void shouldRunTask() {
        // given
        Runnable task = () -> {/* noop */};
        BukkitTask bukkitTask = mock(BukkitTask.class);
        given(scheduler.runTask(authMe, task)).willReturn(bukkitTask);

        // when
        BukkitTask resultingTask = bukkitService.runTask(task);

        // then
        assertThat(resultingTask, equalTo(bukkitTask));
        verify(scheduler, only()).runTask(authMe, task);
    }

    @Test
    public void shouldRunTaskLater() {
        // given
        Runnable task = () -> {/* noop */};
        BukkitTask bukkitTask = mock(BukkitTask.class);
        long delay = 400;
        given(scheduler.runTaskLater(authMe, task, delay)).willReturn(bukkitTask);

        // when
        BukkitTask resultingTask = bukkitService.runTaskLater(task, delay);

        // then
        assertThat(resultingTask, equalTo(bukkitTask));
        verify(scheduler, only()).runTaskLater(authMe, task, delay);
    }

    @Test
    public void shouldRunTaskInAsync() {
        // given
        Runnable task = mock(Runnable.class);
        BukkitService spy = Mockito.spy(bukkitService);
        doReturn(mock(CancellableTask.class)).when(spy).runTaskAsynchronously(task);

        // when
        spy.runTaskOptionallyAsync(task);

        // then
        verifyNoInteractions(task);
        verify(spy).runTaskAsynchronously(task);
    }

    @Test
    public void shouldRunTaskDirectlyIfConfigured() {
        // given
        given(settings.getProperty(PluginSettings.USE_ASYNC_TASKS)).willReturn(false);
        bukkitService.reload(settings);
        BukkitService spy = Mockito.spy(bukkitService);
        Runnable task = mock(Runnable.class);

        // when
        spy.runTaskOptionallyAsync(task);

        // then
        verify(task).run();
        verify(spy, only()).runTaskOptionallyAsync(task);
    }

    @Test
    public void shouldRunTaskAsynchronously() {
        // given
        Runnable task = () -> {/* noop */};
        CancellableTask cancellableTask = mock(CancellableTask.class);
        given(schedulingAdapter.runAsyncTask(authMe, task)).willReturn(cancellableTask);

        // when
        CancellableTask resultingTask = bukkitService.runTaskAsynchronously(task);

        // then
        assertThat(resultingTask, equalTo(cancellableTask));
        verify(schedulingAdapter, only()).runAsyncTask(authMe, task);
    }

    @Test
    public void shouldRunTaskTimerAsynchronously() {
        // given
        Runnable task = mock(Runnable.class);
        long delay = 20L;
        long period = 4000L;
        CancellableTask cancellableTask = mock(CancellableTask.class);
        given(schedulingAdapter.runAsyncTaskTimer(authMe, task, delay, period)).willReturn(cancellableTask);

        // when
        CancellableTask resultingTask = bukkitService.runTaskTimerAsynchronously(task, delay, period);

        // then
        assertThat(resultingTask, equalTo(cancellableTask));
        verify(schedulingAdapter, only()).runAsyncTaskTimer(authMe, task, delay, period);
    }

    @Test
    public void shouldRunTaskTimer() {
        // given
        BukkitRunnable bukkitRunnable = mock(BukkitRunnable.class);
        long delay = 20;
        long period = 80;
        BukkitTask bukkitTask = mock(BukkitTask.class);
        given(bukkitRunnable.runTaskTimer(authMe, delay, period)).willReturn(bukkitTask);

        // when
        BukkitTask result = bukkitService.runTaskTimer(bukkitRunnable, delay, period);

        // then
        assertThat(result, equalTo(bukkitTask));
        verify(bukkitRunnable).runTaskTimer(authMe, delay, period);
    }

    @Test
    public void shouldBroadcastMessage() {
        // given
        String message = "Important message to all";
        given(server.broadcastMessage(message)).willReturn(24);

        // when
        int result = bukkitService.broadcastMessage(message);

        // then
        assertThat(result, equalTo(24));
        verify(server).broadcastMessage(message);
    }

    @Test
    public void shouldCreateAndEmitSyncEvent() {
        // given
        given(settings.getProperty(PluginSettings.USE_ASYNC_TASKS)).willReturn(false);
        bukkitService.reload(settings);
        Player player = mock(Player.class);

        // when
        FailedLoginEvent event = bukkitService.createAndCallEvent(isAsync -> new FailedLoginEvent(player, isAsync));

        // then
        verify(pluginManager).callEvent(event);
        assertThat(event.isAsynchronous(), equalTo(false));
        assertThat(event.getPlayer(), equalTo(player));
    }

    @Test
    public void shouldCreateAndEmitAsyncEvent() {
        // given
        Player player = mock(Player.class);

        // when
        FailedLoginEvent event = bukkitService.createAndCallEvent(isAsync -> new FailedLoginEvent(player, isAsync));

        // then
        verify(pluginManager).callEvent(event);
        assertThat(event.isAsynchronous(), equalTo(true));
        assertThat(event.getPlayer(), equalTo(player));
    }

    @Test
    public void shouldReturnServerIp() {
        // given
        String ip = "99.99.99.99";
        given(server.getIp()).willReturn(ip);

        // when
        String result = bukkitService.getIp();

        // then
        assertThat(result, equalTo(ip));
    }
}


