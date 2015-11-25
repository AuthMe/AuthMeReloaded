package fr.xephi.authme;

import fr.xephi.authme.cache.auth.PlayerCache;
import fr.xephi.authme.settings.Messages;
import fr.xephi.authme.util.Wrapper;
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
     * Set the given class' {@link Wrapper} field to a mock implementation.
     *
     * @param clazz The class to modify
     * @param fieldName The name of the field containing the Wrapper in the class
     *
     * @return The generated Wrapper mock
     * @see WrapperMock
     */
    public static Wrapper insertMockWrapperInstance(Class<?> clazz, String fieldName) {
        Wrapper wrapperMock = new WrapperMock();
        mockSingletonForClass(clazz, fieldName, wrapperMock);
        return wrapperMock;
    }

    public static Wrapper insertMockWrapperInstance(Class<?> clazz, String fieldName, AuthMe authMe) {
        Wrapper wrapperMock = new WrapperMock(authMe);
        mockSingletonForClass(clazz, fieldName, wrapperMock);
        return wrapperMock;
    }

    // TODO ljacqu 20151123: Find the use cases for the WrapperMock and remove any of these
    // methods that will end up unused
    public static Wrapper insertMockWrapperInstance(Class<?> clazz, String fieldName, WrapperMock wrapperMock) {
        mockSingletonForClass(clazz, fieldName, wrapperMock);
        return wrapperMock;
    }

    /**
     * Set a field of a class to the given mock.
     *
     * @param clazz The class to modify
     * @param fieldName The field name
     * @param mock The mock to set for the given field
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
