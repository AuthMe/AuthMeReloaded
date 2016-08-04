package fr.xephi.authme.listener;

import fr.xephi.authme.ReflectionTestUtils;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityEvent;
import org.bukkit.event.player.PlayerEvent;

import java.lang.reflect.Method;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;

/**
 * Utilities for testing AuthMe listener classes.
 */
public final class ListenerTestUtils {

    private ListenerTestUtils() {
    }

    /**
     * Tests a simple event handler that checks with the {@link ListenerService}
     * if the event should be canceled or not. This method tests that the event is
     * canceled when the service says so and the other way around. Do not use this
     * method if the handler method has additional behavior.
     *
     *
     * @param listener the listener to test
     * @param listenerService the listener service mock
     * @param clazz the event class to test the handler method for
     * @param <T> the event type
     */
    public static <T extends Event & Cancellable>
        void checkEventIsCanceledForUnauthed(Listener listener, ListenerService listenerService, Class<T> clazz) {
        Method handlerMethod = findMethod(listener, clazz);

        T event = mock(clazz);
        mockShouldCancel(true, listenerService, event);
        ReflectionTestUtils.invokeMethod(handlerMethod, listener, event);
        verify(event).setCancelled(true);

        event = mock(clazz);
        mockShouldCancel(false, listenerService, event);
        ReflectionTestUtils.invokeMethod(handlerMethod, listener, event);
        verifyZeroInteractions(event);
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

    /**
     * Returns the method in the listener that takes the given event type as parameter.
     *
     * @param listener the listener to scan
     * @param paramType the event type
     * @return the mapped method
     * @throws IllegalStateException if there is not exactly one method with the given event type as parameter
     */
    private static Method findMethod(Listener listener, Class<?> paramType) {
        Method matchingMethod = null;
        for (Method method : listener.getClass().getMethods()) {
            if (method.isAnnotationPresent(EventHandler.class)) {
                Class<?>[] parameters = method.getParameterTypes();
                if (parameters.length == 1 && parameters[0] == paramType) {
                    if (matchingMethod == null) {
                        matchingMethod = method;
                    } else {
                        throw new IllegalStateException("Found multiple eligible methods for " + paramType);
                    }
                }
            }
        }
        if (matchingMethod == null) {
            throw new IllegalStateException("Found no matching method for " + paramType);
        }
        return matchingMethod;
    }

}
