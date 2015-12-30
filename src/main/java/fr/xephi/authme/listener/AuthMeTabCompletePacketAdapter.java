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

public class AuthMeTabCompletePacketAdapter extends PacketAdapter {

	public AuthMeTabCompletePacketAdapter(AuthMe plugin) {
		super(plugin, ListenerPriority.NORMAL, PacketType.Play.Client.TAB_COMPLETE);
	}

	@Override
    public void onPacketReceiving(PacketEvent event)
    {
      if (event.getPacketType() == PacketType.Play.Client.TAB_COMPLETE) {
        try
        {
          String message = ((String)event.getPacket().getSpecificModifier(String.class).read(0)).toLowerCase();
          if ((message.startsWith("")) && (!message.contains("  ")) && !PlayerCache.getInstance().isAuthenticated(event.getPlayer().getName().toLowerCase())) {
            event.setCancelled(true);
          }
        }
        catch (FieldAccessException e)
        {
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
