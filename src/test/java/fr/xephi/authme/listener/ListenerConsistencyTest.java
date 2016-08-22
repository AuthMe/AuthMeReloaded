package fr.xephi.authme.listener;

import com.google.common.collect.Sets;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Test for verifying that AuthMe listener methods are well-formed.
 */
public final class ListenerConsistencyTest {

    private static final Class<?>[] LISTENERS = { BlockListener.class, EntityListener.class,
        PlayerListener.class, PlayerListener16.class, PlayerListener18.class, PlayerListener19.class,
        ServerListener.class };

    private static final Set<String> CANCELED_EXCEPTIONS = Sets.newHashSet(
        "PlayerListener#onPlayerJoin", "PlayerListener#onPlayerLogin",
        "PlayerListener#onPlayerQuit", "ServerListener#onPluginDisable",
        "ServerListener#onServerPing", "ServerListener#onPluginEnable",
        "PlayerListener#onJoinMessage");

    @Test
    public void shouldSetIgnoreCancelledToTrue() {
        for (Class<?> listener : LISTENERS) {
            checkCanceledAttribute(listener);
        }
    }

    @Test
    public void shouldHaveOnlyEventListenersAsPublicMembers() {
        for (Class<?> listener : LISTENERS) {
            checkPublicMethodsAreListeners(listener);
        }
    }

    // #367: Event listeners with EventPriority.MONITOR should not change events
    @Test
    public void shouldNotHaveMonitorLevelEventHandlers() {
        for (Class<?> listener : LISTENERS) {
            verifyListenerIsNotUsingMonitorPriority(listener);
        }
    }

    @Test
    public void shouldNotHaveMultipleMethodsWithSameName() {
        Set<String> events = new HashSet<>();
        for (Class<?> listener : LISTENERS) {
            for (Method method : listener.getDeclaredMethods()) {
                if (isTestableMethod(method) && events.contains(method.getName())) {
                    fail("More than one method '" + method.getName() + "' exists (e.g. class: " + listener + ")");
                }
                events.add(method.getName());
            }
        }
    }

    private static void checkCanceledAttribute(Class<?> listenerClass) {
        final String clazz = listenerClass.getSimpleName();
        Method[] methods = listenerClass.getDeclaredMethods();
        for (Method method : methods) {
            if (isTestableMethod(method) && method.isAnnotationPresent(EventHandler.class)) {
                if (CANCELED_EXCEPTIONS.contains(clazz + "#" + method.getName())) {
                    continue;
                }

                EventHandler eventHandlerAnnotation = method.getAnnotation(EventHandler.class);
                assertThat("Method " + clazz + "#" + method.getName() + " should have ignoreCancelled = true",
                    eventHandlerAnnotation.ignoreCancelled(), equalTo(true));
            }
        }
    }

    private static void checkPublicMethodsAreListeners(Class<?> listenerClass) {
        final String clazz = listenerClass.getSimpleName();
        Method[] methods = listenerClass.getDeclaredMethods();
        for (Method method : methods) {
            if (isTestableMethod(method) && !method.isAnnotationPresent(EventHandler.class)) {
                fail("Expected @EventHandler annotation on non-private method " + clazz + "#" + method.getName());
            }
        }
    }

    private static void verifyListenerIsNotUsingMonitorPriority(Class<?> listenerClass) {
        final String clazz = listenerClass.getSimpleName();
        for (Method method : listenerClass.getDeclaredMethods()) {
            if (isTestableMethod(method) && method.isAnnotationPresent(EventHandler.class)) {
                EventHandler eventHandlerAnnotation = method.getAnnotation(EventHandler.class);
                assertThat("Method " + clazz + "#" + method.getName() + " does not use EventPriority.MONITOR",
                    eventHandlerAnnotation.priority(), not(equalTo(EventPriority.MONITOR)));
            }
        }
    }

    private static boolean isTestableMethod(Method method) {
        // Exclude any methods with "$" in it: jacoco creates a "$jacocoInit" method we want to ignore, and
        // methods like "access$000" are created by the compiler when a private member is being accessed by an inner
        // class, which is not of interest for us
        if (Modifier.isPrivate(method.getModifiers()) || method.getName().contains("$")) {
            return false;
        }
        // Skip reload() method (implementation of Reloadable interface)
        return !"reload".equals(method.getName()) || method.getParameterTypes().length > 0;
    }

}
