package fr.xephi.authme.listener;

import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.player.PlayerEvent;

import java.util.function.Consumer;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Tests simple listener methods that should cancel an event when the listener service says so.
 */
public final class EventCancelVerifier {

    private final ListenerService listenerService;

    private EventCancelVerifier(ListenerService listenerService) {
        this.listenerService = listenerService;
    }

    /**
     * Creates a new verifier that uses the given ListenerService mock (needs to be the same instance
     * as used in the listener class to test).
     *
     * @param listenerService the listener service mock
     * @return new verifier
     */
    public static EventCancelVerifier withServiceMock(ListenerService listenerService) {
        return new EventCancelVerifier(listenerService);
    }

    /**
     * Tests a simple event handler that checks with the {@link ListenerService}
     * if the event should be canceled or not. This method tests that the event is
     * canceled when the service says so and the other way around. Do not use this
     * method if the handler method has additional behavior.
     *
     * @param listenerMethod the listener method to test
     * @param clazz the event class to test the handler method for
     * @param <T> the event type
     * @return the verifier (for chaining of methods)
     */
    public <T extends Event & Cancellable> EventCancelVerifier check(Consumer<T> listenerMethod, Class<T> clazz) {
        T event = mock(clazz);
        mockShouldCancel(true, listenerService, event);
        listenerMethod.accept(event);
        verify(event).setCancelled(true);

        event = mock(clazz);
        mockShouldCancel(false, listenerService, event);
        listenerMethod.accept(event);
        verifyZeroInteractions(event);

        return this;
    }

    /**
     * Mocks, based on the given event, the correct method in {@link ListenerService} to return
     * the provided {@code result}.
     *
     * @param result the result the service should return
     * @param listenerService the service to mock
     * @param event the event
     */
    private static void mockShouldCancel(boolean result, ListenerService listenerService, Event event) {
        if (event instanceof PlayerEvent) {
            given(listenerService.shouldCancelEvent((PlayerEvent) event)).willReturn(result);
        } else if (event instanceof EntityEvent) {
            given(listenerService.shouldCancelEvent((EntityEvent) event)).willReturn(result);
        } else {
            throw new IllegalStateException("Found event with unsupported type: " + event.getClass());
        }
    }
}
