package fr.xephi.authme.datasource.mysqlextensions;

import fr.xephi.authme.TestHelper;
import fr.xephi.authme.datasource.AbstractResourceClosingTest;
import fr.xephi.authme.datasource.Columns;
import fr.xephi.authme.settings.Settings;
import org.junit.BeforeClass;
import org.junit.runners.Parameterized;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;

/**
 * Checks that SQL resources are closed properly in {@link MySqlExtension} implementations.
 */
public abstract class AbstractMySqlExtensionResourceClosingTest extends AbstractResourceClosingTest {

    private static Settings settings;
    private static Columns columns;

    public AbstractMySqlExtensionResourceClosingTest(Method method, String name) {
        super(method, name);
    }

    @BeforeClass
    public static void initSettings() {
        settings = mock(Settings.class);
        TestHelper.returnDefaultsForAllProperties(settings);
        columns = new Columns(settings);
    }

    @Override
    protected MySqlExtension getObjectUnderTest() {
        return createExtension(settings, columns);
    }

    protected abstract MySqlExtension createExtension(Settings settings, Columns columns);

    @Parameterized.Parameters(name = "{1}")
    public static List<Object[]> createParameters() {
        return Arrays.stream(MySqlExtension.class.getDeclaredMethods())
            .filter(m -> Modifier.isPublic(m.getModifiers()))
            .map(m -> new Object[]{m, m.getName()})
            .collect(Collectors.toList());
    }
}
