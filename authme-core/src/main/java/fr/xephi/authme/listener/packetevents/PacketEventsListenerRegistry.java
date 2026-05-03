package fr.xephi.authme.listener.packetevents;

import com.github.retrooper.packetevents.PacketEvents;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.platform.PacketInterceptionAdapter;
import fr.xephi.authme.service.PendingPremiumCache;
import fr.xephi.authme.service.PremiumLoginVerifier;
import org.bukkit.entity.Player;

/**
 * PacketEvents-backed implementation of {@link PacketInterceptionAdapter}.
 * Instantiated lazily so PacketEvents classes are only resolved when the dependency is available.
 */
public final class PacketEventsListenerRegistry implements PacketInterceptionAdapter {

    private InventoryPacketListener inventoryPacketListener;
    private TabCompletePacketListener tabCompletePacketListener;
    private PremiumVerificationPacketListener premiumVerificationPacketListener;

    @Override
    public void registerInventoryProtection(PlayerCache playerCache, DataSource dataSource) {
        if (inventoryPacketListener == null) {
            inventoryPacketListener = new InventoryPacketListener(playerCache, dataSource);
        }
        PacketEvents.getAPI().getEventManager().registerListener(inventoryPacketListener);
    }

    @Override
    public void unregisterInventoryProtection() {
        if (inventoryPacketListener != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(inventoryPacketListener);
            inventoryPacketListener = null;
        }
    }

    @Override
    public void sendBlankInventoryPacket(Player player) {
        if (inventoryPacketListener != null) {
            inventoryPacketListener.sendBlankInventoryPacket(player);
        }
    }

    @Override
    public void registerTabCompleteBlock(PlayerCache playerCache) {
        if (tabCompletePacketListener == null) {
            tabCompletePacketListener = new TabCompletePacketListener(playerCache);
        }
        PacketEvents.getAPI().getEventManager().registerListener(tabCompletePacketListener);
    }

    @Override
    public void unregisterTabCompleteBlock() {
        if (tabCompletePacketListener != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(tabCompletePacketListener);
            tabCompletePacketListener = null;
        }
    }

    @Override
    public void registerPremiumVerification(DataSource dataSource, PremiumLoginVerifier verifier,
                                            PendingPremiumCache pendingPremiumCache) {
        if (premiumVerificationPacketListener == null) {
            premiumVerificationPacketListener =
                new PremiumVerificationPacketListener(dataSource, verifier, pendingPremiumCache);
        }
        PacketEvents.getAPI().getEventManager().registerListener(premiumVerificationPacketListener);
    }

    @Override
    public void unregisterPremiumVerification() {
        if (premiumVerificationPacketListener != null) {
            PacketEvents.getAPI().getEventManager().unregisterListener(premiumVerificationPacketListener);
            premiumVerificationPacketListener = null;
        }
    }
}
