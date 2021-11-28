package fr.xephi.authme.events;

import fr.xephi.authme.ClassCollector;
import fr.xephi.authme.TestHelper;
import org.bukkit.event.Event;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Checks the consistency of the AuthMe event classes.
 */
public class EventsConsistencyTest {

    private static final String EVENTS_FOLDER = TestHelper.PROJECT_ROOT + "events/";
    private static List<Class<? extends Event>> classes;

    @BeforeClass
    public static void scanEventClasses() {
        ClassCollector classCollector = new ClassCollector(TestHelper.SOURCES_FOLDER, EVENTS_FOLDER);
        classes = classCollector.collectClasses(Event.class);

        if (classes.isEmpty()) {
            throw new IllegalStateException("Did not find any AuthMe event classes. Is the folder correct?");
        }
    }

    @Test
    public void shouldExtendFromCustomEvent() {
        for (Class<?> clazz : classes) {
            assertThat("Class " + clazz.getSimpleName() + " is subtype of CustomEvent",
                CustomEvent.class.isAssignableFrom(clazz), equalTo(true));
        }
    }

    /**
     * Bukkit requires a static getHandlerList() method on all event classes, see {@link Event}.
     * This test checks that such a method is present, and that it is <i>absent</i> if the class
     * is not instantiable (abstract class).
     */
    @Test
    public void shouldHaveStaticEventHandlerMethod() {
        for (Class<?> clazz : classes) {
            Method handlerListMethod = null;
            try {
                handlerListMethod = clazz.getMethod("getHandlerList");
            } catch (NoSuchMethodException ignored) {
            }
            if (canBeInstantiated(clazz)) {
                assertThat("Class " + clazz.getSimpleName() + " has static method getHandlerList()",
                    handlerListMethod != null && Modifier.isStatic(handlerListMethod.getModifiers()), equalTo(true));
            } else {
                assertThat("Non-instantiable class " + clazz.getSimpleName() + " does not have static getHandlerList()",
                    handlerListMethod, nullValue());
            }
        }
    }

    private static boolean canBeInstantiated(Class<?> clazz) {
        return !clazz.isInterface() && !clazz.isEnum() && !Modifier.isAbstract(clazz.getModifiers());
    }
}
