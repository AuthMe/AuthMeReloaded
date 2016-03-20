package fr.xephi.authme.listener;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.FieldAccessException;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.cache.auth.PlayerCache;

public class AuthMeTablistPacketAdapter extends PacketAdapter {

    public AuthMeTablistPacketAdapter(AuthMe plugin) {
        super(plugin, ListenerPriority.NORMAL, PacketType.Play.Server.PLAYER_INFO);
    }

    @Override
    public void onPacketSending(PacketEvent event) {
        if (event.getPacketType() == PacketType.Play.Server.PLAYER_INFO) {
            try {
                if (!PlayerCache.getInstance().isAuthenticated(event.getPlayer().getName().toLowerCase())) {
                    event.setCancelled(true);
                }
            } catch (FieldAccessException e) {
                ConsoleLogger.showError("Couldn't access field.");
            }
        }
    }

    // TODO: fix this in 1.9
    public void register() {
        ConsoleLogger.showError("The hideTablistBeforeLogin feature is temporarily disabled due to issues with 1.9 clients.");
        //ProtocolLibrary.getProtocolManager().addPacketListener(this);
    }

    public void unregister() {
        //ProtocolLibrary.getProtocolManager().removePacketListener(this);
    }
}
