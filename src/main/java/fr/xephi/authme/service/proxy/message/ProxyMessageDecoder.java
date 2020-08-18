package fr.xephi.authme.service.proxy.message;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteStreams;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.proxy.ProxyMessenger;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;
import org.jetbrains.annotations.NotNull;

public final class ProxyMessageDecoder implements PluginMessageListener {

    private final BukkitService service;
    private final Management management;

    public ProxyMessageDecoder(BukkitService service, Management management) {
        this.service = service;
        this.management = management;
    }

    @Override
    public void onPluginMessageReceived(@NotNull String channel, @NotNull Player player, @NotNull byte[] data) {
        if (!channel.equalsIgnoreCase(ProxyMessenger.AUTHME_CHANNEL)) {
            return;
        }

        ByteArrayDataInput in = ByteStreams.newDataInput(data);
        String subChannel = in.readUTF();
        ProxyMessage matched = ProxyMessage.match(subChannel);
        if (matched != null) {
            matched.decodeAndHandle(in, service, management);
        }
    }
}
