package fr.xephi.authme.service.proxy.message;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import com.google.common.io.ByteStreams;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.proxy.ProxyMessenger;
import fr.xephi.authme.service.proxy.message.impl.LoginMessageDecoderHandler;
import fr.xephi.authme.service.proxy.message.impl.SendOnlyMessage;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public enum ProxyMessage {
    LOGIN(new LoginMessageDecoderHandler()),
    LOGGED_IN(SendOnlyMessage.INSTANCE),
    CONNECT(SendOnlyMessage.INSTANCE);

    private final ProxyMessageDecoderHandler messageHandler;

    // only for BungeeCord specific messages. Do not use it for anything other
    private static Map<String, String> EXCEPTION_MESSAGES = new ConcurrentHashMap<>();

    static {
        EXCEPTION_MESSAGES.put("connect", "ConnectOther");
    }

    ProxyMessage(ProxyMessageDecoderHandler messageHandler) {
        this.messageHandler = messageHandler;
    }

    public ByteArrayDataOutput encode(String... data) {
        ByteArrayDataOutput out = ByteStreams.newDataOutput();
        String name = name().toLowerCase();
        out.writeUTF(EXCEPTION_MESSAGES.getOrDefault(name, name));
        for (String utf : data) {
            out.writeUTF(utf);
        }
        return out;
    }

    public String getChannel() {
        return EXCEPTION_MESSAGES.containsKey(name().toLowerCase()) ?
            ProxyMessenger.BUNGEECORD_CHANNEL :
            ProxyMessenger.AUTHME_CHANNEL;
    }

    public boolean isBungee() {
        return getChannel().equalsIgnoreCase(ProxyMessenger.BUNGEECORD_CHANNEL);
    }

    public void decodeAndHandle(ByteArrayDataInput input, BukkitService service, Management management) {
        messageHandler.decodeAndHandle(input, service, management);
    }

    public static ProxyMessage match(String subChannel) {
        for (ProxyMessage message : ProxyMessage.values()) {
            if (message.name().toLowerCase().equalsIgnoreCase(subChannel)) {
                return message;
            }
        }
        return valueOf(subChannel.toUpperCase());
    }
}
