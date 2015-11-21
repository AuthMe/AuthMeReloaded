package fr.xephi.authme;

import org.mockito.Mockito;

import java.lang.reflect.Field;

/**
 * Creates a mock implementation of AuthMe for testing purposes.
 */
public final class AuthMeMockUtil {

    private AuthMeMockUtil() {
        // Util class
    }

    /**
     * Set the AuthMe plugin instance to a mock object. Use {@link AuthMe#getInstance()} to retrieve the mock.
     */
    public static void initialize() {
        AuthMe mock = Mockito.mock(AuthMe.class);

        try {
            Field instance = AuthMe.class.getDeclaredField("plugin");
            instance.setAccessible(true);
            instance.set(null, mock);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Could not initialize AuthMe mock", e);
        }
    }
}
