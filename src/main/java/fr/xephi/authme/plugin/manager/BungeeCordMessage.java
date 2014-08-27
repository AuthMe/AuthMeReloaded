package fr.xephi.authme.plugin.manager;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import fr.xephi.authme.AuthMe;

public class BungeeCordMessage implements PluginMessageListener {

    public AuthMe plugin;

    public BungeeCordMessage(AuthMe plugin) {
        this.plugin = plugin;
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player,
            byte[] message) {
        if (!channel.equals("BungeeCord")) {
            return;
        }
        try {
            final DataInputStream in = new DataInputStream(new ByteArrayInputStream(message));
            String subchannel = in.readUTF();
            if (subchannel.equals("IP")) { // We need only the IP channel
                String ip = in.readUTF();
                plugin.realIp.put(player.getName(), ip);
                // Put the IP (only the ip not the port) in the hashmap
            }
        } catch (IOException ex) {
        }
    }

}
