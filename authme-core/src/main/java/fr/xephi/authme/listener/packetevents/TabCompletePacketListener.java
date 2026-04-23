package fr.xephi.authme.listener.packetevents;

import com.github.retrooper.packetevents.event.PacketListenerAbstract;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import fr.xephi.authme.data.auth.PlayerCache;
import org.bukkit.entity.Player;

public class TabCompletePacketListener extends PacketListenerAbstract {

    private final PlayerCache playerCache;

    public TabCompletePacketListener(PlayerCache playerCache) {
        this.playerCache = playerCache;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.TAB_COMPLETE) {
            Player player = event.getPlayer();
            if (player != null && !playerCache.isAuthenticated(player.getName())) {
                event.setCancelled(true);
            }
        }
    }
}
