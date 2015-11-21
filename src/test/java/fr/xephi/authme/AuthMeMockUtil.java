package fr.xephi.authme;

import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.settings.Messages;
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
     * Sets the AuthMe plugin instance to a mock object. Use {@link AuthMe#getInstance()} to retrieve the mock.
     */
    public static void mockAuthMeInstance() {
        AuthMe mock = Mockito.mock(AuthMe.class);
        mockSingletonForClass(AuthMe.class, "plugin", mock);
    }

    /**
     * Creates a mock Messages object for the instance returned from {@link Messages#getInstance()}.
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
     * Sets a field of a class to the given mock.
     *
     * @param clazz the class to modify
     * @param fieldName the field name
     * @param mock the mock to set for the given field
     */
    private static void mockSingletonForClass(Class<?> clazz, String fieldName, Object mock) {
        try {
            Field instance = clazz.getDeclaredField(fieldName);
            instance.setAccessible(true);
            instance.set(null, mock);
        } catch (NoSuchFieldException | IllegalAccessException e) {
            throw new RuntimeException("Could not set mock instance for class " + clazz.getName(), e);
        }
    }
}
