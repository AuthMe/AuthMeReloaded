package fr.xephi.authme.listener.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.FieldAccessException;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.data.auth.PlayerCache;

class TabCompletePacketAdapter extends PacketAdapter {

    private final PlayerCache playerCache;

    TabCompletePacketAdapter(AuthMe plugin, PlayerCache playerCache) {
        super(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.TAB_COMPLETE);
        this.playerCache = playerCache;
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.TAB_COMPLETE) {
            try {
                if (!playerCache.isAuthenticated(event.getPlayer().getName())) {
                    event.setCancelled(true);
                }
            } catch (FieldAccessException e) {
                ConsoleLogger.logException("Couldn't access field:", e);
            }
        }
    }

    public void register() {
        ProtocolLibrary.getProtocolManager().addPacketListener(this);
    }

    public void unregister() {
        ProtocolLibrary.getProtocolManager().removePacketListener(this);
    }
}
