package fr.xephi.authme.permission.handlers;

import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.permission.PermissionsSystemType;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.List;

public class VaultHandler implements PermissionHandler {

    private Permission vaultProvider;

    public VaultHandler(Permission vaultProvider) {
        this.vaultProvider = vaultProvider;
    }

    @Override
    public boolean addToGroup(Player player, String group) {
        return vaultProvider.playerAddGroup(player, group);
    }

    @Override
    public boolean hasGroupSupport() {
        return vaultProvider.hasGroupSupport();
    }

    @Override
    public boolean hasPermission(Player player, PermissionNode node) {
        return vaultProvider.has(player, node.getNode());
    }

    @Override
    public boolean isInGroup(Player player, String group) {
        return vaultProvider.playerInGroup(player, group);
    }

    @Override
    public boolean removeFromGroup(Player player, String group) {
        return vaultProvider.playerRemoveGroup(player, group);
    }

    @Override
    public boolean setGroup(Player player, String group) {
        for (String groupName : getGroups(player)) {
            removeFromGroup(player, groupName);
        }

        return vaultProvider.playerAddGroup(player, group);
    }

    @Override
    public List<String> getGroups(Player player) {
        return Arrays.asList(vaultProvider.getPlayerGroups(player));
    }

    @Override
    public String getPrimaryGroup(Player player) {
        return vaultProvider.getPrimaryGroup(player);
    }

    @Override
    public PermissionsSystemType getPermissionSystem() {
        return PermissionsSystemType.VAULT;
    }
}
