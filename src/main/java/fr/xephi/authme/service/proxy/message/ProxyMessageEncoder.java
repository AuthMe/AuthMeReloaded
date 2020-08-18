package fr.xephi.authme.service.proxy.message;

import com.google.common.collect.Iterables;
import fr.xephi.authme.AuthMe;
import fr.xephi.authme.service.proxy.ProxyMessenger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

public class ProxyMessageEncoder {

    private final AuthMe plugin;
    private boolean enabled;

    public ProxyMessageEncoder(AuthMe plugin) {
        this.plugin = plugin;
    }

    public void sendMessage(ProxyMessage message, String... data) {
        if (!plugin.isEnabled()) {
            ProxyMessenger.LOGGER.debug(
                "Tried to send a " + message.name().toLowerCase() + " message but the plugin was disabled."
            );
            return;
        }
        if (!enabled) {
            return;
        }
        if (message.isBungee()) {
            plugin.getServer().getScheduler()
                .scheduleSyncDelayedTask(plugin, () -> send(message, data), 5L);
        } else {
            send(message, data);
        }
    }

    private void send(ProxyMessage message, String... data) {
        Player player = Iterables.getFirst(Bukkit.getOnlinePlayers(), null);
        if (player == null || !player.isOnline()) {
            // what is going on???
            return;
        }
        player.sendPluginMessage(plugin, message.getChannel(), message.encode(data).toByteArray());
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }
}
