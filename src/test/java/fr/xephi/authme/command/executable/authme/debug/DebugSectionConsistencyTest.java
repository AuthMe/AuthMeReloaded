package fr.xephi.authme.command.executable.authme.debug;

import fr.xephi.authme.ClassCollector;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Consistency tests for {@link DebugSection} implementors.
 */
public class DebugSectionConsistencyTest {

    private static List<Class<?>> debugClasses;

    @BeforeClass
    public static void collectClasses() {
        debugClasses = new ClassCollector("src/main/java", "fr/xephi/authme/command/executable/authme/debug")
            .collectClasses();
    }

    @Test
    public void shouldAllBePackagePrivate() {
        for (Class<?> clazz : debugClasses) {
            if (clazz != DebugCommand.class) {
                assertThat(clazz + " should be package-private",
                    Modifier.isPublic(clazz.getModifiers()), equalTo(false));
            }
        }
    }

    @Test
    public void shouldHaveDifferentSubcommandName() throws IllegalAccessException, InstantiationException {
        Set<String> names = new HashSet<>();
        for (Class<?> clazz : debugClasses) {
            if (DebugSection.class.isAssignableFrom(clazz) && !clazz.isInterface()) {
                DebugSection debugSection = (DebugSection) clazz.newInstance();
                if (!names.add(debugSection.getName())) {
                    fail("Encountered name '" + debugSection.getName() + "' a second time in " + clazz);
                }
            }
        }
    }
}
