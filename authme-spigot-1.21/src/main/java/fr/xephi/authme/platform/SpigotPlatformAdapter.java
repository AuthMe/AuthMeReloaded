package fr.xephi.authme.platform;

import fr.xephi.authme.listener.PlayerSignOpenListener;
import org.bukkit.event.Listener;

import java.util.Arrays;
import java.util.List;

/**
 * Platform adapter implementation for Spigot 1.21.
 */
public class SpigotPlatformAdapter extends AbstractSpigotPlatformAdapter {

    @Override
    public String getPlatformName() {
        return "spigot-1.21";
    }

    @Override
    public List<Class<? extends Listener>> getAdditionalListeners() {
        return Arrays.asList(PlayerSignOpenListener.class);
    }
}
