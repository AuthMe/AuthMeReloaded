package fr.xephi.authme.platform;

import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import org.bukkit.entity.Player;

/**
 * Platform-specific packet interception for inventory protection and tab-complete blocking.
 * Implementations are provided by each version module and use the PacketEvents library.
 */
public interface PacketInterceptionAdapter {

    void registerInventoryProtection(PlayerCache playerCache, DataSource dataSource);

    void unregisterInventoryProtection();

    void sendBlankInventoryPacket(Player player);

    void registerTabCompleteBlock(PlayerCache playerCache);

    void unregisterTabCompleteBlock();
}
