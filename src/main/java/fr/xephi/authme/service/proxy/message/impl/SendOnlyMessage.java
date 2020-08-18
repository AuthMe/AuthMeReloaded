package fr.xephi.authme.service.proxy.message.impl;

import com.google.common.io.ByteArrayDataInput;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.proxy.message.ProxyMessageDecoderHandler;

public class SendOnlyMessage implements ProxyMessageDecoderHandler {

    public static final SendOnlyMessage INSTANCE = new SendOnlyMessage();

    private SendOnlyMessage() {
    }

    @Override
    public void decodeAndHandle(ByteArrayDataInput in, BukkitService service, Management management) {
    }
}
