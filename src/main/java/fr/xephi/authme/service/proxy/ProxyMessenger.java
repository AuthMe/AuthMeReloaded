package fr.xephi.authme.service.proxy;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.ConsoleLogger;
import fr.xephi.authme.initialization.SettingsDependent;
import fr.xephi.authme.output.ConsoleLoggerFactory;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.proxy.message.ProxyMessageDecoder;
import fr.xephi.authme.service.proxy.message.ProxyMessageEncoder;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.properties.HooksSettings;
import org.bukkit.plugin.messaging.Messenger;

import javax.inject.Inject;

public final class ProxyMessenger implements SettingsDependent {

    public static final String AUTHME_CHANNEL = "authme:plugin";
    public static final String BUNGEECORD_CHANNEL = "BungeeCord";
    public static final ConsoleLogger LOGGER = ConsoleLoggerFactory.get(ProxyMessenger.class);

    private final AuthMe plugin;
    private final BukkitService service;
    private final Management management;

    private final ProxyMessageEncoder encoder;

    private String connectServer;

    @Inject
    ProxyMessenger(AuthMe plugin, BukkitService bukkitService, Management management, Settings settings) {
        this.plugin = plugin;
        this.service = bukkitService;
        this.management = management;
        this.encoder = new ProxyMessageEncoder(plugin);
        reload(settings);
    }

    public ProxyMessageEncoder getEncoder() {
        return encoder;
    }

    public String getConnectServer() {
        return connectServer;
    }

    public boolean shouldSendConnectMessage() {
        return connectServer != null && !connectServer.isEmpty();
    }

    @Override
    public void reload(Settings settings) {
        boolean enabled = settings.getProperty(HooksSettings.PROXY);
        connectServer = settings.getProperty(HooksSettings.PROXY_SERVER);

        Messenger messenger = plugin.getServer().getMessenger();
        if (enabled) {
            if (!messenger.isIncomingChannelRegistered(plugin, AUTHME_CHANNEL)) {
                messenger.registerIncomingPluginChannel(
                    plugin,
                    AUTHME_CHANNEL,
                    new ProxyMessageDecoder(service, management)
                );
            }
            if (!messenger.isOutgoingChannelRegistered(plugin, AUTHME_CHANNEL)) {
                messenger.registerOutgoingPluginChannel(plugin, AUTHME_CHANNEL);
                messenger.registerOutgoingPluginChannel(plugin, BUNGEECORD_CHANNEL);
            }
        } else {
            if (messenger.isIncomingChannelRegistered(plugin, AUTHME_CHANNEL)) {
                messenger.unregisterIncomingPluginChannel(plugin, AUTHME_CHANNEL);
            }
            if (messenger.isOutgoingChannelRegistered(plugin, AUTHME_CHANNEL)) {
                messenger.unregisterOutgoingPluginChannel(plugin, AUTHME_CHANNEL);
            }
            if (messenger.isOutgoingChannelRegistered(plugin, BUNGEECORD_CHANNEL)) {
                messenger.unregisterOutgoingPluginChannel(plugin, BUNGEECORD_CHANNEL);
            }
        }
        encoder.setEnabled(enabled);
    }
}
