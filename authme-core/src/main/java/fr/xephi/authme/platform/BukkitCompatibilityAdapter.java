package fr.xephi.authme.platform;

import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.util.Optional;

public interface BukkitCompatibilityAdapter {

    Optional<String> getPlayerLocale(Player player);

    int getSpawnRadius(World world);

    boolean isBlockPassable(Block block);
}
