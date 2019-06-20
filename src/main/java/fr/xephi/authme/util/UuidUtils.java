package fr.xephi.authme.util;

import java.util.UUID;

/**
 * Utility class for various operations on UUID.
 */
public final class UuidUtils {

    // Utility class
    private UuidUtils() {
    }

    /**
     * Returns whether the given string as an UUID or null
     *
     * @param string the uuid to parse
     * @return parsed UUID if succeed or null
     */
    public static UUID parseUuidSafely(String string) {
        try {
            return UUID.fromString(string);
        } catch (IllegalArgumentException | NullPointerException ex) {
            return null;
        }
    }
}
