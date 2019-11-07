package fr.xephi.authme.datasource;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import fr.xephi.authme.TestHelper;
import fr.xephi.authme.data.auth.PlayerAuth;
import fr.xephi.authme.security.crypts.HashedPassword;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

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

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

/**
 * Test class which runs through objects interacting with a database and verifies that all
 * instances of {@link AutoCloseable} that are created in the calls are closed again.
 * <p>
 * Instead of an actual connection to a datasource, we pass a mock Connection object
 * which is set to create additional mocks on demand for Statement and ResultSet objects.
 * This test ensures that all such objects that are created will be closed again by
 * keeping a list of mocks ({@link #closeables}) and then verifying that all have been
 * closed ({@link #verifyHaveMocksBeenClosed(Method)}).
 */
public abstract class AbstractResourceClosingTest {

    /** Collection of values to use to call methods with the parameters they expect. */
    private static final Map<Class<?>, Object> PARAM_VALUES = getDefaultParameters();

    /** Keeps track of the closeables which are created during the tested call. */
    private List<AutoCloseable> closeables = new ArrayList<>();

    private boolean hasCreatedConnection = false;


    @BeforeAll
    static void initializeLogger() {
        TestHelper.setupLogger();
    }

    /**
     * The actual test -- executes the method given through the constructor and then verifies that all
     * AutoCloseable mocks it constructed have been closed.
     */
    @ParameterizedTest(name = "{1}")
    @MethodSource("createParameters")
    // Note ljacqu 20160227: The name parameter is necessary as we pass it from the arguments source method;
    // we use the method name in the annotation to name the test sensibly
    void shouldCloseResources(Method method, String name) throws IllegalAccessException, InvocationTargetException {
        method.invoke(getObjectUnderTest(), buildParamListForMethod(method));
        verifyHaveMocksBeenClosed(method);
    }

    protected abstract Object getObjectUnderTest();

    /**
     * Verify that all AutoCloseables that have been created during the method execution have been closed.
     */
    private void verifyHaveMocksBeenClosed(Method method) {
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
    private Object[] buildParamListForMethod(Method method) {
        List<Object> params = new ArrayList<>();
        int index = 0;
        for (Class<?> paramType : method.getParameterTypes()) {
            // Checking List.class == paramType instead of Class#isAssignableFrom means we really only accept List,
            // but that is a sensible assumption and makes our life much easier later on when juggling with Type
            Object param = Collection.class.isAssignableFrom(paramType)
                ? getTypedCollection(method.getGenericParameterTypes()[index])
                : getMethodParameter(paramType);
            Preconditions.checkNotNull(param, "No param type for " + paramType);
            params.add(param);
            ++index;
        }
        return params.toArray();
    }

    private Object getMethodParameter(Class<?> paramType) {
        if (paramType.equals(Connection.class)) {
            Preconditions.checkArgument(!hasCreatedConnection, "A Connection object was already created in this test run");
            hasCreatedConnection = true;
            return initConnection();
        }
        return PARAM_VALUES.get(paramType);
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

    // ---------------------
    // Mock initialization
    // ---------------------
    /**
     * Initializes the connection mock which produces additional AutoCloseable mocks and records them.
     *
     * @return Connection mock
     */
    protected Connection initConnection() {
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
        return invocation -> {
            PreparedStatement pst = mock(PreparedStatement.class);
            closeables.add(pst);
            given(pst.executeQuery()).willAnswer(resultSetAnswer());
            given(pst.executeQuery(anyString())).willAnswer(resultSetAnswer());
            return pst;
        };
    }

    /* Create Answer that returns a ResultSet mock. */
    private Answer<ResultSet> resultSetAnswer() {
        return invocation -> {
            ResultSet rs = initResultSet();
            closeables.add(rs);
            return rs;
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
