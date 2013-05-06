package uk.org.whoami.authme.plugin.manager;

import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import uk.org.whoami.authme.AuthMe;
import uk.org.whoami.authme.ConsoleLogger;
 
public class BungeeCordMessage implements PluginMessageListener {

    public AuthMe plugin;
 
    public BungeeCordMessage(AuthMe plugin)
    {
        this.plugin = plugin;
    }
 
    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
    	ConsoleLogger.info("PluginMessage send to " + player.getName() + " , the message was : " + message.toString());
    	plugin.realIp.put(player.getName().toLowerCase(), message.toString());
    }

}