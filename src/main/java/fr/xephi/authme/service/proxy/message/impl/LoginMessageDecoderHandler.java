package fr.xephi.authme.service.proxy.message.impl;

import com.google.common.io.ByteArrayDataInput;
import fr.xephi.authme.process.Management;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.service.proxy.ProxyMessenger;
import fr.xephi.authme.service.proxy.message.ProxyMessageDecoderHandler;
import org.bukkit.entity.Player;

public class LoginMessageDecoderHandler implements ProxyMessageDecoderHandler {

    @Override
    public void decodeAndHandle(ByteArrayDataInput in, BukkitService service, Management management) {
        String username = in.readUTF();
        Player player = service.getPlayerExact(username);
        if (player != null && player.isOnline()) {
            management.forceLogin(player);
            ProxyMessenger.LOGGER.info("The user " + player.getName() + " has been automatically logged in, "
                + "as requested by proxy.");
        }
    }
}
