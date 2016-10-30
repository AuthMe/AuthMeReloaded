package fr.xephi.authme.datasource;

import com.github.authme.configme.properties.Property;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.security.HashAlgorithm;
import fr.xephi.authme.security.crypts.HashedPassword;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.SecuritySettings;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.sql.Blob;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test class which runs through a datasource implementation and verifies that all
 * instances of {@link AutoCloseable} that are created in the calls are closed again.
 * <p>
 * Instead of an actual connection to a datasource, we pass a mock Connection object
 * which is set to create additional mocks on demand for Statement and ResultSet objects.
 * This test ensures that all such objects that are created will be closed again by
 * keeping a list of mocks ({@link #closeables}) and then verifying that all have been
 * closed {@link #verifyHaveMocksBeenClosed()}.
 */
@RunWith(Parameterized.class)
public abstract class AbstractResourceClosingTest {

    /** List of DataSource method names not to test. */
    private static final Set<String> IGNORED_METHODS = ImmutableSet.of("reload", "close", "getType");

    /** Collection of values to use to call methods with the parameters they expect. */
    private static final Map<Class<?>, Object> PARAM_VALUES = getDefaultParameters();

    /**
     * Custom list of hash algorithms to use to test a method. By default we define {@link HashAlgorithm#XFBCRYPT} as
     * algorithms we use as a lot of methods execute additional statements in {@link MySQL}. If other algorithms
     * have custom behaviors, they can be supplied in this map so it will be tested as well.
     */
    private static final Map<String, HashAlgorithm[]> CUSTOM_ALGORITHMS = getCustomAlgorithmList();

    /** Mock of a settings instance. */
    private static Settings settings;

    /** The datasource to test. */
    private DataSource dataSource;

    /** The DataSource method to test. */
    private Method method;

    /** Keeps track of the closeables which are created during the tested call. */
    private List<AutoCloseable> closeables = new ArrayList<>();

    /**
     * Constructor for the test instance verifying the given method with the given hash algorithm.
     *
     * @param method The DataSource method to test
     * @param name The name of the method
     * @param algorithm The hash algorithm to use
     */
    public AbstractResourceClosingTest(Method method, String name, HashAlgorithm algorithm) {
        // Note ljacqu 20160227: The name parameter is necessary as we pass it from the @Parameters method;
        // we use the method name in the annotation to name the test sensibly
        this.method = method;
        given(settings.getProperty(SecuritySettings.PASSWORD_HASH)).willReturn(algorithm);
    }

    /** Initialize the settings mock and makes it return the default of any given property by default. */
    @SuppressWarnings("unchecked")
    @BeforeClass
    public static void initializeSettings() throws IOException, ClassNotFoundException {
        settings = mock(Settings.class);
        given(settings.getProperty(any(Property.class))).willAnswer(new Answer() {
            @Override
            public Object answer(InvocationOnMock invocation) {
                return ((Property<?>) invocation.getArguments()[0]).getDefaultValue();
            }
        });
        TestHelper.setupLogger();
    }

    /** Initialize the dataSource implementation to test based on a mock connection. */
    @Before
    public void setUpMockConnection() throws Exception {
        Connection connection = initConnection();
        dataSource = createDataSource(settings, connection);
    }

    /**
     * The actual test -- executes the method given through the constructor and then verifies that all
     * AutoCloseable mocks it constructed have been closed.
     */
    @Test
    public void shouldCloseResources() throws IllegalAccessException, InvocationTargetException {
        method.invoke(dataSource, buildParamListForMethod(method));
        verifyHaveMocksBeenClosed();
    }

    /**
     * Initialization method -- provides the parameters to run the test with by scanning all DataSource
     * methods. By default, we run one test per method with the default hash algorithm, XFBCRYPT.
     * If the map of custom algorithms has an entry for the method name, we add an entry for each algorithm
     * supplied by the map.
     *
     * @return Test parameters
     */
    @Parameterized.Parameters(name = "{1}({2})")
    public static Collection<Object[]> data() {
        List<Method> methods = getDataSourceMethods();
        List<Object[]> data = new ArrayList<>();
        // Use XFBCRYPT if nothing else specified as there is a lot of specific behavior to this hash algorithm in MySQL
        final HashAlgorithm[] defaultAlgorithm = new HashAlgorithm[]{HashAlgorithm.XFBCRYPT};
        for (Method method : methods) {
            HashAlgorithm[] algorithms = Objects.firstNonNull(CUSTOM_ALGORITHMS.get(method.getName()), defaultAlgorithm);
            for (HashAlgorithm algorithm : algorithms) {
                data.add(new Object[]{method, method.getName(), algorithm});
            }
        }
        return data;
    }

    /* Create a DataSource instance with the given mock settings and mock connection. */
    protected abstract DataSource createDataSource(Settings settings, Connection connection) throws Exception;

    /* Get all methods of the DataSource interface, minus the ones in the ignored list. */
    private static List<Method> getDataSourceMethods() {
        List<Method> publicMethods = new ArrayList<>();
        for (Method method : DataSource.class.getDeclaredMethods()) {
            if (!IGNORED_METHODS.contains(method.getName())) {
                publicMethods.add(method);
            }
        }
        return publicMethods;
    }

    /**
     * Verify that all AutoCloseables that have been created during the method execution have been closed.
     */
    private void verifyHaveMocksBeenClosed() {
        if (closeables.isEmpty()) {
            System.out.println("Note: detected no AutoCloseables for method '" + method.getName() + "'");
        }
        try {
            for (AutoCloseable autoCloseable : closeables) {
                verify(autoCloseable).close();
            }
        } catch (Exception e) {
            throw new IllegalStateException("Error verifying if autoCloseable was closed", e);
        }
    }

    /**
     * Helper method for building a list of test values to satisfy a method's signature.
     *
     * @param method The method to create a valid parameter list for
     * @return Parameter list to invoke the given method with
     */
    private static Object[] buildParamListForMethod(Method method) {
        List<Object> params = new ArrayList<>();
        int index = 0;
        for (Class<?> paramType : method.getParameterTypes()) {
            // Checking List.class == paramType instead of Class#isAssignableFrom means we really only accept List,
            // but that is a sensible assumption and makes our life much easier later on when juggling with Type
            Object param = Collection.class.isAssignableFrom(paramType)
                ? getTypedCollection(method.getGenericParameterTypes()[index])
                : PARAM_VALUES.get(paramType);
            Preconditions.checkNotNull(param, "No param type for " + paramType);
            params.add(param);
            ++index;
        }
        return params.toArray();
    }

    /**
     * Return a collection of the required type with some test elements that correspond to the
     * collection's generic type.
     *
     * @param type The collection type to process and build a test collection for
     * @return Test collection with sample elements of the correct type
     */
    private static Collection<?> getTypedCollection(Type type) {
        if (type instanceof ParameterizedType) {
            ParameterizedType parameterizedType = (ParameterizedType) type;
            Preconditions.checkArgument(Collection.class.isAssignableFrom((Class<?>) parameterizedType.getRawType()),
                type + " should extend from Collection");
            Type genericType = parameterizedType.getActualTypeArguments()[0];

            Object element = PARAM_VALUES.get(genericType);
            Preconditions.checkNotNull(element, "No sample element for list of generic type " + genericType);
            if (isAssignableFrom(parameterizedType.getRawType(), List.class)) {
                return Arrays.asList(element, element, element);
            } else if (Set.class == parameterizedType.getRawType()) {
                return new HashSet<>(Arrays.asList(element, element, element));
            }
            throw new IllegalStateException("Unknown collection type " + parameterizedType.getRawType());
        }
        throw new IllegalStateException("Cannot build list for unexpected Type: " + type);
    }

    private static boolean isAssignableFrom(Type type, Class<?> fromType) {
        return (type instanceof Class<?>)
            && ((Class<?>) type).isAssignableFrom(fromType);
    }

    /* Initialize the map of test values to pass to methods to satisfy their signature. */
    private static Map<Class<?>, Object> getDefaultParameters() {
        HashedPassword hash = new HashedPassword("test", "test");
        return ImmutableMap.<Class<?>, Object>builder()
            .put(String.class, "test")
            .put(int.class, 3)
            .put(long.class, 102L)
            .put(boolean.class, true)
            .put(PlayerAuth.class, PlayerAuth.builder().name("test").realName("test").password(hash).build())
            .put(HashedPassword.class, hash)
            .build();
    }

    /**
     * Return the custom list of hash algorithms to test a method with to execute code specific to
     * one hash algorithm. By default, XFBCRYPT is used. Only MySQL has code specific to algorithms
     * but for technical reasons the custom list will be used for all tested classes.
     *
     * @return List of custom algorithms by method
     */
    private static Map<String, HashAlgorithm[]> getCustomAlgorithmList() {
        // We use XFBCRYPT as default encryption method so we don't have to list many of the special cases for it
        return ImmutableMap.<String, HashAlgorithm[]>builder()
            .put("saveAuth", new HashAlgorithm[]{HashAlgorithm.PHPBB, HashAlgorithm.WORDPRESS})
            .build();
    }

    // ---------------------
    // Mock initialization
    // ---------------------
    /**
     * Initialize the connection mock which produces additional AutoCloseable mocks and records them.
     *
     * @return Connection mock
     */
    private Connection initConnection() {
        Connection connection = mock(Connection.class);
        try {
            given(connection.prepareStatement(anyString())).willAnswer(preparedStatementAnswer());
            given(connection.createStatement()).willAnswer(preparedStatementAnswer());
            given(connection.createBlob()).willReturn(mock(Blob.class));
            return connection;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not initialize connection mock", e);
        }
    }

    /* Create Answer that returns a PreparedStatement mock. */
    private Answer<PreparedStatement> preparedStatementAnswer() {
        return new Answer<PreparedStatement>() {
            @Override
            public PreparedStatement answer(InvocationOnMock invocation) throws SQLException {
                PreparedStatement pst = mock(PreparedStatement.class);
                closeables.add(pst);
                given(pst.executeQuery()).willAnswer(resultSetAnswer());
                given(pst.executeQuery(anyString())).willAnswer(resultSetAnswer());
                return pst;
            }
        };
    }

    /* Create Answer that returns a ResultSet mock. */
    private Answer<ResultSet> resultSetAnswer() throws SQLException {
        return new Answer<ResultSet>() {
            @Override
            public ResultSet answer(InvocationOnMock invocation) throws Throwable {
                ResultSet rs = initResultSet();
                closeables.add(rs);
                return rs;
            }
        };
    }

    /* Create a ResultSet mock. */
    private ResultSet initResultSet() throws SQLException {
        ResultSet rs = mock(ResultSet.class);
        // Return true for ResultSet#next the first time to make sure we execute all code
        given(rs.next()).willAnswer(new Answer<Boolean>() {
            boolean isInitial = true;
            @Override
            public Boolean answer(InvocationOnMock invocation) {
                if (isInitial) {
                    isInitial = false;
                    return true;
                }
                return false;
            }
        });
        given(rs.getString(anyInt())).willReturn("test");
        given(rs.getString(anyString())).willReturn("test");

        Blob blob = mock(Blob.class);
        given(blob.getBytes(anyLong(), anyInt())).willReturn(new byte[]{});
        given(blob.length()).willReturn(0L);
        given(rs.getBlob(anyInt())).willReturn(blob);
        given(rs.getBlob(anyString())).willReturn(blob);
        return rs;
    }

}
