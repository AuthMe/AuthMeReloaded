package fr.xephi.authme.permission.handlers;

import com.platymuus.bukkit.permissions.Group;
import com.platymuus.bukkit.permissions.PermissionsPlugin;
import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.permission.PermissionsSystemType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Handler for PermissionsBukkit.
 *
 * @see <a href="https://dev.bukkit.org/projects/permbukkit">PermissionsBukkit Bukkit page</a>
 */
public class PermissionsBukkitHandler implements PermissionHandler {

    private PermissionsPlugin permissionsBukkitInstance;

    public PermissionsBukkitHandler(PluginManager pluginManager) throws PermissionHandlerException {
        Plugin plugin = pluginManager.getPlugin("PermissionsBukkit");
        if (plugin == null) {
            throw new PermissionHandlerException("Could not get instance of PermissionsBukkit");
        }
        permissionsBukkitInstance = (PermissionsPlugin) plugin;
    }

    @Override
    public boolean addToGroup(Player player, String group) {
        return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "permissions player addgroup " + player.getName() + " " + group);
    }

    @Override
    public boolean hasGroupSupport() {
        return true;
    }

    @Override
    public boolean hasPermissionOffline(String name, PermissionNode node) {
        return false;
    }

    @Override
    public boolean removeFromGroup(Player player, String group) {
        return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "permissions player removegroup " + player.getName() + " " + group);
    }

    @Override
    public boolean setGroup(Player player, String group) {
        return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "permissions player setgroup " + player.getName() + " " + group);
    }

    @Override
    public List<String> getGroups(Player player) {
        List<String> groups = new ArrayList<String>();
        for (Group group : permissionsBukkitInstance.getGroups(player.getUniqueId())) {
            groups.add(group.getName());
        }
        return groups;
    }

    @Override
    public PermissionsSystemType getPermissionSystem() {
        return PermissionsSystemType.PERMISSIONS_BUKKIT;
    }
}
