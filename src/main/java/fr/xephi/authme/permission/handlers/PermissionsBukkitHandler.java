package fr.xephi.authme.permission.handlers;

import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.permission.PermissionsSystemType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;

public class PermissionsBukkitHandler implements PermissionHandler {

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
    public boolean isInGroup(Player player, String group) {
        List<String> groupNames = getGroups(player);

        return groupNames.contains(group);
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
        // FIXME Gnat008 20160601: Add support for this
        return new ArrayList<>();
    }

    @Override
    public String getPrimaryGroup(Player player) {
        // Get the groups of the player
        List<String> groups = getGroups(player);

        // Make sure there is any group available, or return null
        if (groups.size() == 0)
            return null;

        // Return the first group
        return groups.get(0);
    }

    @Override
    public PermissionsSystemType getPermissionSystem() {
        return PermissionsSystemType.PERMISSIONS_BUKKIT;
    }
}
