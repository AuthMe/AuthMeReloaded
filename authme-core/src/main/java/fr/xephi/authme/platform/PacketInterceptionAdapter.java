package fr.xephi.authme.platform;

import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.service.PendingPremiumCache;
import fr.xephi.authme.service.PremiumLoginVerifier;
import org.bukkit.entity.Player;

/**
 * Platform-specific packet interception for inventory protection, tab-complete blocking,
 * and cryptographic premium session verification.
 * Implementations are provided by each version module and use the PacketEvents library.
 */
public interface PacketInterceptionAdapter {

    void registerInventoryProtection(PlayerCache playerCache, DataSource dataSource);

    void unregisterInventoryProtection();

    void sendBlankInventoryPacket(Player player);

    void registerTabCompleteBlock(PlayerCache playerCache);

    void unregisterTabCompleteBlock();

    void registerPremiumVerification(DataSource dataSource, PremiumLoginVerifier verifier,
                                     PendingPremiumCache pendingPremiumCache);

    void unregisterPremiumVerification();

    /**
     * Returns {@code true} if the server is configured to receive player connections via a
     * BungeeCord or Velocity proxy (i.e., proxy IP forwarding is enabled at the server level).
     * When this returns {@code true}, the premium PacketEvents verification listener must NOT
     * be registered: the proxy handles the login handshake, and any synthetic
     * {@code EncryptionRequest} sent by the backend would cause the proxy to abort the connection
     * with "Backend server is online-mode!".
     *
     * <p>The default implementation returns {@code false}. Version adapters override it to inspect
     * the server's own proxy-forwarding configuration (e.g. {@code spigot.yml settings.bungeecord}
     * or Paper's {@code proxies.velocity.enabled}).</p>
     */
    default boolean isProxyForwardingEnabled() {
        return false;
    }
}
