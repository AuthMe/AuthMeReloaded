package fr.xephi.authme.events;

import org.apache.commons.lang.reflect.MethodUtils;
import org.bukkit.event.Event;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

/**
 * Checks the consistency of the AuthMe event classes.
 */
public class EventsConsistencyTest {

    private static final String SRC_FOLDER = "src/main/java/";
    private static final String EVENTS_FOLDER = SRC_FOLDER + "/fr/xephi/authme/events/";
    private static List<Class<? extends Event>> classes;

    @BeforeClass
    public static void scanEventClasses() {
        File eventsFolder = new File(EVENTS_FOLDER);
        File[] filesInFolder = eventsFolder.listFiles();
        if (filesInFolder == null || filesInFolder.length == 0) {
            throw new IllegalStateException("Could not read folder '" + EVENTS_FOLDER + "'. Is it correct?");
        }

        classes = new ArrayList<>();
        for (File file : filesInFolder) {
            Class<? extends Event> clazz = getEventClassFromFile(file);
            if (clazz != null) {
                classes.add(clazz);
            }
        }
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
            Method handlerListMethod = MethodUtils.getAccessibleMethod(clazz, "getHandlerList", new Class<?>[]{});
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

    private static Class<? extends Event> getEventClassFromFile(File file) {
        String fileName = file.getPath();
        String className = fileName
            .substring(SRC_FOLDER.length(), fileName.length() - ".java".length())
            .replace(File.separator, ".");
        try {
            Class<?> clazz = EventsConsistencyTest.class.getClassLoader().loadClass(className);
            if (Event.class.isAssignableFrom(clazz)) {
                return (Class<? extends Event>) clazz;
            }
            return null;
        } catch (ClassNotFoundException e) {
            throw new IllegalStateException("Could not load class '" + className + "'", e);
        }
    }

}
