package fr.xephi.authme.permission.handlers;

import fr.xephi.authme.OfflinePlayerWrapper;
import fr.xephi.authme.permission.PermissionNode;
import fr.xephi.authme.permission.PermissionsSystemType;
import net.milkbowl.vault.permission.Permission;
import org.bukkit.OfflinePlayer;
import org.bukkit.Server;
import org.bukkit.plugin.RegisteredServiceProvider;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static java.util.concurrent.CompletableFuture.completedFuture;

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
    @SuppressWarnings("deprecated")
    public CompletableFuture<Boolean> addToGroup(OfflinePlayerWrapper player, String groupName) {
        return completedFuture(vaultProvider.playerAddGroup((String)null, player.getName(), groupName));
    }

    @Override
    public boolean hasGroupSupport() {
        return vaultProvider.hasGroupSupport();
    }

    @Override
    public CompletableFuture<Boolean> hasPermissionOffline(OfflinePlayerWrapper player, PermissionNode node) {
        return completedFuture(vaultProvider.playerHas((String)null, player.getName(), node.getNode()));
    }

    @Override
    public CompletableFuture<Boolean> isInGroup(OfflinePlayerWrapper player, String groupName) {
        return completedFuture(vaultProvider.playerInGroup(null, player, groupName));
    }

    @Override
    public CompletableFuture<Boolean> removeFromGroup(OfflinePlayerWrapper player, String groupName) {
        return completedFuture(vaultProvider.playerRemoveGroup(null, player, groupName));
    }

    @Override
    public CompletableFuture<Boolean> setGroup(OfflinePlayerWrapper player, String groupName) {
        return getGroups(player).thenApply(groups -> {
            groups.forEach(group -> removeFromGroup(player, group));
            return vaultProvider.playerAddGroup(null, player, groupName);
        });
    }

    @Override
    public CompletableFuture<List<String>> getGroups(OfflinePlayerWrapper player) {
        return completedFuture(Arrays.asList(vaultProvider.getPlayerGroups(null, player)));
    }

    @Override
    public CompletableFuture<Optional<String>> getPrimaryGroup(OfflinePlayerWrapper player) {
        return completedFuture(Optional.ofNullable(vaultProvider.getPrimaryGroup(null, player)));
    }

    @Override
    public PermissionsSystemType getPermissionSystem() {
        return PermissionsSystemType.VAULT;
    }
}
