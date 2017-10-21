package fr.xephi.authme.command.executable.authme.debug;

import fr.xephi.authme.ClassCollector;
import fr.xephi.authme.TestHelper;
import org.junit.BeforeClass;
import org.junit.Test;

import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * Consistency tests for {@link DebugSection} implementors.
 */
public class DebugSectionConsistencyTest {

    private static List<Class<?>> debugClasses;
    private static List<DebugSection> debugSections;

    @BeforeClass
    public static void collectClasses() {
        // TODO ljacqu 20171021: Improve ClassCollector (pass pkg by class, improve #getInstancesOfType's instantiation)
        ClassCollector classCollector = new ClassCollector(
            TestHelper.SOURCES_FOLDER, TestHelper.PROJECT_ROOT + "command/executable/authme/debug");

        debugClasses = classCollector.collectClasses();
        debugSections = classCollector.getInstancesOfType(DebugSection.class, clz -> instantiate(clz));
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
        for (DebugSection debugSection : debugSections) {
            if (!names.add(debugSection.getName())) {
                fail("Encountered name '" + debugSection.getName() + "' a second time in " + debugSection.getClass());
            }
        }
    }

    @Test
    public void shouldAllHaveDescription() {
        for (DebugSection debugSection : debugSections) {
            assertThat("Description of '" + debugSection.getClass() + "' may not be null",
                debugSection.getDescription(), not(nullValue()));
        }
    }

    private static DebugSection instantiate(Class<? extends DebugSection> clazz) {
        try {
            return ClassCollector.canInstantiate(clazz) ? clazz.newInstance() : null;
        } catch (InstantiationException | IllegalAccessException e) {
            throw new IllegalStateException(e);
        }
    }
}
