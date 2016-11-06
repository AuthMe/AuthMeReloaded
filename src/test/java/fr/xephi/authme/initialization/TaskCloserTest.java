package fr.xephi.authme.initialization;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.service.BukkitService;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginLogger;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitWorker;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.hamcrest.Matchers.contains;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link TaskCloser}.
 */
@RunWith(MockitoJUnitRunner.class)
public class TaskCloserTest {

    private static final int[] ACTIVE_WORKERS_ID = {2, 5};

    private TaskCloser taskCloser;
    @Mock
    private AuthMe authMe;
    @Mock
    private PluginLogger logger;
    @Mock
    private BukkitScheduler bukkitScheduler;
    @Mock
    private DataSource dataSource;
    @Mock
    private BukkitService bukkitService;

    @Before
    public void initAuthMe() {
        Server server = mock(Server.class);
        given(server.getScheduler()).willReturn(bukkitScheduler);
        ReflectionTestUtils.setField(JavaPlugin.class, authMe, "server", server);
        ReflectionTestUtils.setField(JavaPlugin.class, authMe, "logger", logger);
        taskCloser = spy(new TaskCloser(authMe, dataSource, bukkitService));
    }

    @Test
    public void shouldWaitForTasksToClose() throws InterruptedException {
        // given
        doNothing().when(taskCloser).sleep(); // avoid sleeping in tests
        mockActiveWorkers();
        given(bukkitScheduler.isCurrentlyRunning(ACTIVE_WORKERS_ID[0])).willReturn(false);
        given(bukkitScheduler.isCurrentlyRunning(ACTIVE_WORKERS_ID[1]))
            .willReturn(true) // first time
            .willReturn(false); // second time

        // when
        taskCloser.run();

        // then
        verify(bukkitScheduler, times(3)).isQueued(anyInt());
        ArgumentCaptor<Integer> taskIds = ArgumentCaptor.forClass(Integer.class);
        verify(bukkitScheduler, times(3)).isCurrentlyRunning(taskIds.capture());
        assertThat(taskIds.getAllValues(), contains(ACTIVE_WORKERS_ID[0], ACTIVE_WORKERS_ID[1], ACTIVE_WORKERS_ID[1]));
        verify(taskCloser, times(2)).sleep();
        verify(dataSource).close();
    }

    @Test
    public void shouldAbortForNeverEndingTask() throws InterruptedException {
        // given
        doNothing().when(taskCloser).sleep(); // avoid sleeping in tests
        mockActiveWorkers();
        // This task never ends
        given(bukkitScheduler.isCurrentlyRunning(ACTIVE_WORKERS_ID[0])).willReturn(true);
        given(bukkitScheduler.isCurrentlyRunning(ACTIVE_WORKERS_ID[1])).willReturn(false);

        // when
        taskCloser.run();

        // then
        verify(bukkitScheduler, times(3)).isQueued(anyInt());
        verify(bukkitScheduler, times(61)).isCurrentlyRunning(anyInt());
        verify(taskCloser, times(60)).sleep();
        verify(dataSource).close();
    }

    @Test
    public void shouldStopForInterruptedThread() throws InterruptedException, ExecutionException {
        // Note ljacqu 20160827: This test must be run in its own thread because we throw an InterruptedException.
        // Somehow the java.nio.Files API used in tests that are run subsequently don't like this and fail otherwise.
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(new Runnable() {
            @Override
            public void run() {
                try {
                    shouldStopForInterruptedThread0();
                } catch (Exception e) {
                    throw new IllegalStateException(e);
                }
            }
        }).get();
    }

    /** Test implementation for {@link #shouldStopForInterruptedThread()}. */
    private void shouldStopForInterruptedThread0() throws InterruptedException {
        // given
        taskCloser = spy(new TaskCloser(authMe, null, bukkitService));
        // First two times do nothing, third time throw exception when we sleep
        doNothing().doNothing().doThrow(InterruptedException.class).when(taskCloser).sleep();
        mockActiveWorkers();
        given(bukkitScheduler.isCurrentlyRunning(anyInt())).willReturn(true);

        // when
        taskCloser.run();

        // then
        verify(bukkitScheduler, times(4)).isCurrentlyRunning(anyInt());
        verify(taskCloser, times(3)).sleep();
    }

    private void mockActiveWorkers() {
        Plugin otherOwner = mock(Plugin.class);
        List<BukkitWorker> tasks = Arrays.asList(
            mockBukkitWorker(authMe, ACTIVE_WORKERS_ID[0], false),
            mockBukkitWorker(otherOwner, 3, false),
            mockBukkitWorker(authMe, ACTIVE_WORKERS_ID[1], false),
            mockBukkitWorker(authMe, 7, true),
            mockBukkitWorker(otherOwner, 11, true));
        given(bukkitScheduler.getActiveWorkers()).willReturn(tasks);
    }

    private BukkitWorker mockBukkitWorker(Plugin owner, int taskId, boolean isQueued) {
        BukkitWorker worker = mock(BukkitWorker.class);
        given(worker.getOwner()).willReturn(owner);
        given(worker.getTaskId()).willReturn(taskId);
        given(bukkitScheduler.isQueued(taskId)).willReturn(isQueued);
        return worker;
    }
}
