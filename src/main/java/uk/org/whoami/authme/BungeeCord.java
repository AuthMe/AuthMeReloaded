package uk.org.whoami.authme;

import java.io.ByteArrayInputStream;
import java.io.DataInputStream;
import java.io.IOException;

import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import uk.org.whoami.authme.settings.Settings;


public class BungeeCord implements PluginMessageListener {
	
	public AuthMe plugin;
	
	public BungeeCord(AuthMe plugin) {
		this.plugin = plugin;
	}

	    @Override
	    public void onPluginMessageReceived(String channel, Player player,
		    byte[] message) {

		if (channel.equals("BungeeCord")) {
		    DataInputStream in = new DataInputStream(new ByteArrayInputStream(
			    message));
		    try {

			String packetType = in.readUTF();

			if (packetType.equals("IP")) {

			    if (player.isOnline()) {
			    	String ip = in.readUTF();

			    	if (!Settings.noConsoleSpam)
			    	ConsoleLogger.info(
			    			"Got data from bungeecord: Player "
			    			+ player.getName()
			    			+ " Logged in with IP :" + ip);

			    	String name = player.getName();
			    	plugin.bungeesIp.put(name, ip);
			    }
			}
		    } catch (IOException e) {
			e.printStackTrace();
		    }
		}
	    }
}
