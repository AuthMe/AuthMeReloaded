package fr.xephi.authme.permission.handlers;

import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.permission.PermissionsSystemType;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Arrays;
import java.util.List;

/**
 * Handler for permissions via Vault.
 *
 * @see <a href="https://dev.bukkit.org/projects/vault">Vault Bukkit page</a>
 * @see <a href="https://github.com/milkbowl/Vault">Vault on Github</a>
 */
public class VaultHandler implements PermissionHandler {

    private Permission vaultProvider;

    public VaultHandler(Server server) throws PermissionHandlerException {
        this.vaultProvider = getVaultPermission(server);
    }

    private static Permission getVaultPermission(Server server) throws PermissionHandlerException {
        // Get the permissions provider service
        RegisteredServiceProvider<Permission> permissionProvider = server
            .getServicesManager().getRegistration(Permission.class);
        if (permissionProvider == null) {
            throw new PermissionHandlerException("Could not load permissions provider service");
        }

        // Get the Vault provider and make sure it's valid
        Permission vaultPerms = permissionProvider.getProvider();
        if (vaultPerms == null) {
            throw new PermissionHandlerException("Could not load Vault permissions provider");
        }
        return vaultPerms;
    }

    @Override
    public boolean addToGroup(OfflinePlayer player, String group) {
        return vaultProvider.playerAddGroup(null, player, group);
    }

    @Override
    public boolean hasGroupSupport() {
        return vaultProvider.hasGroupSupport();
    }

    @Override
    public boolean hasPermissionOffline(String name, PermissionNode node) {
        return vaultProvider.has((String) null, name, node.getNode());
    }

    @Override
    public boolean isInGroup(OfflinePlayer player, String group) {
        return vaultProvider.playerInGroup(null, player, group);
    }

    @Override
    public boolean removeFromGroup(OfflinePlayer player, String group) {
        return vaultProvider.playerRemoveGroup(null, player, group);
    }

    @Override
    public boolean setGroup(OfflinePlayer player, String group) {
        for (String groupName : getGroups(player)) {
            removeFromGroup(player, groupName);
        }

        return vaultProvider.playerAddGroup(null, player, group);
    }

    @Override
    public List<String> getGroups(OfflinePlayer player) {
        return Arrays.asList(vaultProvider.getPlayerGroups(null, player));
    }

    @Override
    public String getPrimaryGroup(OfflinePlayer player) {
        return vaultProvider.getPrimaryGroup(null, player);
    }

    @Override
    public PermissionsSystemType getPermissionSystem() {
        return PermissionsSystemType.VAULT;
    }
}
