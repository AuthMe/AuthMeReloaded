package fr.xephi.authme.command.executable.authme.debug;

import fr.xephi.authme.permission.DebugSectionPermissions;
import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.service.BukkitService;
import fr.xephi.authme.settings.Settings;
import fr.xephi.authme.settings.SpawnLoader;
import fr.xephi.authme.settings.properties.RestrictionSettings;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;

import static fr.xephi.authme.command.executable.authme.debug.DebugSectionUtils.formatLocation;

/**
 * Shows the spawn location that AuthMe is configured to use.
 */
class SpawnLocationViewer implements DebugSection {

    @Inject
    private SpawnLoader spawnLoader;

    @Inject
    private Settings settings;

    @Inject
    private BukkitService bukkitService;


    @Override
    public String getName() {
        return "spawn";
    }

    @Override
    public String getDescription() {
        return "Shows the spawn location that AuthMe will use";
    }

    @Override
    public void execute(CommandSender sender, List<String> arguments) {
        sender.sendMessage(ChatColor.BLUE + "AuthMe spawn location viewer");
        if (arguments.isEmpty()) {
            showGeneralInfo(sender);
        } else if ("?".equals(arguments.get(0))) {
            showHelp(sender);
        } else {
            showPlayerSpawn(sender, arguments.get(0));
        }
    }

    @Override
    public PermissionNode getRequiredPermission() {
        return DebugSectionPermissions.SPAWN_LOCATION;
    }

    private void showGeneralInfo(CommandSender sender) {
        sender.sendMessage("Spawn priority: "
            + String.join(", ", settings.getProperty(RestrictionSettings.SPAWN_PRIORITY)));
        sender.sendMessage("AuthMe spawn location: " + formatLocation(spawnLoader.getSpawn()));
        sender.sendMessage("AuthMe first spawn location: " + formatLocation(spawnLoader.getFirstSpawn()));
        sender.sendMessage("AuthMe (first)spawn are only used depending on the configured priority!");
        sender.sendMessage("Use '/authme debug spawn ?' for further help");
    }

    private void showHelp(CommandSender sender) {
        sender.sendMessage("Use /authme spawn and /authme firstspawn to teleport to the spawns.");
        sender.sendMessage("/authme set(first)spawn sets the (first) spawn to your current location.");
        sender.sendMessage("Use /authme debug spawn <player> to view where a player would be teleported to.");
        sender.sendMessage("Read more at https://github.com/AuthMe/AuthMeReloaded/wiki/Spawn-Handling");
    }

    private void showPlayerSpawn(CommandSender sender, String playerName) {
        Player player = bukkitService.getPlayerExact(playerName);
        if (player == null) {
            sender.sendMessage("Player '" + playerName + "' is not online!");
        } else {
            Location spawn = spawnLoader.getSpawnLocation(player);
            sender.sendMessage("Player '" + playerName + "' has spawn location: " + formatLocation(spawn));
            sender.sendMessage("Note: this check excludes the AuthMe firstspawn.");
        }
    }
}
