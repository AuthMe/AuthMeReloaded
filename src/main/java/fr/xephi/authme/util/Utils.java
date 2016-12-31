package fr.xephi.authme.util;

import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.message.MessageKey;
import fr.xephi.authme.process.register.RegisterSecondaryArgument;
import fr.xephi.authme.process.register.RegistrationType;

import java.util.regex.Pattern;

/**
 * Utility class for various operations used in the codebase.
 */
public final class Utils {

    /** Number of milliseconds in a minute. */
    public static final long MILLIS_PER_MINUTE = 60_000L;
    /** Number of milliseconds in an hour. */
    public static final long MILLIS_PER_HOUR = 60 * MILLIS_PER_MINUTE;

    // Utility class
    private Utils() {
    }

    /**
     * Compile Pattern sneaky without throwing Exception.
     *
     * @param pattern pattern string to compile
     *
     * @return the given regex compiled into Pattern object.
     */
    public static Pattern safePatternCompile(String pattern) {
        try {
            return Pattern.compile(pattern);
        } catch (Exception e) {
            ConsoleLogger.warning("Failed to compile pattern '" + pattern + "' - defaulting to allowing everything");
            return Pattern.compile(".*?");
        }
    }

    /**
     * Returns whether the class exists in the current class loader.
     *
     * @param className the class name to check
     *
     * @return true if the class is loaded, false otherwise
     */
    public static boolean isClassLoaded(String className) {
        try {
            Class.forName(className);
            return true;
        } catch (ClassNotFoundException e) {
            return false;
        }
    }

    /**
     * Return the available core count of the JVM.
     *
     * @return the core count
     */
    public static int getCoreCount() {
        return Runtime.getRuntime().availableProcessors();
    }

    /**
     * Returns the proper message key for the given registration types.
     *
     * @param registrationType the registration type
     * @param secondaryArgType secondary argument type for the register command
     * @return the message key
     */
    // TODO #1037: Remove this method
    public static MessageKey getRegisterMessage(RegistrationType registrationType,
                                                RegisterSecondaryArgument secondaryArgType) {
        if (registrationType == RegistrationType.PASSWORD) {
            if (secondaryArgType == RegisterSecondaryArgument.CONFIRMATION) {
                return MessageKey.REGISTER_MESSAGE;
            } else if (secondaryArgType == RegisterSecondaryArgument.NONE) {
                return MessageKey.REGISTER_NO_REPEAT_MESSAGE;
            } else { /* EMAIL_MANDATORY || EMAIL_OPTIONAL */
                return MessageKey.REGISTER_PASSWORD_EMAIL_MESSAGE;
            }
        } else { /* registrationType == EMAIL */
            if (secondaryArgType == RegisterSecondaryArgument.NONE) {
                return MessageKey.REGISTER_EMAIL_NO_REPEAT_MESSAGE;
            } else { /* CONFIRMATION || EMAIL_MANDATORY || EMAIL_OPTIONAL */
                return MessageKey.REGISTER_EMAIL_MESSAGE;
            }
        }
    }
}
