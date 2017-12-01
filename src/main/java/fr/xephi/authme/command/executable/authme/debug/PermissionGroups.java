package fr.xephi.authme.command.executable.authme.debug;

import fr.xephi.authme.permission.DebugSectionPermissions;
import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.permission.PermissionsManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;

/**
 * Outputs the permission groups of a player.
 */
class PermissionGroups implements DebugSection {

    @Inject
    private PermissionsManager permissionsManager;

    @Override
    public String getName() {
        return "groups";
    }

    @Override
    public String getDescription() {
        return "Show permission groups a player belongs to";
    }

    @Override
    public void execute(CommandSender sender, List<String> arguments) {
        sender.sendMessage(ChatColor.BLUE + "AuthMe permission groups");
        String name = arguments.isEmpty() ? sender.getName() : arguments.get(0);
        Player player = Bukkit.getPlayer(name);
        if (player == null) {
            sender.sendMessage("Player " + name + " could not be found");
        } else {
            sender.sendMessage("Player " + name + " has permission groups: "
                + String.join(", ", permissionsManager.getGroups(player)));
            sender.sendMessage("Primary group is: " + permissionsManager.getGroups(player));
        }
    }

    @Override
    public PermissionNode getRequiredPermission() {
        return DebugSectionPermissions.PERM_GROUPS;
    }
}
