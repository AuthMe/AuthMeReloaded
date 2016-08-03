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

    private static void mockShouldCancel(boolean result, ListenerService listenerService, Event event) {
        if (event instanceof PlayerEvent) {
            given(listenerService.shouldCancelEvent((PlayerEvent) event)).willReturn(result);
        } else if (event instanceof EntityEvent) {
            given(listenerService.shouldCancelEvent((EntityEvent) event)).willReturn(result);
        } else {
            throw new IllegalStateException("Found event with unsupported type: " + event.getClass());
        }
    }

    private static <T> Method findMethod(Listener listener, Class<T> paramType) {
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
