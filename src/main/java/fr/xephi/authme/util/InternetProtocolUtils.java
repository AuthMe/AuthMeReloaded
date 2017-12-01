package fr.xephi.authme.util;

import java.util.regex.Pattern;

/**
 * Utility class about the InternetProtocol
 */
public final class InternetProtocolUtils {

    private static final Pattern LOCAL_ADDRESS_PATTERN =
        Pattern.compile("(^127\\.)|(^(0)?10\\.)|(^172\\.(0)?1[6-9]\\.)|(^172\\.(0)?2[0-9]\\.)"
            + "|(^172\\.(0)?3[0-1]\\.)|(^169\\.254\\.)|(^192\\.168\\.)");

    // Utility class
    private InternetProtocolUtils() {
    }

    /**
     * Checks if the specified address is a private or loopback address
     *
     * @param address address to check
     *
     * @return true if the address is a local or loopback address, false otherwise
     */
    public static boolean isLocalAddress(String address) {
        return LOCAL_ADDRESS_PATTERN.matcher(address).find();
    }
}
