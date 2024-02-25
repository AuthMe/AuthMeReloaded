package fr.xephi.authme.service;

import fr.xephi.authme.AuthMe;
import fr.xephi.authme.settings.Settings;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import javax.inject.Inject;

public class PaperBukkitService extends SpigotBukkitService {

    @Inject
    public PaperBukkitService(AuthMe authMe, Settings settings) {
        super(authMe, settings);
    }

    @Override
    public void teleport(Player player, Location location) {
        player.teleportAsync(location);
    }
}
