package fr.xephi.authme.listener.protocollib;

import com.comphenix.protocol.PacketType;
import com.comphenix.protocol.ProtocolLibrary;
import com.comphenix.protocol.events.ListenerPriority;
import com.comphenix.protocol.events.PacketAdapter;
import com.comphenix.protocol.events.PacketEvent;
import com.comphenix.protocol.reflect.FieldAccessException;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.listener.ListenerService;
import fr.xephi.authme.output.ConsoleLoggerFactory;

class TabCompletePacketAdapter extends PacketAdapter {

    private final ConsoleLogger logger = ConsoleLoggerFactory.get(TabCompletePacketAdapter.class);

    private final ListenerService listenerService;

    TabCompletePacketAdapter(AuthMe plugin, ListenerService listenerService) {
        super(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.TAB_COMPLETE);
        this.listenerService = listenerService;
    }

    @Override
    public void onPacketReceiving(PacketEvent event) {
        if (event.getPacketType() == PacketType.Play.Client.TAB_COMPLETE) {
            try {
                String playerName = event.getPlayer().getName();
                if (listenerService.shouldRestrictPlayer(playerName)) {
                    event.setCancelled(true);
                }
            } catch (FieldAccessException e) {
                logger.logException("Couldn't access field:", e);
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
