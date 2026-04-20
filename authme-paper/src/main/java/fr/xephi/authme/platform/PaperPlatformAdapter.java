package fr.xephi.authme.platform;

import fr.xephi.authme.listener.PaperChatListener;
import fr.xephi.authme.listener.PlayerOpenSignListener;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerKickEvent;

import java.util.Arrays;
import java.util.List;

/**
 * Platform adapter implementation for PaperMC 1.21.
 * Uses Paper's async teleport API for non-blocking player teleportation.
 */
public class PaperPlatformAdapter extends AbstractSpigotPlatformAdapter {

    @Override
    public void teleportPlayer(Player player, Location location) {
        player.teleportAsync(location);
    }

    @Override
    public String getPlatformName() {
        return "paper-1.21";
    }

    @Override
    public List<Class<? extends Listener>> getAdditionalListeners() {
        return Arrays.asList(PaperChatListener.class, PlayerOpenSignListener.class);
    }

    @Override
    public String getKickReason(PlayerKickEvent event) {
        return PlainTextComponentSerializer.plainText().serialize(event.reason());
    }
}
