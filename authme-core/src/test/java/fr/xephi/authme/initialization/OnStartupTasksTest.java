package fr.xephi.authme.initialization;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.message.Messages;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.EmailSettings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link OnStartupTasks}.
 */
@ExtendWith(MockitoExtension.class)
class OnStartupTasksTest {

    @InjectMocks
    private OnStartupTasks onStartupTasks;

    @Mock
    private DataSource dataSource;
    @Mock
    private Settings settings;
    @Mock
    private BukkitService bukkitService;
    @Mock
    private Messages messages;

    @BeforeAll
    static void initLogger() {
        TestHelper.setupLogger();
    }

    @Test
    void shouldRemindPlayersWithoutEmailOnGlobalRegion() {
        // given
        given(settings.getProperty(EmailSettings.RECALL_PLAYERS)).willReturn(true);
        given(settings.getProperty(EmailSettings.DELAY_RECALL)).willReturn(5);
        given(dataSource.getLoggedPlayersWithEmptyMail()).willReturn(Collections.singletonList("bobby"));
        onStartupTasks.scheduleRecallEmailTask();
        ArgumentCaptor<Runnable> taskCaptor = ArgumentCaptor.forClass(Runnable.class);
        verify(bukkitService).runTaskTimerAsynchronously(taskCaptor.capture(), anyLong(), anyLong());

        // when
        taskCaptor.getValue().run();

        // then
        verify(bukkitService).runOnGlobalRegion(any(Runnable.class));
        verify(bukkitService, never()).runTask(any(Runnable.class));
    }
}
