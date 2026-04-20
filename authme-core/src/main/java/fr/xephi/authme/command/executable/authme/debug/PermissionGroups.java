package fr.xephi.authme.command.executable.authme.debug;

import fr.xephi.authme.data.limbo.UserGroup;
import fr.xephi.authme.permission.DebugSectionPermissions;
import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.permission.PermissionsManager;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.inject.Inject;
import java.util.List;

import static java.util.stream.Collectors.toList;

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
            List<String> groupNames = permissionsManager.getGroups(player).stream()
                .map(UserGroup::getGroupName)
                .collect(toList());

            sender.sendMessage("Player " + name + " has permission groups: " + String.join(", ", groupNames));
            sender.sendMessage("Primary group is: " + permissionsManager.getGroups(player));
        }
    }

    @Override
    public PermissionNode getRequiredPermission() {
        return DebugSectionPermissions.PERM_GROUPS;
    }
}
