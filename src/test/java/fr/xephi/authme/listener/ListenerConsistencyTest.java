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

    private static final Class<?>[] LISTENERS = { AuthMeBlockListener.class, AuthMeEntityListener.class,
        AuthMePlayerListener.class, AuthMePlayerListener16.class, AuthMePlayerListener18.class,
        AuthMeServerListener.class };

    // TODO #368: Ensure that these exceptions are really intentional, if not fix them and remove them here
    private static final Set<String> CANCELED_EXCEPTIONS = Sets.newHashSet("AuthMePlayerListener#onPlayerJoin",
        "AuthMePlayerListener#onPreLogin", "AuthMePlayerListener#onPlayerLogin",
        "AuthMePlayerListener#onPlayerQuit", "AuthMeServerListener#onPluginDisable",
        "AuthMeServerListener#onServerPing", "AuthMeServerListener#onPluginEnable");

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
    public void shouldNotHaveMultipleHandlersForSameEvent() {
        Set<String> events = new HashSet<>();
        for (Class<?> listener : LISTENERS) {
            for (Method method : listener.getDeclaredMethods()) {
                if (events.contains(method.getName())) {
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
        // A method like "access$000" is created by the compiler when a private member is being accessed by an inner
        // class, so we need to ignore such methods
        return !Modifier.isPrivate(method.getModifiers()) && !method.getName().startsWith("access$");
    }

}
