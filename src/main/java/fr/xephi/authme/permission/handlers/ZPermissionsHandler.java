package fr.xephi.authme.permission.handlers;

import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.permission.PermissionsSystemType;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.tyrannyofheaven.bukkit.zPermissions.ZPermissionsService;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ZPermissionsHandler implements PermissionHandler {

    private ZPermissionsService zPermissionsService;

    public ZPermissionsHandler(ZPermissionsService zPermissionsService) {
        this.zPermissionsService = zPermissionsService;
    }

    @Override
    public boolean addToGroup(Player player, String group) {
        return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "permissions player " + player.getName() + " addgroup " + group);
    }

    @Override
    public boolean hasGroupSupport() {
        return true;
    }

    @Override
    public boolean hasPermission(Player player, PermissionNode node, boolean def) {
        Map<String, Boolean> perms = zPermissionsService.getPlayerPermissions(player.getWorld().getName(), null, player.getName());
        if (perms.containsKey(node.getNode()))
            return perms.get(node.getNode());
        else
            return def;
    }

    @Override
    public boolean isInGroup(Player player, String group) {
        return getGroups(player).contains(group);
    }

    @Override
    public boolean removeFromGroup(Player player, String group) {
        return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "permissions player " + player.getName() + " removegroup " + group);
    }

    @Override
    public boolean setGroup(Player player, String group) {
        return Bukkit.dispatchCommand(Bukkit.getConsoleSender(), "permissions player " + player.getName() + " setgroup " + group);
    }

    @Override
    @SuppressWarnings("unchecked")
    public List<String> getGroups(Player player) {
        // TODO Gnat008 20160631: Use UUID not name?
        return new ArrayList(zPermissionsService.getPlayerGroups(player.getName()));
    }

    @Override
    public String getPrimaryGroup(Player player) {
        // TODO Gnat008 20160631: Use UUID not name?
        return zPermissionsService.getPlayerPrimaryGroup(player.getName());
    }

    @Override
    public PermissionsSystemType getPermissionSystem() {
        return PermissionsSystemType.Z_PERMISSIONS;
    }
}
