package uk.org.whoami.authme.plugin.manager;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import uk.org.whoami.authme.AuthMe;
 
public class BungeeCordMessage implements PluginMessageListener {

    public AuthMe plugin;
 
    public BungeeCordMessage(AuthMe plugin)
    {
        this.plugin = plugin;
    }
 
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        try {
            final DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
            if (in.readUTF().equals("IP")) { //We need only the IP channel
                plugin.realIp.put(player.getName().toLowerCase(), in.readUTF()); //Put the IP (only the ip not the port) in the hashmap
            }
        } catch (IOException ex) {
        }
    }

}