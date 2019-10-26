package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.task.purge.PurgeService;
import org.bukkit.command.CommandSender;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Calendar;
import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.hamcrest.MockitoHamcrest.argThat;

/**
 * Test for {@link PurgeCommand}.
 */
@RunWith(MockitoJUnitRunner.class)
public class PurgeCommandTest {

    @InjectMocks
    private PurgeCommand command;

    @Mock
    private PurgeService purgeService;

    @Test
    public void shouldHandleInvalidNumber() {
        // given
        String interval = "invalid";
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.singletonList(interval));

        // then
        verify(sender).sendMessage(argThat(containsString("The value you've entered is invalid")));
        verifyNoInteractions(purgeService);
    }

    @Test
    public void shouldRejectTooSmallInterval() {
        // given
        String interval = "29";
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.singletonList(interval));

        // then
        verify(sender).sendMessage(argThat(containsString("You can only purge data older than 30 days")));
        verifyNoInteractions(purgeService);
    }

    @Test
    public void shouldForwardToService() {
        // given
        String interval = "45";
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Collections.singletonList(interval));

        // then
        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(purgeService).runPurge(eq(sender), captor.capture());

        // Check the timestamp with a certain tolerance
        int toleranceMillis = 100;
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -Integer.valueOf(interval));
        assertIsCloseTo(captor.getValue(), calendar.getTimeInMillis(), toleranceMillis);
    }

    private static void assertIsCloseTo(long value1, long value2, long tolerance) {
        assertThat(Math.abs(value1 - value2), not(greaterThan(tolerance)));
    }

}
