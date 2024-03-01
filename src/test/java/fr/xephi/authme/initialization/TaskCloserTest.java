package fr.xephi.authme.initialization;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ReflectionTestUtils;
import fr.xephi.authme.datasource.DataSource;
import org.bukkit.Server;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginLogger;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitScheduler;
import org.bukkit.scheduler.BukkitWorker;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
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
@ExtendWith(MockitoExtension.class)
class TaskCloserTest {

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

    @BeforeEach
    void initAuthMe() {
        Server server = mock(Server.class);
        given(server.getScheduler()).willReturn(bukkitScheduler);
        ReflectionTestUtils.setField(JavaPlugin.class, authMe, "server", server);
        given(authMe.getLogger()).willReturn(logger);
        taskCloser = spy(new TaskCloser(authMe, dataSource));
    }

    @Test
    void shouldWaitForTasksToClose() throws InterruptedException {
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
        verify(dataSource).closeConnection();
    }

    @Test
    void shouldAbortForNeverEndingTask() throws InterruptedException {
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
        verify(dataSource).closeConnection();
    }

    @Test
    void shouldStopForInterruptedThread() throws InterruptedException, ExecutionException {
        // Note ljacqu 20160827: This test must be run in its own thread because we throw an InterruptedException.
        // Somehow the java.nio.Files API used in tests that are run subsequently don't like this and fail otherwise.
        ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.submit(() -> {
            try {
                shouldStopForInterruptedThread0();
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }).get();
    }

    /** Test implementation for {@link #shouldStopForInterruptedThread()}. */
    private void shouldStopForInterruptedThread0() throws InterruptedException {
        // given
        taskCloser = spy(new TaskCloser(authMe, null));
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
            new BukkitWorkerTestImpl(authMe, ACTIVE_WORKERS_ID[0]),
            new BukkitWorkerTestImpl(otherOwner, 3),
            new BukkitWorkerTestImpl(authMe, ACTIVE_WORKERS_ID[1]),
            new BukkitWorkerTestImpl(authMe, 7),
            new BukkitWorkerTestImpl(otherOwner, 11));
        given(bukkitScheduler.getActiveWorkers()).willReturn(tasks);
        given(bukkitScheduler.isQueued(anyInt())).willAnswer(invocation -> {
            int taskId = invocation.getArgument(0);
            return taskId == 7 || taskId == 11;
        });
    }

    private static class BukkitWorkerTestImpl implements BukkitWorker {

        private final Plugin owner;
        private final int taskId;

        BukkitWorkerTestImpl(Plugin owner, int taskId) {
            this.owner = owner;
            this.taskId = taskId;
        }

        @Override
        public int getTaskId() {
            return taskId;
        }

        @Override
        public Plugin getOwner() {
            return owner;
        }

        @Override
        public Thread getThread() {
            return null;
        }
    }
}
