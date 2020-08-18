package fr.xephi.authme.service.proxy.message;

import com.google.common.io.ByteArrayDataInput;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.service.BukkitService;

public interface ProxyMessageDecoderHandler {

    void decodeAndHandle(ByteArrayDataInput in, BukkitService service, Management management);
}
