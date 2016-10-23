package fr.xephi.authme.command.executable.authme;

import fr.xephi.authme.task.purge.PurgeService;
import org.bukkit.command.CommandSender;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.argThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

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
        verifyZeroInteractions(purgeService);
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
        verifyZeroInteractions(purgeService);
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
        verify(purgeService).runPurge(eq(sender), captor.capture(), eq(false));

        // Check the timestamp with a certain tolerance
        int toleranceMillis = 100;
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -Integer.valueOf(interval));
        assertIsCloseTo(captor.getValue(), calendar.getTimeInMillis(), toleranceMillis);
    }

    @Test
    public void shouldProcessCommandWithAllParameter() {
        // given
        String interval = "32";
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Arrays.asList(interval, "all"));

        // then
        ArgumentCaptor<Long> captor = ArgumentCaptor.forClass(Long.class);
        verify(purgeService).runPurge(eq(sender), captor.capture(), eq(true));

        // Check the timestamp with a certain tolerance
        int toleranceMillis = 100;
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, -Integer.valueOf(interval));
        assertIsCloseTo(captor.getValue(), calendar.getTimeInMillis(), toleranceMillis);
    }

    @Test
    public void shouldRejectCommandWithInvalidSecondParameter() {
        // given
        String interval = "80";
        CommandSender sender = mock(CommandSender.class);

        // when
        command.executeCommand(sender, Arrays.asList(interval, "bogus"));

        // then
        verify(sender).sendMessage(
            argThat(containsString("Purge process aborted; use '/authme purge " + interval + " all'")));
        verifyZeroInteractions(purgeService);
    }

    private static void assertIsCloseTo(long value1, long value2, long tolerance) {
        assertThat(Math.abs(value1 - value2), not(greaterThan(tolerance)));
    }

}
