package fr.xephi.authme.permission.handlers;

import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.permission.PermissionsSystemType;
import org.anjocaido.groupmanager.GroupManager;
import org.anjocaido.groupmanager.data.User;
import org.anjocaido.groupmanager.permissions.AnjoPermissionsHandler;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class GroupManagerHandler implements PermissionHandler {

    private GroupManager groupManager;

    public GroupManagerHandler(GroupManager groupManager) {
        this.groupManager = groupManager;
    }

    @Override
    public boolean addToGroup(Player player, String group) {
        return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "manuaddsub " + player.getName() + " " + group);
    }

    @Override
    public boolean hasGroupSupport() {
        return true;
    }

    @Override
    public boolean hasPermission(Player player, PermissionNode node) {
        final AnjoPermissionsHandler handler = groupManager.getWorldsHolder().getWorldPermissions(player);
        return handler != null && handler.has(player, node.getNode());
    }

    @Override
    public boolean hasPermission(String name, PermissionNode node) {
        final AnjoPermissionsHandler handler = groupManager.getWorldsHolder().getWorldPermissionsByPlayerName(name);
        List<String> perms = handler.getAllPlayersPermissions(name);
        return perms.contains(node.getNode());
    }

    @Override
    public boolean isInGroup(Player player, String group) {
        final AnjoPermissionsHandler handler = groupManager.getWorldsHolder().getWorldPermissions(player);
        return handler != null && handler.inGroup(player.getName(), group);
    }

    @Override
    public boolean removeFromGroup(Player player, String group) {
        return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "manudelsub " + player.getName() + " " + group);
    }

    @Override
    public boolean setGroup(Player player, String group) {
        final AnjoPermissionsHandler handler = groupManager.getWorldsHolder().getWorldPermissions(player);
        for (String groupName : handler.getGroups(player.getName())) {
            removeFromGroup(player, groupName);
        }

        return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "manuadd " + player.getName() + " " + group);
    }

    @Override
    public List<String> getGroups(Player player) {
        final AnjoPermissionsHandler handler = groupManager.getWorldsHolder().getWorldPermissions(player);
        if (handler == null)
            return new ArrayList<>();
        return Arrays.asList(handler.getGroups(player.getName()));
    }

    @Override
    public String getPrimaryGroup(Player player) {
        final AnjoPermissionsHandler handler = groupManager.getWorldsHolder().getWorldPermissions(player);
        if (handler == null)
            return null;
        return handler.getGroup(player.getName());
    }

    @Override
    public PermissionsSystemType getPermissionSystem() {
        return PermissionsSystemType.ESSENTIALS_GROUP_MANAGER;
    }
}
