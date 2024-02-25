package fr.xephi.authme.task;

import ch.jalu.injector.factory.SingletonStore;
import fr.xephi.authme.initialization.HasCleanup;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.List;

import static java.util.Arrays.asList;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test for {@link CleanupTask}.
 */
@RunWith(MockitoJUnitRunner.class)
public class CleanupTaskTest {

    @InjectMocks
    private CleanupTask cleanupTask;

    @Mock
    private SingletonStore<HasCleanup> hasCleanupStore;

    @Test
    public void shouldPerformCleanup() {
        // given
        List<HasCleanup> services = asList(mock(HasCleanup.class), mock(HasCleanup.class), mock(HasCleanup.class));
        given(hasCleanupStore.retrieveAllOfType()).willReturn(services);

        // when
        cleanupTask.accept(mock(CancellableTask.class));

        // then
        verify(services.get(0)).performCleanup();
        verify(services.get(1)).performCleanup();
        verify(services.get(2)).performCleanup();
    }
}
