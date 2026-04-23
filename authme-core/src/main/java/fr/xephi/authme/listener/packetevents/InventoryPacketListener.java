package fr.xephi.authme.listener.packetevents;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.item.ItemStack;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSetSlot;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerWindowItems;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerCache;
import fr.xephi.authme.datasource.DataSource;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import org.bukkit.entity.Player;

import java.util.Collections;

public class InventoryPacketListener extends PacketListenerAbstract {

    private static final int PLAYER_INVENTORY = 0;
    // Crafting (5) + Armor (4) + Main inventory (27) + Hotbar (9) = 45 slots
    private static final int INVENTORY_SIZE = 45;

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(InventoryPacketListener.class);
    private final PlayerCache playerCache;
    private final DataSource dataSource;

    public InventoryPacketListener(PlayerCache playerCache, DataSource dataSource) {
        this.playerCache = playerCache;
        this.dataSource = dataSource;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.WINDOW_ITEMS) {
            if (new WrapperPlayServerWindowItems(event).getWindowId() == PLAYER_INVENTORY) {
                cancelIfShouldHide(event);
            }
        } else if (event.getPacketType() == PacketType.Play.Server.SET_SLOT) {
            if (new WrapperPlayServerSetSlot(event).getWindowId() == PLAYER_INVENTORY) {
                cancelIfShouldHide(event);
            }
        }
    }

    private void cancelIfShouldHide(PacketSendEvent event) {
        Player player = event.getPlayer();
        if (player != null && !playerCache.isAuthenticated(player.getName())
                && dataSource.isAuthAvailable(player.getName())) {
            event.setCancelled(true);
        }
    }

    public void sendBlankInventoryPacket(Player player) {
        WrapperPlayServerWindowItems packet = new WrapperPlayServerWindowItems(
            PLAYER_INVENTORY, 0,
            Collections.nCopies(INVENTORY_SIZE, ItemStack.EMPTY),
            ItemStack.EMPTY
        );
        try {
            PacketEvents.getAPI().getPlayerManager().sendPacket(player, packet);
        } catch (Exception e) {
            logger.logException("Error during sending blank inventory", e);
        }
    }
}
