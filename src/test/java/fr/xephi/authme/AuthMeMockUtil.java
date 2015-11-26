package fr.xephi.authme;

import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.util.Wrapper;
import fr.xephi.authme.util.WrapperMock;
import org.mockito.Mockito;

import java.lang.reflect.Field;

/**
 * Creates a mock implementation of AuthMe for testing purposes.
 */
@Deprecated
public final class AuthMeMockUtil {

    private AuthMeMockUtil() {
        // Util class
    }

    /**
     * Set the AuthMe plugin instance to a mock object. Use {@link AuthMe#getInstance()} to retrieve the mock.
     *
     * @return The generated mock for the AuthMe instance
     */
    public static AuthMe mockAuthMeInstance() {
        AuthMe mock = Mockito.mock(AuthMe.class);
        mockSingletonForClass(AuthMe.class, "plugin", mock);
        return mock;
    }

    /**
     * Create a mock Messages object for the instance returned from {@link Messages#getInstance()}.
     */
    public static void mockMessagesInstance() {
        Messages mock = Mockito.mock(Messages.class);
        mockSingletonForClass(Messages.class, "singleton", mock);
    }

    /**
     * Creates a mock singleton for the player cache, retrievable from {@link PlayerCache#getInstance()}.
     */
    public static void mockPlayerCacheInstance() {
        PlayerCache mock = Mockito.mock(PlayerCache.class);
        mockSingletonForClass(PlayerCache.class, "singleton", mock);
    }


    /**
     * Set a field of a class to the given mock.
     *
     * @param clazz The class to modify
     * @param fieldName The field name
     * @param mock The mock to set for the given field
     */
    public static void mockSingletonForClass(Class<?> clazz, String fieldName, Object mock) {
        try {
            Field instance = clazz.getDeclaredField(fieldName);
            instance.setAccessible(true);
            instance.set(null, mock);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Could not set mock instance for class " + clazz.getName(), e);
        }
    }
}
