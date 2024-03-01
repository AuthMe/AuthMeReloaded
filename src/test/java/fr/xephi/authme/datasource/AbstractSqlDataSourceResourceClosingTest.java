package fr.xephi.authme.datasource;

import com.google.common.collect.ImmutableSet;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.settings.Settings;
import org.junit.jupiter.api.BeforeAll;

import java.lang.reflect.Method;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import static org.mockito.Mockito.mock;

/**
 * Resource-closing test for SQL data sources.
 */
public abstract class AbstractSqlDataSourceResourceClosingTest extends AbstractResourceClosingTest {

    /** List of DataSource method names not to test. */
    private static final Set<String> IGNORED_METHODS = ImmutableSet.of("reload", "getType", "isCached");

    private static Settings settings;

    @BeforeAll
    static void initializeSettings() {
        settings = mock(Settings.class);
        TestHelper.returnDefaultsForAllProperties(settings);
        TestHelper.setupLogger();
    }

    protected DataSource getObjectUnderTest() {
        try {
            return createDataSource(settings, initConnection());
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    /* Create a DataSource instance with the given mock settings and mock connection. */
    protected abstract DataSource createDataSource(Settings settings, Connection connection) throws Exception;

    /**
     * Initialization method -- provides the parameters to run the test with by scanning all DataSource methods.
     *
     * @return Test parameters
     */
    public static Collection<Object[]> createParameters() {
        List<Method> methods = getDataSourceMethods();
        List<Object[]> data = new ArrayList<>();
        for (Method method : methods) {
            data.add(new Object[]{method, method.getName()});
        }
        return data;
    }

    /* Get all methods of the DataSource interface, minus the ones in the ignored list. */
    private static List<Method> getDataSourceMethods() {
        return Arrays.stream(DataSource.class.getDeclaredMethods())
            .filter(method -> !IGNORED_METHODS.contains(method.getName()) && !method.isSynthetic())
            .collect(Collectors.toList());
    }
}
