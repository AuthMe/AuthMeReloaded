package fr.xephi.authme.util;

import java.net.InetAddress;
import java.net.UnknownHostException;

/**
 * Utility class about the InternetProtocol
 */
public final class InternetProtocolUtils {

    // Utility class
    private InternetProtocolUtils() {
    }

    /**
     * Checks if the specified address is a private or loopback address
     *
     * @param address address to check
     *
     * @return true if the address is a local (site and link) or loopback address, false otherwise
     */
    public static boolean isLocalAddress(String address) {
        try {
            InetAddress byName = InetAddress.getByName(address);

            // Examples: 127.0.0.1, localhost or [::1]
            return byName.isLoopbackAddress()
                    // Example: 10.0.0.0, 172.16.0.0, 192.168.0.0, fec0::/10 (deprecated)
                    // Ref: https://en.wikipedia.org/wiki/IP_address#Private_addresses
                    || byName.isSiteLocalAddress()
                    // Example: 169.254.0.0/16, fe80::/10
                    // Ref: https://en.wikipedia.org/wiki/IP_address#Address_autoconfiguration
                    || byName.isLinkLocalAddress()
                    // non deprecated unique site-local that java doesn't check yet -> fc00::/7
                    || isIPv6UniqueSiteLocal(byName);
        } catch (UnknownHostException e) {
            return false;
        }
    }

    private static boolean isIPv6UniqueSiteLocal(InetAddress address) {
        // ref: https://en.wikipedia.org/wiki/Unique_local_address

        // currently undefined but could be used in the near future fc00::/8
        return (address.getAddress()[0] & 0xFF) == 0xFC
                // in use for unique site-local fd00::/8
                || (address.getAddress()[0] & 0xFF) == 0xFD;
    }
}
