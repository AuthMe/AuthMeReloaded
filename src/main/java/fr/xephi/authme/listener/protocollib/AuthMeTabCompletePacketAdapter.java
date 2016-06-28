package fr.xephi.authme.listener.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.FieldAccessException;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerCache;

import javax.inject.Inject;

public class AuthMeTabCompletePacketAdapter extends PacketAdapter {

    @Inject
    public AuthMeTabCompletePacketAdapter(AuthMe plugin) {
        super(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.TAB_COMPLETE);
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.TAB_COMPLETE) {
            try {
                if (!PlayerCache.getInstance().isAuthenticated(event.getPlayer().getName().toLowerCase())) {
                    event.setCancelled(true);
                }
            } catch (FieldAccessException e) {
                ConsoleLogger.showError("Couldn't access field.");
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
